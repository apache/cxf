/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.utils.NamespaceMap;

/**
 * Walks the service model and sets up the element/type names.
 */
class JAXBSchemaInitializer extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getLogger(JAXBSchemaInitializer.class);

    private SchemaCollection schemas;
    private JAXBContextProxy context;
    private final boolean qualifiedSchemas;
    
    public JAXBSchemaInitializer(ServiceInfo serviceInfo, 
                                 SchemaCollection col, 
                                 JAXBContext context, 
                                 boolean q) {
        super(serviceInfo);
        schemas = col;
        this.context = ReflectionInvokationHandler.createProxyWrapper(context, JAXBContextProxy.class);
        this.qualifiedSchemas = q;
    }

    static Class<?> getArrayComponentType(Type cls) {
        if (cls instanceof Class) {
            if (((Class)cls).isArray()) {
                return ((Class)cls).getComponentType();
            } else {
                return (Class)cls;
            }
        } else if (cls instanceof ParameterizedType) {
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                return getArrayComponentType(t2);
            }
        } else if (cls instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)cls;
            Class ct = (Class) gt.getGenericComponentType();
            return Array.newInstance(ct, 0).getClass();
        }
        return null;
    }
    public JAXBBeanInfo getBeanInfo(Type cls) {
        if (cls instanceof Class) {
            if (((Class)cls).isArray()) {
                return getBeanInfo(((Class)cls).getComponentType());
            } else {
                return getBeanInfo((Class)cls);
            }
        } else if (cls instanceof ParameterizedType) {
            for (Type t2 : ((ParameterizedType)cls).getActualTypeArguments()) {
                return getBeanInfo(t2);
            }
        } else if (cls instanceof GenericArrayType) {
            GenericArrayType gt = (GenericArrayType)cls;
            Class ct = (Class) gt.getGenericComponentType();
            ct = Array.newInstance(ct, 0).getClass();

            return getBeanInfo(ct);
        }
        
        return null;
    }
    public JAXBBeanInfo getBeanInfo(Class<?> cls) {
        return getBeanInfo(context, cls);
    }
    public static JAXBBeanInfo getBeanInfo(JAXBContextProxy context, Class<?> cls) {
        Object o = context.getBeanInfo(cls);
        if (o == null) {
            return null;
        }
        return ReflectionInvokationHandler.createProxyWrapper(o, JAXBBeanInfo.class);
    }
    @Override
    public void begin(MessagePartInfo part) {
        // Check to see if the WSDL information has been filled in for us.
        if (part.getTypeQName() != null || part.getElementQName() != null) {
            checkForExistence(part);
            return;
        }
        
        Class<?> clazz = part.getTypeClass();
        if (clazz == null) {
            return;
        }

        boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
        if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
            clazz = clazz.getComponentType();
        }

        JAXBBeanInfo beanInfo = getBeanInfo(clazz);
        if (beanInfo == null) {
            Annotation[] anns = (Annotation[])part.getProperty("parameter.annotations");
            XmlJavaTypeAdapter jta = findFromTypeAdapter(clazz, anns);
            if (jta != null) {
                beanInfo = findFromTypeAdapter(jta.value());
                if (anns == null) {
                    anns = new Annotation[] {jta};
                } else {
                    boolean found = false;
                    for (Annotation t : anns) {
                        if (t == jta) {
                            found = true;
                        }
                    }
                    if (!found) {
                        Annotation tmp[] = new Annotation[anns.length + 1];
                        System.arraycopy(anns, 0, tmp, 0, anns.length);
                        tmp[anns.length] = jta;
                        anns = tmp;
                    }
                }
                part.setProperty("parameter.annotations", anns);
                part.setProperty("honor.jaxb.annotations", Boolean.TRUE);
            }
        }
        if (beanInfo == null) {
            if (Exception.class.isAssignableFrom(clazz)) {
                QName name = (QName)part.getMessageInfo().getProperty("elementName");
                part.setElementQName(name);
                buildExceptionType(part, clazz);
            }
            return;
        }
        boolean isElement = beanInfo.isElement() 
            && !Boolean.TRUE.equals(part.getMessageInfo().getOperation()
                                        .getProperty("operation.force.types"));
        boolean hasType = !beanInfo.getTypeNames().isEmpty();
        if (isElement && isFromWrapper && hasType) {
            //if there is both a Global element and a global type, AND we are in a wrapper,
            //make sure we use the type instead of a ref to the element to 
            //match the rules for wrapped/unwrapped
            isElement = false;
        }

        part.setElement(isElement);
        
        if (isElement) {
            QName name = new QName(beanInfo.getElementNamespaceURI(null), 
                                   beanInfo.getElementLocalName(null));
            XmlSchemaElement el = schemas.getElementByQName(name);
            if (el != null && el.getRefName() != null) {
                part.setTypeQName(el.getRefName());
            } else {
                part.setElementQName(name);
            }
            part.setXmlSchema(el);
        } else  {
            QName typeName = getTypeName(beanInfo);
            if (typeName != null) {
                part.setTypeQName(typeName);
                part.setXmlSchema(schemas.getTypeByQName(typeName));
            }
        }
    }

    private XmlJavaTypeAdapter findFromTypeAdapter(Class<?> clazz, Annotation[] anns) {
        JAXBBeanInfo ret = null;
        if (anns != null) {
            for (Annotation a : anns) {
                if (XmlJavaTypeAdapter.class.isAssignableFrom(a.annotationType())) {
                    ret = findFromTypeAdapter(((XmlJavaTypeAdapter)a).value());
                    if (ret != null) {
                        return (XmlJavaTypeAdapter)a;
                    }
                }
            }
        }
        XmlJavaTypeAdapter xjta = clazz.getAnnotation(XmlJavaTypeAdapter.class);
        if (xjta != null) {
            ret = findFromTypeAdapter(xjta.value());
            if (ret != null) {
                return xjta;
            }
        }
        return null;
    }

    private JAXBBeanInfo findFromTypeAdapter(Class<? extends XmlAdapter> aclass) {
        Class<?> c2 = aclass;
        Type sp = c2.getGenericSuperclass();
        while (!XmlAdapter.class.equals(c2) && c2 != null) {
            sp = c2.getGenericSuperclass();
            c2 = c2.getSuperclass();
        }
        if (sp instanceof ParameterizedType) {
            Type tp = ((ParameterizedType)sp).getActualTypeArguments()[0];
            if (tp instanceof Class) {
                return getBeanInfo((Class<?>)tp);
            }
        }
        return null;
    }

    private QName getTypeName(JAXBBeanInfo beanInfo) {
        Iterator<QName> itr = beanInfo.getTypeNames().iterator();
        if (!itr.hasNext()) {
            return null;
        }
        
        return itr.next();
    } 
    public void checkForExistence(MessagePartInfo part) {
        QName qn = part.getElementQName();
        if (qn != null) {
            XmlSchemaElement el = schemas.getElementByQName(qn);
            if (el == null) {
                Class<?> clazz = part.getTypeClass();
                if (clazz == null) {
                    return;
                }

                boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
                if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
                    clazz = clazz.getComponentType();
                }
                JAXBBeanInfo beanInfo = getBeanInfo(clazz);
                if (beanInfo == null) {
                    if (Exception.class.isAssignableFrom(clazz)) {
                        QName name = (QName)part.getMessageInfo().getProperty("elementName");
                        part.setElementQName(name);
                        buildExceptionType(part, clazz);
                    }
                    return;
                }
                
                QName typeName = getTypeName(beanInfo);

                createBridgeXsElement(part, qn, typeName);
            } else if (part.getXmlSchema() == null) {
                part.setXmlSchema(el);
            }
        }
    }

    private void createBridgeXsElement(MessagePartInfo part, QName qn, QName typeName) {
        XmlSchemaElement el = null;
        SchemaInfo schemaInfo = serviceInfo.getSchema(qn.getNamespaceURI());
        if (schemaInfo != null) {
            el = schemaInfo.getElementByQName(qn);
            if (el == null) {
                el = createXsElement(part, typeName, schemaInfo);

                schemaInfo.getSchema().getElements().add(el.getQName(), el);
                schemaInfo.getSchema().getItems().add(el);
            } else if (!typeName.equals(el.getSchemaTypeName())) {
                throw new Fault(new Message("CANNOT_CREATE_ELEMENT", LOG, 
                                            qn, typeName, el.getSchemaTypeName()));
            }
            return;
        }
        
        schemaInfo = new SchemaInfo(qn.getNamespaceURI(), qualifiedSchemas, false);
        
        el = createXsElement(part, typeName, schemaInfo);

        XmlSchema schema = schemas.newXmlSchemaInCollection(qn.getNamespaceURI());
        if (qualifiedSchemas) {
            schema.setElementFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));
        }
        schemaInfo.setSchema(schema);
        schema.getElements().add(el.getQName(), el);
        schema.getItems().add(el);

        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, schema.getTargetNamespace());
        nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
        schema.setNamespaceContext(nsMap);
        
        serviceInfo.addSchema(schemaInfo);
    }

    private XmlSchemaElement createXsElement(MessagePartInfo part, QName typeName, SchemaInfo schemaInfo) {
        XmlSchemaElement el = new XmlSchemaElement();
        XmlSchemaUtils.setElementQName(el, part.getElementQName());
        el.setNillable(true);
        el.setSchemaTypeName(typeName);
        part.setXmlSchema(el);
        return el;
    }
    
    public void end(FaultInfo fault) {
        MessagePartInfo part = fault.getMessageParts().get(0); 
        Class<?> cls = part.getTypeClass();
        Class<?> cl2 = (Class)fault.getProperty(Class.class.getName());
        if (cls != cl2) {            
            QName name = (QName)fault.getProperty("elementName");
            part.setElementQName(name);           
            JAXBBeanInfo beanInfo = getBeanInfo(cls);
            if (beanInfo == null) {
                throw new Fault(new Message("NO_BEAN_INFO", LOG, cls.getName()));
            }
            SchemaInfo schemaInfo = serviceInfo.getSchema(part.getElementQName().getNamespaceURI());
            if (schemaInfo != null
                && !isExistSchemaElement(schemaInfo.getSchema(), part.getElementQName())) {
                    
                XmlSchemaElement el = new XmlSchemaElement();
                XmlSchemaUtils.setElementQName(el, part.getElementQName());
                el.setNillable(true);
                
                schemaInfo.getSchema().getItems().add(el);
                schemaInfo.getSchema().getElements().add(el.getQName(), el);

                Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                if (!itr.hasNext()) {
                    return;
                }
                QName typeName = itr.next();
                el.setSchemaTypeName(typeName);
            }
        } else if (part.getXmlSchema() == null) {
            try {
                cls.getConstructor(new Class[] {String.class});
            } catch (Exception e) {
                try {
                    cls.getConstructor(new Class[0]);
                } catch (Exception e2) {
                    //no String or default constructor, we cannot use it
                    return;
                }
            }            
            
            //not mappable in JAXBContext directly, we'll have to do it manually :-(
            SchemaInfo schemaInfo = serviceInfo.getSchema(part.getElementQName().getNamespaceURI());
            if (schemaInfo == null
                || isExistSchemaElement(schemaInfo.getSchema(), part.getElementQName())) {
                return;
            }
                
            XmlSchemaElement el = new XmlSchemaElement();
            XmlSchemaUtils.setElementQName(el, part.getElementQName());
            
            schemaInfo.getSchema().getItems().add(el);
            schemaInfo.getSchema().getElements().add(el.getQName(), el);

            part.setXmlSchema(el);

            XmlSchemaComplexType ct = new XmlSchemaComplexType(schemaInfo.getSchema());
            el.setSchemaType(ct);
            XmlSchemaSequence seq = new XmlSchemaSequence();
            ct.setParticle(seq);
                
            Method methods[] = cls.getMethods();
            for (Method m : methods) {
                if (m.getName().startsWith("get")
                    || m.getName().startsWith("is")) {
                    int beginIdx = m.getName().startsWith("get") ? 3 : 2;
                    try {
                        m.getDeclaringClass().getMethod("set" + m.getName().substring(beginIdx),
                                                        m.getReturnType());
                        
                        JAXBBeanInfo beanInfo = getBeanInfo(m.getReturnType());
                        if (beanInfo != null) {
                            el = new XmlSchemaElement();
                            el.setName(m.getName().substring(beginIdx));
                            
                            String ns = schemaInfo.getSchema().getElementFormDefault()
                                .getValue().equals(XmlSchemaForm.UNQUALIFIED) 
                                ? "" : part.getElementQName().getLocalPart();
                            XmlSchemaUtils.setElementQName(el, 
                                                           new QName(ns, m.getName().substring(beginIdx)));
                            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
                            if (!itr.hasNext()) {
                                return;
                            }
                            QName typeName = itr.next();
                            el.setSchemaTypeName(typeName);
                        }
                        
                        seq.getItems().add(el);
                    } catch (Exception e) {
                        //not mappable
                    }
                }
            }
        }
    }

    
    private void buildExceptionType(MessagePartInfo part, Class<?> cls) {
        SchemaInfo schemaInfo = null;
        for (SchemaInfo s : serviceInfo.getSchemas()) {
            if (s.getNamespaceURI().equals(part.getElementQName().getNamespaceURI())) {
                schemaInfo = s;                
                break;
            }
        }
        XmlSchema schema;
        if (schemaInfo == null) {
            schema = schemas.newXmlSchemaInCollection(part.getElementQName().getNamespaceURI());

            if (qualifiedSchemas) {
                schema.setElementFormDefault(new XmlSchemaForm(XmlSchemaForm.QUALIFIED));
            }

            NamespaceMap nsMap = new NamespaceMap();
            nsMap.add(WSDLConstants.CONVENTIONAL_TNS_PREFIX, schema.getTargetNamespace());
            nsMap.add(WSDLConstants.NP_SCHEMA_XSD, WSDLConstants.NS_SCHEMA_XSD);
            schema.setNamespaceContext(nsMap);

            
            schemaInfo = new SchemaInfo(part.getElementQName().getNamespaceURI());
            schemaInfo.setSchema(schema);
            serviceInfo.addSchema(schemaInfo);
        } else {
            schema = schemaInfo.getSchema();
        }
        
        XmlSchemaComplexType ct = new XmlSchemaComplexType(schema);
        ct.setName(part.getElementQName().getLocalPart());
        // Before updating everything, make sure we haven't added this 
        // type yet.  Multiple methods that throw the same exception 
        // types will cause duplicates. 
        if (schema.getTypeByName(ct.getQName()) != null) {
            return; 
        }
        
        XmlSchemaElement el = new XmlSchemaElement();
        XmlSchemaUtils.setElementQName(el, part.getElementQName());
        schema.getItems().add(el);
        schema.getElements().add(el.getQName(), el);
        part.setXmlSchema(el);
        
        schema.getItems().add(ct);
        schema.addType(ct);
        el.setSchemaTypeName(part.getElementQName());
        
        XmlSchemaSequence seq = new XmlSchemaSequence();
        ct.setParticle(seq);
        String namespace = part.getElementQName().getNamespaceURI();
        
        XmlAccessorType accessorType = cls.getAnnotation(XmlAccessorType.class);
        if (accessorType == null && cls.getPackage() != null) {
            accessorType = cls.getPackage().getAnnotation(XmlAccessorType.class);
        }
        XmlAccessType accessType = accessorType != null ? accessorType.value() : XmlAccessType.PUBLIC_MEMBER;

        
        for (Field f : cls.getDeclaredFields()) {
            if (JAXBContextInitializer.isFieldAccepted(f, accessType)) {
                //map field
                Type type = f.getGenericType();
                JAXBBeanInfo beanInfo = getBeanInfo(type);
                if (beanInfo != null) {
                    addElement(seq, beanInfo, new QName(namespace, f.getName()), isArray(type));
                }                
            }
        }
        for (Method m : cls.getMethods()) {
            if (JAXBContextInitializer.isMethodAccepted(m, accessType)) {
                //map field
                Type type = m.getGenericReturnType();
                JAXBBeanInfo beanInfo = getBeanInfo(type);
                if (beanInfo != null) {
                    int idx = m.getName().startsWith("get") ? 3 : 2;
                    String name = m.getName().substring(idx);
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    addElement(seq, beanInfo, new QName(namespace, name), isArray(type));
                }                
            }
        }
        part.setProperty(JAXBDataBinding.class.getName() + ".CUSTOM_EXCEPTION", Boolean.TRUE);
    }
    
    static boolean isArray(Type cls) {
        if (cls instanceof Class) {
            return ((Class)cls).isArray();
        } else if (cls instanceof ParameterizedType) {
            return true;
        } else if (cls instanceof GenericArrayType) {
            return true;
        }
        return false;
    }

    public void addElement(XmlSchemaSequence seq, JAXBBeanInfo beanInfo,
                           QName name, boolean isArray) {    
        XmlSchemaElement el = new XmlSchemaElement();
        el.setName(name.getLocalPart());
        XmlSchemaUtils.setElementQName(el, name);

        if (isArray) {
            el.setMinOccurs(0);
            el.setMaxOccurs(Long.MAX_VALUE);
        } else {
            el.setMinOccurs(1);
            el.setMaxOccurs(1);
            el.setNillable(true);
        }

        if (beanInfo.isElement()) {
            QName ename = new QName(beanInfo.getElementNamespaceURI(null), 
                                   beanInfo.getElementLocalName(null));
            XmlSchemaElement el2 = schemas.getElementByQName(ename);
            XmlSchemaUtils.setElementQName(el, null);
            XmlSchemaUtils.setElementRefName(el, el2.getRefName());
        } else {
            Iterator<QName> itr = beanInfo.getTypeNames().iterator();
            if (!itr.hasNext()) {
                return;
            }
            QName typeName = itr.next();
            el.setSchemaTypeName(typeName);
        }
        
        seq.getItems().add(el);
    }
    
    
    private boolean isExistSchemaElement(XmlSchema schema, QName qn) {
        boolean isExist = false;
        for (Iterator ite = schema.getItems().getIterator(); ite.hasNext();) {
            XmlSchemaObject obj = (XmlSchemaObject)ite.next();
            if (obj instanceof XmlSchemaElement) {
                XmlSchemaElement xsEle = (XmlSchemaElement)obj;
                if (xsEle.getQName().equals(qn)) {
                    isExist = true;
                    break;
                }
            }
        }
        return isExist;
    }
}

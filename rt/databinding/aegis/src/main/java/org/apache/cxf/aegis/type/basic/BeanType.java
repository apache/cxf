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
package org.apache.cxf.aegis.type.basic;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AbstractTypeCreator;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.mtom.AbstractXOPType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;

/**
 * Serializes JavaBeans.
 * 
 * There's a really dangerous coding convention in this class, maintainers beware.
 * There are two constructor. The no-args constructor defers, until later,
 * the construction of a BeanTypeInfo. The one-arg constructor gets the BeanTypeInfo passed as a parameter.
 * Aegis doesn't have any uniform discipline of 'construct, set properties, initialize'. Instead,
 * each piece of code that uses the type info needs to call getTypeInfo() instead of referencing the
 * 'info' field. 
 */
public class BeanType extends Type {
    private BeanTypeInfo info;

    private boolean isInterface;

    private boolean isException;

    /**
     * Construct a type info. Caller must pass in the type class via 
     * setTypeClass later.
     */
    public BeanType() {
    }

    /**
     * Construct a type info given a full BeanTypeInfo.
     * @param info
     */
    public BeanType(BeanTypeInfo info) {
        this.info = info;
        this.typeClass = info.getTypeClass();
        initTypeClass();
    }

    private void initTypeClass() {
        this.isInterface = typeClass.isInterface();
        isException = Exception.class.isAssignableFrom(typeClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            Class clazz = getTypeClass();
            Object object;
            // the target for properties; either the object or the proxy handler
            Object target;

            if (isInterface) {
                String impl = context.getGlobalContext().getBeanImplementationMap().get(clazz);

                if (impl == null) {
                    InvocationHandler handler = new InterfaceInvocationHandler();
                    object = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {
                        clazz
                    }, handler);
                    target = handler;
                } else {
                    try {
                        clazz = ClassLoaderUtils.loadClass(impl, getClass());
                        object = clazz.newInstance();
                        target = object;
                    } catch (ClassNotFoundException e) {
                        throw new DatabindingException("Could not find implementation class " + impl
                                                       + " for class " + clazz.getName());
                    }
                }
            } else if (isException) {
                object = createFromFault(context);
                target = object;
            } else {
                object = clazz.newInstance();
                target = object;
            }

            // Read attributes
            while (reader.hasMoreAttributeReaders()) {
                MessageReader childReader = reader.getNextAttributeReader();
                QName name = childReader.getName();

                Type type = inf.getType(name);

                if (type != null) {
                    Object writeObj = type.readObject(childReader, context);
                    writeProperty(name, target, writeObj, clazz, inf);
                }
            }

            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                QName name = childReader.getName();

                // Find the BeanTypeInfo that contains a property for the element name
                BeanTypeInfo propertyTypeInfo = getBeanTypeInfoWithProperty(name);

                // Get the Type for the property
                Type type = getElementType(name, propertyTypeInfo, childReader, context);

                if (type != null) {
                    if (!childReader.isXsiNil()) {
                        Object writeObj = type.readObject(childReader, context);

                        writeProperty(name, target, writeObj, clazz, propertyTypeInfo);
                    } else {
                        if (!propertyTypeInfo.isNillable(name)) {
                            throw new DatabindingException(name.getLocalPart() 
                                                           + " is nil, but not nillable.");

                        }
                        childReader.readToEnd();
                    }
                } else {
                    childReader.readToEnd();
                }
            }

            return object;
        } catch (IllegalAccessException e) {
            throw new DatabindingException("Illegal access. " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new DatabindingException("Couldn't instantiate class. " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new DatabindingException("Illegal access. " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new DatabindingException("Could not create class: " + e.getMessage(), e);
        }
    }

    protected Type getElementType(QName name, BeanTypeInfo beanTypeInfo, 
                                  MessageReader reader, Context context) {

        Type type = beanTypeInfo.getType(name);

        // Type can be overriden with a xsi:type attribute
        type = TypeUtil.getReadType(reader.getXMLStreamReader(), context.getGlobalContext(), type);
        return type;
    }

    /**
     * If the class is an exception, this will try and instantiate it with information from the XFireFault (if
     * it exists).
     */
    protected Object createFromFault(Context context) throws SecurityException, InstantiationException,
        IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> clazz = getTypeClass();
        Constructor ctr;
        Object o;

        Fault fault = context.getFault();

        try {
            ctr = clazz.getConstructor(new Class[] {
                String.class, Throwable.class
            });
            o = ctr.newInstance(new Object[] {
                fault.getMessage(), fault
            });
        } catch (NoSuchMethodException e) {
            try {
                ctr = clazz.getConstructor(new Class[] {
                    String.class, Exception.class
                });
                o = ctr.newInstance(new Object[] {
                    fault.getMessage(), fault
                });
            } catch (NoSuchMethodException e1) {
                try {
                    ctr = clazz.getConstructor(new Class[] {
                        String.class
                    });
                    o = ctr.newInstance(new Object[] {
                        fault.getMessage()
                    });
                } catch (NoSuchMethodException e2) {
                    return clazz.newInstance();
                }
            }
        }

        return o;
    }

    /**
     * Write the specified property to a field.
     */
    protected void writeProperty(QName name, Object object, Object property, Class impl, BeanTypeInfo inf)
        throws DatabindingException {

        if (object instanceof InterfaceInvocationHandler) {
            InterfaceInvocationHandler delegate = (InterfaceInvocationHandler)object;
            delegate.writeProperty(name.getLocalPart(), property);
            return;
        }

        try {
            PropertyDescriptor desc = inf.getPropertyDescriptorFromMappedName(name);

            Method m = desc.getWriteMethod();

            if (m == null) {
                if (getTypeClass().isInterface()) {
                    m = getWriteMethodFromImplClass(impl, desc);
                }

                if (m == null) {
                    throw new DatabindingException("No write method for property " + name + " in "
                                                   + object.getClass());
                }
            }

            Class propertyType = desc.getPropertyType();
            if ((property == null && !propertyType.isPrimitive()) || (property != null)) {
                m.invoke(object, new Object[] {
                    property
                });
            }
        } catch (Exception e) {
            if (e instanceof DatabindingException) {
                throw (DatabindingException)e;
            }

            throw new DatabindingException("Couldn't set property " + name + " on " + object + ". "
                                           + e.getMessage(), e);
        }
    }

    /**
     * This is a hack to get the write method from the implementation class for an interface.
     */
    private Method getWriteMethodFromImplClass(Class<?> impl, PropertyDescriptor pd) throws Exception {
        String name = pd.getName();
        name = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);

        return impl.getMethod(name, new Class[] {
            pd.getPropertyType()
        });
    }

    /**
     * To avoid double-writing xsi:type attributes, ObjectType uses this special entrypoint.
     * 
     * @param object
     * @param writer
     * @param context
     * @param wroteXsiType
     */
    void writeObjectFromObjectType(Object object, MessageWriter writer, 
                                   Context context, boolean wroteXsiType) {
        writeObjectInternal(object, writer, context, wroteXsiType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObject(Object object, MessageWriter writer, 
                            Context context) throws DatabindingException {
        writeObjectInternal(object, writer, context, false);
    }

    private void writeObjectInternal(Object object, MessageWriter writer, Context context,
                                     boolean wroteXsiType) throws DatabindingException {
        if (object == null) {
            return;
        }

        BeanTypeInfo inf = getTypeInfo();

        if (!wroteXsiType && object.getClass() == getTypeClass() && context.isWriteXsiTypes()) {
            writer.writeXsiType(getSchemaType());
        }

        for (QName name : inf.getAttributes()) {

            Object value = readProperty(object, name);
            if (value != null) {
                Type type = getType(inf, name);

                if (type == null) {
                    throw new DatabindingException("Couldn't find type for " + value.getClass()
                                                   + " for property " + name);
                }

                MessageWriter cwriter = writer.getAttributeWriter(name);

                type.writeObject(value, cwriter, context);

                cwriter.close();
            }
        }
        
        if (inf.isExtension()) {
            Type t = getSuperType();
            if (t != null) {
                t.writeObject(object, writer, context);
            }
        }

        for (QName name : inf.getElements()) {

            if (inf.isExtension()
                && inf.getPropertyDescriptorFromMappedName(name).getReadMethod().getDeclaringClass() != inf
                    .getTypeClass()) {
                continue;
            }
            Object value = readProperty(object, name);

            Type defaultType = getType(inf, name);
            Type type = TypeUtil.getWriteType(context.getGlobalContext(), value, defaultType);

            // Write the value if it is not null.
            if (value != null) {
                if (type == null) {
                    throw new DatabindingException("Couldn't find type for " + value.getClass()
                                                   + " for property " + name);
                }

                writeElement(name, value, type, writer, context);
            } else if (inf.isNillable(name)) {
                MessageWriter cwriter = getWriter(writer, name, type);

                // Write the xsi:nil if it is null.
                cwriter.writeXsiNil();

                cwriter.close();
            }
        }
       
    }

    protected void writeElement(QName name, Object value, Type type, MessageWriter writer, Context context) {
        MessageWriter cwriter = getWriter(writer, name, type);

        type.writeObject(value, cwriter, context);

        cwriter.close();
    }

    private MessageWriter getWriter(MessageWriter writer, QName name, Type type) {
        MessageWriter cwriter;
        if (type.isAbstract()) {
            cwriter = writer.getElementWriter(name);
        } else {
            cwriter = writer.getElementWriter(name);
        }
        return cwriter;
    }

    protected Object readProperty(Object object, QName name) {
        try {
            PropertyDescriptor desc = getTypeInfo().getPropertyDescriptorFromMappedName(name);

            Method m = desc.getReadMethod();

            if (m == null) {
                throw new DatabindingException("No read method for property " + name + " in class "
                                               + object.getClass().getName());
            }

            return m.invoke(object, new Object[0]);
        } catch (Exception e) {
            throw new DatabindingException("Couldn't get property " + name + " from bean " + object, e);
        }
    }

    private Type getType(BeanTypeInfo inf, QName name) {
        Type type = inf.getType(name);

        if (type == null) {
            throw new NullPointerException("Couldn't find type for" + name + " in class "
                                           + getTypeClass().getName());
        }

        return type;
    }

    private void writeTypeReference(QName name, XmlSchemaElement element, Type type, 
                                    XmlSchema schemaRoot) {
        if (type.isAbstract()) {
            element.setName(name.getLocalPart());
            element.setSchemaTypeName(type.getSchemaType());
            XmlSchemaUtils.addImportIfNeeded(schemaRoot, type.getSchemaType().getNamespaceURI());

            /*
             * Here we have a semi-giant mess. If a parameter has a minOccurs > 1, it ends
             * up in the type info. However, it really got used in the array type.
             * All we really want to do here is manage 'optional' elements. If we 
             * ever implement flat arrays, this will change. For now, we ignore
             * maxOccurs and we only look for 0's in the minOccurs. 
             */
            long minOccurs = getTypeInfo().getMinOccurs(name);
            /* If it is 1, that's the default, and if it's greater than one, it means
             * that there is a real array at work here. So the only value we want to pay
             * attention to is 0.
             */
            if (minOccurs == 0) {
                element.setMinOccurs(minOccurs);
            }

            
            element.setNillable(getTypeInfo().isNillable(name));
        } else {
            element.setRefName(type.getSchemaType());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTypeClass(Class typeClass) {
        super.setTypeClass(typeClass);

        initTypeClass();
    }

    /**
     * We need to write a complex type schema for Beans, so return true.
     * 
     * @see org.apache.cxf.aegis.type.Type#isComplex()
     */
    @Override
    public boolean isComplex() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Type> getDependencies() {
        Set<Type> deps = new HashSet<Type>();

        BeanTypeInfo inf = getTypeInfo();

        for (QName name : inf.getAttributes()) {
            deps.add(inf.getType(name));
        }

        for (QName name : inf.getElements()) {
            if (inf.isExtension()
                && inf.getPropertyDescriptorFromMappedName(name).getReadMethod().getDeclaringClass() != inf
                    .getTypeClass()) {
                continue;
            }
            deps.add(inf.getType(name));
        }

        /*
         * Automagically add chain of superclasses if this is an an extension.
         */
        if (inf.isExtension()) {
            Type sooperType = getSuperType();
            if (sooperType != null) {
                deps.add(sooperType);
            }
        }

        return deps;
    }

    protected BeanTypeInfo getBeanTypeInfoWithProperty(QName name) {
        // search the BeanType superType tree for the first BeanType with a property named 'name'
        BeanType beanType = this;
        Type type = null;
        while (type == null && beanType != null) {
            type = beanType.getTypeInfo().getType(name);

            if (type == null) {
                Type superType = beanType.getSuperType(); /*
                                                           * The class might inherit from, say, 'Integer'. In
                                                           * which case we've got no BeanType to work with.
                                                           */
                if (superType instanceof BeanType) {
                    beanType = (BeanType)superType;
                } else {
                    break; // give up.
                }
            }
        }

        BeanTypeInfo elementTypeInfo;
        if (beanType != null) {
            elementTypeInfo = beanType.getTypeInfo();
        } else {
            // didn't find a bean type so just use this bean's type info
            elementTypeInfo = getTypeInfo();
        }
        return elementTypeInfo;
    }

    /**
     * Return the Type for the superclass if this type's class, if any.
     * @return
     */
    public Type getSuperType() {
        BeanTypeInfo inf = getTypeInfo();
        Class c = inf.getTypeClass().getSuperclass();
        /*
         * Don't dig any deeper than Object or Exception
         */
        if (c != null && c != Object.class && c != Exception.class && c != RuntimeException.class) {
            TypeMapping tm = inf.getTypeMapping();
            Type superType = tm.getType(c);
            if (superType == null) {
                // if we call createType, we know that we'll get a BeanType. */
                superType = (BeanType)getTypeMapping().getTypeCreator().createType(c);
                Class cParent = c.getSuperclass();
                if (cParent != null && cParent != Object.class) {
                    ((BeanType)superType).getTypeInfo().setExtension(true);
                }
                tm.register(superType);
            }
            return superType;
        } else {
            return null;
        }
    }

    /**
     * Return the type info.
     * @return
     */
    public BeanTypeInfo getTypeInfo() {
        if (info == null) {
            info = createTypeInfo();
        }

        info.initialize();

        return info;
    }

    /**
     * Create type info based in the type class.
     * @return
     */
    public BeanTypeInfo createTypeInfo() {
        BeanTypeInfo inf = new BeanTypeInfo(getTypeClass(), getSchemaType().getNamespaceURI());

        inf.setTypeMapping(getTypeMapping());

        return inf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(": [class=");
        Class c = getTypeClass();
        sb.append((c == null) ? "<null>" : c.getName());
        sb.append(",\nQName=");
        QName q = getSchemaType();
        sb.append((q == null) ? "<null>" : q.toString());
        sb.append(",\ninfo=");
        sb.append(getTypeInfo().toString());
        sb.append("]");
        return sb.toString();
    }

    private void addXmimeToSchema(XmlSchema root) {
        XmlSchemaUtils.addImportIfNeeded(root, AbstractXOPType.XML_MIME_NS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMinOccurs() {
        return getTypeInfo().getMinOccurs();
    }

    @Override
    public boolean hasMinOccurs() {
        return true;
    }

    @Override
    public void setTypeMapping(TypeMapping typeMapping) {
        super.setTypeMapping(typeMapping);
        if (info != null) {
            // this seems dangerous .. what if the type info is later created, it won't be passed the mapping.
            info.setTypeMapping(typeMapping);
        }
    }
    
    @Override
    public void writeSchema(XmlSchema root) {
        BeanTypeInfo inf = getTypeInfo();
        XmlSchemaComplexType complex = new XmlSchemaComplexType(root);
        complex.setName(getSchemaType().getLocalPart());
        root.addType(complex);
        root.getItems().add(complex);

        Type sooperType = getSuperType();

        /*
         * See Java Virtual Machine specification:
         * http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#75734
         */
        if (((inf.getTypeClass().getModifiers() & Modifier.ABSTRACT) != 0)
            && !inf.getTypeClass().isInterface()) {
            complex.setAbstract(true);
        }

        XmlSchemaSequence sequence = new XmlSchemaSequence();
        /*
         * Decide if we're going to extend another type. If we are going to defer, then make sure that we
         * extend the type for our superclass.
         */
        boolean isExtension = inf.isExtension();

        if (isExtension && sooperType != null) {
            // if sooperType is null, things are confused.
            XmlSchemaComplexContent content = new XmlSchemaComplexContent();
            complex.setContentModel(content);
            XmlSchemaComplexContentExtension extension = new XmlSchemaComplexContentExtension();
            content.setContent(extension);
            extension.setBaseTypeName(sooperType.getSchemaType());
            extension.setParticle(sequence);
        } else {
            complex.setParticle(sequence);
        }

        boolean needXmime = false;
        boolean needUtilityTypes = false;

        // Write out schema for elements
        for (QName name : inf.getElements()) {

            if (isExtension) {
                PropertyDescriptor pd = inf.getPropertyDescriptorFromMappedName(name);

                // assert pd.getReadMethod() != null && pd.getWriteMethod() != null;

                if (pd.getReadMethod().getDeclaringClass() != inf.getTypeClass()) {
                    continue;
                }
            }

            XmlSchemaElement element = new XmlSchemaElement();
            element.setName(name.getLocalPart());
            sequence.getItems().add(element);

            Type type = getType(inf, name);
            if (AbstractTypeCreator.
                HTTP_CXF_APACHE_ORG_ARRAYS.equals(type.getSchemaType().getNamespaceURI())) {
                XmlSchemaUtils.addImportIfNeeded(root, AbstractTypeCreator.HTTP_CXF_APACHE_ORG_ARRAYS);
            }

            writeTypeReference(name, element, type, root);
            needXmime |= type.usesXmime();
            needUtilityTypes |= type.usesUtilityTypes();
        }

        if (needXmime) {
            addXmimeToSchema(root);
        }

        if (needUtilityTypes) {
            AegisContext.addUtilityTypesToSchema(root);
        }

        /**
         * if future proof then add <xsd:any/> element
         */
        if (inf.isExtensibleElements()) {
            XmlSchemaAny any = new XmlSchemaAny();
            any.setMinOccurs(0);
            any.setMaxOccurs(Long.MAX_VALUE);
            sequence.getItems().add(any);
        }

        // Write out schema for attributes
        for (QName name : inf.getAttributes()) {
            XmlSchemaAttribute attribute = new XmlSchemaAttribute();
            complex.getAttributes().add(attribute);
            attribute.setName(name.getLocalPart());
            Type type = getType(inf, name);
            attribute.setSchemaTypeName(type.getSchemaType());
            String ns = name.getNamespaceURI();
            if (!ns.equals(root.getTargetNamespace())) {
                XmlSchemaUtils.addImportIfNeeded(root, ns);
            }
        }

        /**
         * If extensible attributes then add <xsd:anyAttribute/>
         */
        if (inf.isExtensibleAttributes()) {
            sequence.getItems().add(new XmlSchemaAnyAttribute());
        }
    }

}

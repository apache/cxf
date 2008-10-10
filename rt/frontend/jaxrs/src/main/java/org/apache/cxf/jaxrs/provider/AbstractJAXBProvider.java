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

package org.apache.cxf.jaxrs.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public abstract class AbstractJAXBProvider 
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractJAXBProvider.class);

    private static final String CHARSET_PARAMETER = "charset"; 
    private static final String CLASSPATH_PREFIX = "classpath:";
        
    private static Map<String, JAXBContext> packageContexts = new WeakHashMap<String, JAXBContext>();
    private static Map<Class<?>, JAXBContext> classContexts = new WeakHashMap<Class<?>, JAXBContext>();
    
    @Context protected ContextResolver<JAXBContext> resolver;
    private Schema schema;
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns) {
        return isSupported(type, genericType, anns)
               || AnnotationUtils.getAnnotation(anns, XmlJavaTypeAdapter.class) != null;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations) {
        return isSupported(type, genericType, annotations);
    }

    public void setSchemas(List<String> locations) {
        schema = createSchema(locations);    
    }
    
    public long getSize(Object o) {
        return -1;
    }

    protected JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        
        if (resolver != null) {
            JAXBContext context = resolver.getContext(type);
            // it's up to the resolver to keep its contexts in a map
            if (context != null) {
                return context;
            }
        }
        
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context != null) {
                return context;
            }
        }
        
        JAXBContext context = getPackageContext(type);
        if (context == null && type != genericType) {
            context = getPackageContext(InjectionUtils.getActualType(genericType));
        }
        
        return context != null ? context : getClassContext(type);
    }
    
    private JAXBContext getClassContext(Class<?> type) throws JAXBException {
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context == null) {
                context = JAXBContext.newInstance(new Class[]{type});
                classContexts.put(type, context);
            }
            return context;
        }
    }
    
    private JAXBContext getPackageContext(Class<?> type) {
        if (type == null) {
            return null;
        }
        synchronized (packageContexts) {
            String packageName = PackageUtils.getPackageName(type);
            JAXBContext context = packageContexts.get(packageName);
            if (context == null) {
                try {
                    context = JAXBContext.newInstance(packageName, type.getClassLoader());
                    packageContexts.put(packageName, context);
                    return context;
                } catch (JAXBException ex) {
                    LOG.fine("Error creating a JAXBContext using ObjectFactory : " 
                                + ex.getMessage());
                    return null;
                }
            }
        }
        return null;
    }
    
    protected boolean isSupported(Class<?> type, Type genericType, Annotation[] annotations) {
        
        return type.getAnnotation(XmlRootElement.class) != null
            || JAXBElement.class.isAssignableFrom(type)
            || objectFactoryForClass(type)
            || (type != genericType && objectFactoryForType(genericType));
    
    }
    
    private boolean objectFactoryForClass(Class<?> type) {
        try {
            return type.getClassLoader().loadClass(PackageUtils.getPackageName(type) 
                                        + ".ObjectFactory") != null;
        } catch (Exception ex) {
            return false;
        }
    }
    
    private boolean objectFactoryForType(Type genericType) {
        return objectFactoryForClass(InjectionUtils.getActualType(genericType));
    }
    
    @SuppressWarnings("deprecation")
    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType) 
        throws JAXBException {
        JAXBContext context = getJAXBContext(cls, genericType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        return unmarshaller;        
    }
    
    protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, MediaType m)
        throws JAXBException {
        
        Class<?> objClazz = JAXBElement.class.isAssignableFrom(cls) 
                            ? ((JAXBElement)obj).getDeclaredType() : cls;
        JAXBContext context = getJAXBContext(objClazz, genericType);
        Marshaller marshaller = context.createMarshaller();
        String enc = m.getParameters().get(CHARSET_PARAMETER);
        if (enc != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, enc);
        }
        return marshaller;
    }
    
    protected Class<?> getActualType(Class<?> type, Type genericType) {
        Class<?> theType = null;
        if (JAXBElement.class.isAssignableFrom(type)) {
            theType = InjectionUtils.getActualType(genericType);
        } else {
            theType = type;
        }
        
        return theType;
    }
    
    @SuppressWarnings("unchecked")
    protected Object checkAdapter(Object obj, Annotation[] anns) {
        XmlJavaTypeAdapter typeAdapter = AnnotationUtils.getAnnotation(anns, XmlJavaTypeAdapter.class);
        if (typeAdapter != null) {
            try {
                XmlAdapter xmlAdapter = typeAdapter.value().newInstance();
                return xmlAdapter.marshal(obj);
            } catch (Exception ex) {
                LOG.warning("Problem using the XmlJavaTypeAdapter");
                ex.printStackTrace();
            }
        }
        return obj;
    }
    
    private Schema createSchema(List<String> locations) {
        
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema s = null;
        try {
            List<Source> sources = new ArrayList<Source>();
            for (String loc : locations) {
                InputStream is = null;
                if (loc.startsWith(CLASSPATH_PREFIX)) {
                    String path = loc.substring(CLASSPATH_PREFIX.length() + 1);
                    is = getClass().getClassLoader().getResourceAsStream(path);
                    if (is == null) {
                        LOG.warning("No schema resource " + loc + " is available on classpath");
                        return null;
                    }
                } else {
                    File f = new File(loc);
                    if (!f.exists()) {
                        LOG.warning("No schema resource " + loc + " is available on local disk");
                        return null;
                    }
                    is = new FileInputStream(f);
                }
                                
                Reader r = new BufferedReader(
                               new InputStreamReader(is, "UTF-8"));
                sources.add(new StreamSource(r));
            }
            s = factory.newSchema(sources.toArray(new Source[]{}));
        } catch (Exception ex) {
            LOG.warning("Validation will be disabled, failed to create schema : " + ex.getMessage());
        }
        return s;
        
    }
    
    protected Schema getSchema() {
        return schema;
    }
}

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

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.validation.Schema;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;

public abstract class AbstractJAXBProvider extends AbstractConfigurableProvider
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractJAXBProvider.class);
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractJAXBProvider.class);
    
    private static final String CHARSET_PARAMETER = "charset";
    private static Map<String, JAXBContext> packageContexts = new WeakHashMap<String, JAXBContext>();
    private static Map<Class<?>, JAXBContext> classContexts = new WeakHashMap<Class<?>, JAXBContext>();
   
    private MessageContext mc;
    private Schema schema;
    
    protected void setContext(MessageContext context) {
        mc = context;
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        return isSupported(type, genericType, anns);
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        return isSupported(type, genericType, anns);
    }

    public void setSchemaLocations(List<String> locations) {
        schema = SchemaHandler.createSchema(locations, getBus());    
    }
    
    public void setSchema(Schema s) {
        schema = s;    
    }
    
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    protected MessageContext getContext() {
        return mc;
    }
    
    @SuppressWarnings("unchecked")
    protected JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        if (mc != null) {
            ContextResolver<JAXBContext> resolver = 
                mc.getResolver(ContextResolver.class, JAXBContext.class);
            if (resolver != null) {
                JAXBContext customContext = resolver.getContext(type);
                if (customContext != null) {
                    return customContext;
                }
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
    
    // TODO : move this method to a dedicated JAXBContextRegistry class
    public JAXBContext getClassContext(Class<?> type) throws JAXBException {
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context == null) {
                context = JAXBContext.newInstance(new Class[]{type});
                classContexts.put(type, context);
            }
            return context;
        }
    }
    
    //  TODO : move this method to a dedicated JAXBContextRegistry class
    public JAXBContext getPackageContext(Class<?> type) {
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
                } catch (JAXBException ex) {
                    LOG.fine("Error creating a JAXBContext using ObjectFactory : " 
                                + ex.getMessage());
                    return null;
                }
            }
            return context;
        }
    }
    
    protected boolean isSupported(Class<?> type, Type genericType, Annotation[] anns) {
        // TODO : Shall we just return true and let readFrom/writeTo 
        // fail if JAXB can't handle a given type ?
        
        // TODO: still not checked : 
        // - XmlJavaTypeAdapter at package level
        // - anything else ?
        return type.getAnnotation(XmlRootElement.class) != null
            || JAXBElement.class.isAssignableFrom(type)
            || objectFactoryForClass(type)
            || (type != genericType && objectFactoryForType(genericType))
            || adapterAvailable(type, anns);
    
    }
    
    protected boolean adapterAvailable(Class<?> type, Annotation[] anns) {
        return AnnotationUtils.getAnnotation(anns, XmlJavaTypeAdapter.class) != null
               || type.getAnnotation(XmlJavaTypeAdapter.class) != null;
    }
    
    protected boolean objectFactoryForClass(Class<?> type) {
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
    
    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType) 
        throws JAXBException {
        JAXBContext context = getJAXBContext(cls, genericType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        return unmarshaller;        
    }
    
    protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, String enc)
        throws JAXBException {
        
        Class<?> objClazz = JAXBElement.class.isAssignableFrom(cls) 
                            ? ((JAXBElement)obj).getDeclaredType() : cls;
        JAXBContext context = getJAXBContext(objClazz, genericType);
        Marshaller marshaller = context.createMarshaller();
        if (enc != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, enc);
        }
        return marshaller;
    }
    
    protected String getEncoding(MediaType mt, MultivaluedMap<String, Object> headers) {
        String enc = mt.getParameters().get(CHARSET_PARAMETER);
        if (enc == null) {
            return null;
        }
        try {
            "0".getBytes(enc);
            return enc;
        } catch (UnsupportedEncodingException ex) {
            String message = new org.apache.cxf.common.i18n.Message("UNSUPPORTED_ENCODING", 
                                 BUNDLE, enc).toString();
            LOG.warning(message);
            headers.putSingle(HttpHeaders.CONTENT_TYPE, 
                JAXRSUtils.removeMediaTypeParameter(mt, CHARSET_PARAMETER) 
                + ';' + CHARSET_PARAMETER + "=UTF-8");
        }
        return null;
    }
        

    
    protected Class<?> getActualType(Class<?> type, Type genericType, Annotation[] anns) {
        Class<?> theType = null;
        if (JAXBElement.class.isAssignableFrom(type)) {
            theType = InjectionUtils.getActualType(genericType);
        } else {
            theType = type;
        }
        XmlJavaTypeAdapter adapter = getAdapter(theType, anns);
        if (adapter != null) {
            if (adapter.type() != XmlJavaTypeAdapter.DEFAULT.class) {
                theType = adapter.type();
            } else {
                Type[] types = InjectionUtils.getActualTypes(adapter.value().getGenericSuperclass());
                if (types != null && types.length == 2) {
                    theType = (Class)types[0];
                }
            }
        }
        
        return theType;
    }
    
    @SuppressWarnings("unchecked")
    protected Object checkAdapter(Object obj, Annotation[] anns, boolean marshal) {
        XmlJavaTypeAdapter typeAdapter = getAdapter(obj.getClass(), anns); 
        if (typeAdapter != null) {
            try {
                XmlAdapter xmlAdapter = typeAdapter.value().newInstance();
                if (marshal) {
                    return xmlAdapter.marshal(obj);
                } else {
                    return xmlAdapter.unmarshal(obj);
                }
            } catch (Exception ex) {
                LOG.warning("Problem using the XmlJavaTypeAdapter");
                ex.printStackTrace();
            }
        }
        return obj;
    }
    
    protected XmlJavaTypeAdapter getAdapter(Class<?> type, Annotation[] anns) {
        XmlJavaTypeAdapter typeAdapter = AnnotationUtils.getAnnotation(anns, XmlJavaTypeAdapter.class);
        if (typeAdapter == null) {
            typeAdapter = type.getAnnotation(XmlJavaTypeAdapter.class);
        }
        return typeAdapter;
    }
    
    
    protected Schema getSchema() {
        return schema;
    }

    
    static void clearContexts() {
        classContexts.clear();
        packageContexts.clear();
    }
    
    protected static void handleJAXBException(JAXBException e) {
        Throwable t = e.getLinkedException() != null 
            ? e.getLinkedException() : e.getCause() != null ? e.getCause() : e;
        String message = new org.apache.cxf.common.i18n.Message("JAXB_EXCEPTION", 
                             BUNDLE, t.getMessage()).toString();
        LOG.warning(message);
        Response r = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.TEXT_PLAIN).entity(message).build();
        throw new WebApplicationException(t, r);
    }
}

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
package org.apache.cxf.jaxrs.servlet;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

public class CXFNonSpringJaxrsServlet extends CXFNonSpringServlet {

    private static final Logger LOG = LogUtils.getL7dLogger(CXFNonSpringJaxrsServlet.class);
    
    private static final String USER_MODEL_PARAM = "user.model";
    private static final String SERVICE_ADDRESS_PARAM = "jaxrs.address";
    private static final String IGNORE_APP_PATH_PARAM = "jaxrs.application.address.ignore";
    private static final String SERVICE_CLASSES_PARAM = "jaxrs.serviceClasses";
    private static final String PROVIDERS_PARAM = "jaxrs.providers";
    private static final String OUT_INTERCEPTORS_PARAM = "jaxrs.outInterceptors";
    private static final String IN_INTERCEPTORS_PARAM = "jaxrs.inInterceptors";
    private static final String SERVICE_SCOPE_PARAM = "jaxrs.scope";
    private static final String EXTENSIONS_PARAM = "jaxrs.extensions";
    private static final String LANGUAGES_PARAM = "jaxrs.languages";
    private static final String PROPERTIES_PARAM = "jaxrs.properties";
    private static final String SCHEMAS_PARAM = "jaxrs.schemaLocations";
    private static final String STATIC_SUB_RESOLUTION_PARAM = "jaxrs.static.subresources";
    private static final String SERVICE_SCOPE_SINGLETON = "singleton";
    private static final String SERVICE_SCOPE_REQUEST = "prototype";
    
    private static final String JAXRS_APPLICATION_PARAM = "javax.ws.rs.Application";
    
    @Override
    public void loadBus(ServletConfig servletConfig) throws ServletException {
        super.loadBus(servletConfig);
        
        String applicationClass = servletConfig.getInitParameter(JAXRS_APPLICATION_PARAM);
        if (applicationClass != null) {
            createServerFromApplication(applicationClass, servletConfig);
            return;
        }
        
        JAXRSServerFactoryBean bean = new JAXRSServerFactoryBean();
        
        String address = servletConfig.getInitParameter(SERVICE_ADDRESS_PARAM);
        if (address == null) {
            address = "/";
        }
        bean.setAddress(address);
        
        bean.setStaticSubresourceResolution(getStaticSubResolutionValue(servletConfig));
        
        String modelRef = servletConfig.getInitParameter(USER_MODEL_PARAM);
        if (modelRef != null) {
            bean.setModelRef(modelRef.trim());
        }
        
        setSchemasLocations(bean, servletConfig);
        setAllInterceptors(bean, servletConfig);
        
        Map<Class, Map<String, String>> resourceClasses = 
            getServiceClasses(servletConfig, modelRef != null);
        Map<Class, ResourceProvider> resourceProviders = 
            getResourceProviders(servletConfig, resourceClasses);
        
        List<?> providers = getProviders(servletConfig);
                
        bean.setResourceClasses(new ArrayList<Class>(resourceClasses.keySet()));
        bean.setProviders(providers);
        for (Map.Entry<Class, ResourceProvider> entry : resourceProviders.entrySet()) {
            bean.setResourceProvider(entry.getKey(), entry.getValue());
        }
        setExtensions(bean, servletConfig);
                
        bean.create();
    }

    protected boolean getStaticSubResolutionValue(ServletConfig servletConfig) {
        String param = servletConfig.getInitParameter(STATIC_SUB_RESOLUTION_PARAM);
        if (param != null) {
            return Boolean.valueOf(param.trim());
        } else {
            return false;
        }
    }
    
    protected void setExtensions(JAXRSServerFactoryBean bean, ServletConfig servletConfig) {
        bean.setExtensionMappings(handleMapSequence(servletConfig.getInitParameter(EXTENSIONS_PARAM)));
        bean.setLanguageMappings(handleMapSequence(servletConfig.getInitParameter(LANGUAGES_PARAM)));
        bean.setProperties(CastUtils.cast(
                handleMapSequence(servletConfig.getInitParameter(PROPERTIES_PARAM)),
                String.class, Object.class));
    }
    
    protected Map<Object, Object> handleMapSequence(String sequence) {
        if (sequence != null) {
            sequence = sequence.trim();
            Map<Object, Object> map = new HashMap<Object, Object>();
            String[] pairs = sequence.split(" ");
            for (String pair : pairs) {
                String[] value = pair.split("=");
                if (value.length == 2) {
                    map.put(value[0].trim(), value[1].trim());
                }
            }
            return map;
        } else {
            return Collections.emptyMap();    
        }
    }
    
    protected void setAllInterceptors(JAXRSServerFactoryBean bean, ServletConfig servletConfig) {
        setInterceptors(bean, servletConfig, OUT_INTERCEPTORS_PARAM);
        setInterceptors(bean, servletConfig, IN_INTERCEPTORS_PARAM);
    }
    
    protected void setSchemasLocations(JAXRSServerFactoryBean bean, ServletConfig servletConfig) {
        String schemas = servletConfig.getInitParameter(SCHEMAS_PARAM);
        if (schemas == null) {
            return;
        }
        String[] locations = schemas.split(" ");
        List<String> list = new ArrayList<String>();
        for (String loc : locations) {
            String theLoc = loc.trim();
            if (theLoc.length() != 0) {
                list.add(theLoc);
            }
        }
        if (list.size() > 0) {
            bean.setSchemaLocations(list);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void setInterceptors(JAXRSServerFactoryBean bean, ServletConfig servletConfig,
                                   String paramName) {
        String value  = servletConfig.getInitParameter(paramName);
        if (value == null) {
            return;
        }
        String[] values = value.split(" ");
        List<Interceptor<? extends Message>> list = new ArrayList<Interceptor<? extends Message>>();
        for (String interceptorVal : values) {
            Map<String, String> props = new HashMap<String, String>();
            String theValue = getClassNameAndProperties(interceptorVal, props);
            if (theValue.length() != 0) {
                try {
                    Class<?> intClass = ClassLoaderUtils.loadClass(theValue,
                                                                   CXFNonSpringJaxrsServlet.class);
                    Object object = intClass.newInstance();
                    injectProperties(object, props);
                    list.add((Interceptor<? extends Message>)object);
                } catch (ClassNotFoundException ex) {
                    LOG.warning("Interceptor class " + theValue + " can not be found");
                } catch (InstantiationException ex) {
                    LOG.warning(theValue + " class can not be instantiated");
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    LOG.warning("CXF Interceptor can not be instantiated due to IllegalAccessException"); 
                } catch (ClassCastException ex) {
                    LOG.warning(theValue + " class does not implement " + Interceptor.class.getName()); 
                }
            }
        }
        if (list.size() > 0) {
            if (OUT_INTERCEPTORS_PARAM.equals(paramName)) {
                bean.setOutInterceptors(list);
            } else {
                bean.setInInterceptors(list);
            }
        }
    }
    
    protected Map<Class, Map<String, String>> getServiceClasses(ServletConfig servletConfig,
                                            boolean modelAvailable) throws ServletException {
        String serviceBeans = servletConfig.getInitParameter(SERVICE_CLASSES_PARAM);
        if (serviceBeans == null) {
            if (modelAvailable) {
                return Collections.emptyMap();
            }
            throw new ServletException("At least one resource class should be specified");
        }
        String[] classNames = serviceBeans.split(" ");
        Map<Class, Map<String, String>> map = new HashMap<Class, Map<String, String>>();
        for (String cName : classNames) {
            Map<String, String> props = new HashMap<String, String>();
            String theName = getClassNameAndProperties(cName, props);
            if (theName.length() != 0) {
                Class<?> cls = loadClass(theName);
                map.put(cls, props);
            }
        }
        if (map.isEmpty()) {
            throw new ServletException("At least one resource class should be specified");
        }
        return map;
    }
    
    protected List<?> getProviders(ServletConfig servletConfig) throws ServletException {
        String providersList = servletConfig.getInitParameter(PROVIDERS_PARAM);
        if (providersList == null) {
            return Collections.EMPTY_LIST;
        }
        String[] classNames = providersList.split(" ");
        List<Object> providers = new ArrayList<Object>();
        for (String cName : classNames) {
            Map<String, String> props = new HashMap<String, String>();
            String theName = getClassNameAndProperties(cName, props);
            if (theName.length() != 0) {
                Class<?> cls = loadClass(theName);
                providers.add(createSingletonInstance(cls, props, servletConfig));
            }
        }
        return providers;
    }
    
    private String getClassNameAndProperties(String cName, Map<String, String> props) {
        String theName = cName.trim();
        int ind = theName.indexOf("(");
        if (ind != -1 && theName.endsWith(")")) {
            props.putAll(CastUtils.cast(handleMapSequence(theName.substring(ind + 1, theName.length() - 1)),
                    String.class, String.class));
            theName = theName.substring(0, ind).trim();
        }
        return theName;
    }
    
    protected Map<Class, ResourceProvider> getResourceProviders(ServletConfig servletConfig,
            Map<Class, Map<String, String>> resourceClasses) throws ServletException {
        String scope = servletConfig.getInitParameter(SERVICE_SCOPE_PARAM);
        if (scope != null && !SERVICE_SCOPE_SINGLETON.equals(scope)
            && !SERVICE_SCOPE_REQUEST.equals(scope)) {
            throw new ServletException("Only singleton and prototype scopes are supported");
        }
        boolean isPrototype = SERVICE_SCOPE_REQUEST.equals(scope);
        Map<Class, ResourceProvider> map = new HashMap<Class, ResourceProvider>();
        for (Map.Entry<Class, Map<String, String>> entry : resourceClasses.entrySet()) {
            Class<?> c = entry.getKey();
            map.put(c, isPrototype ? new PerRequestResourceProvider(c)
                                   : new SingletonResourceProvider(
                                         createSingletonInstance(c, entry.getValue(), servletConfig), 
                                         true));
        }
        return map;
    }    
    
    
    protected Object createSingletonInstance(Class<?> cls, Map<String, String> props, ServletConfig sc) 
        throws ServletException {
        Constructor c = ResourceUtils.findResourceConstructor(cls, false);
        if (c == null) {
            throw new ServletException("No valid constructor found for " + cls.getName());
        }
        boolean isDefault = c.getParameterTypes().length == 0; 
        if (!isDefault && (c.getParameterTypes().length != 1 
            || c.getParameterTypes()[0] != ServletConfig.class
            && c.getParameterTypes()[0] != ServletContext.class)) {
            throw new ServletException("Resource classes with singleton scope can only have "
                + "ServletConfig or ServletContext instances injected through their constructors");
        }
        Object[] values = isDefault ? new Object[]{} 
            : new Object[]{c.getParameterTypes()[0] == ServletConfig.class ? sc : sc.getServletContext()}; 
        try {
            Object instance = c.newInstance(values);
            injectProperties(instance, props);
            configureSingleton(instance);
            return instance;
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            throw new ServletException("Resource class " + cls.getName()
                                       + " can not be instantiated"); 
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            throw new ServletException("Resource class " + cls.getName()
                                       + " can not be instantiated due to IllegalAccessException"); 
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            throw new ServletException("Resource class " + cls.getName()
                                       + " can not be instantiated due to InvocationTargetException"); 
        }
    }
    
    private void injectProperties(Object instance, Map<String, String> props) {
        if (props == null || props.isEmpty()) {
            return;
        }
        Method[] methods = instance.getClass().getMethods();
        Map<String, Method> methodsMap = new HashMap<String, Method>();
        for (Method m : methods) {
            methodsMap.put(m.getName(), m);
        }
        for (Map.Entry<String, String> entry : props.entrySet()) {
            Method m = methodsMap.get("set" + Character.toUpperCase(entry.getKey().charAt(0))
                           + entry.getKey().substring(1));
            if (m != null) {
                Class<?> type = m.getParameterTypes()[0];
                Object value = entry.getValue();
                if (InjectionUtils.isPrimitive(type)) {
                    value = PrimitiveUtils.read(entry.getValue(), type);
                } else if (List.class.isAssignableFrom(type)) {
                    value = Collections.singletonList(value);
                } 
                InjectionUtils.injectThroughMethod(instance, m, value);
            }
        }
    }
    
    protected void configureSingleton(Object instance) {
        
    }
    
    protected void createServerFromApplication(String cName, ServletConfig servletConfig) 
        throws ServletException {
        Map<String, String> props = new HashMap<String, String>();
        cName = getClassNameAndProperties(cName, props);
        Class<?> appClass = loadClass(cName, "Application");
        Application app = (Application)createSingletonInstance(appClass, props, servletConfig);
        
        String ignoreParam = servletConfig.getInitParameter(IGNORE_APP_PATH_PARAM);
        JAXRSServerFactoryBean bean = ResourceUtils.createApplication(app, 
                                            MessageUtils.isTrue(ignoreParam),
                                            getStaticSubResolutionValue(servletConfig));
        setAllInterceptors(bean, servletConfig);
        setExtensions(bean, servletConfig);
        setSchemasLocations(bean, servletConfig);
        
        bean.create();
    }
    
    private Class<?> loadClass(String cName) throws ServletException {
        return loadClass(cName, "Resource");
    }
    
    private Class<?> loadClass(String cName, String classType) throws ServletException {
        try {
            return ClassLoaderUtils.loadClass(cName, CXFNonSpringJaxrsServlet.class);
        } catch (ClassNotFoundException ex) {
            throw new ServletException("No " + classType + " class " + cName.trim() + " can be found", ex); 
        }
    }
    
    
}

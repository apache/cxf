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
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
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
    private static final String SCHEMAS_PARAM = "jaxrs.schemaLocations";
    private static final String SERVICE_SCOPE_SINGLETON = "singleton";
    private static final String SERVICE_SCOPE_REQUEST = "prototype";
    
    private static final String JAXRS_APPLICATION_PARAM = "javax.ws.rs.Application";
    
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String applicationClass = servletConfig.getInitParameter(JAXRS_APPLICATION_PARAM);
        if (applicationClass != null) {
            createServerFromApplication(applicationClass, servletConfig);
            return;
        }
        
        JAXRSServerFactoryBean bean = new JAXRSServerFactoryBean();
        bean.setBus(getBus());
        
        String address = servletConfig.getInitParameter(SERVICE_ADDRESS_PARAM);
        if (address == null) {
            address = "/";
        }
        bean.setAddress(address);
        String modelRef = servletConfig.getInitParameter(USER_MODEL_PARAM);
        if (modelRef != null) {
            bean.setModelRef(modelRef.trim());
        }
        
        setSchemasLocations(bean, servletConfig);
        setAllInterceptors(bean, servletConfig);
        
        List<Class> resourceClasses = getServiceClasses(servletConfig, modelRef != null);
        Map<Class, ResourceProvider> resourceProviders = 
            getResourceProviders(servletConfig, resourceClasses);
        
        List<?> providers = getProviders(servletConfig);
                
        bean.setResourceClasses(resourceClasses);
        bean.setProviders(providers);
        for (Map.Entry<Class, ResourceProvider> entry : resourceProviders.entrySet()) {
            bean.setResourceProvider(entry.getKey(), entry.getValue());
        }
        bean.create();
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
            String theValue = interceptorVal.trim();
            if (theValue.length() != 0) {
                try {
                    Class<?> intClass = ClassLoaderUtils.loadClass(theValue,
                                                                   CXFNonSpringJaxrsServlet.class);
                    list.add((Interceptor<? extends Message>)intClass.newInstance());
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
    
    protected List<Class> getServiceClasses(ServletConfig servletConfig,
                                            boolean modelAvailable) throws ServletException {
        String serviceBeans = servletConfig.getInitParameter(SERVICE_CLASSES_PARAM);
        if (serviceBeans == null) {
            if (modelAvailable) {
                return Collections.emptyList();
            }
            throw new ServletException("At least one resource class should be specified");
        }
        String[] classNames = serviceBeans.split(" ");
        List<Class> resourceClasses = new ArrayList<Class>();
        for (String cName : classNames) {
            String theName = cName.trim();
            if (theName.length() != 0) {
                Class<?> cls = loadClass(theName);
                resourceClasses.add(cls);
            }
        }
        if (resourceClasses.isEmpty()) {
            throw new ServletException("At least one resource class should be specified");
        }
        return resourceClasses;
    }
    
    protected List<?> getProviders(ServletConfig servletConfig) throws ServletException {
        String providersList = servletConfig.getInitParameter(PROVIDERS_PARAM);
        if (providersList == null) {
            return Collections.EMPTY_LIST;
        }
        String[] classNames = providersList.split(" ");
        List<Object> providers = new ArrayList<Object>();
        for (String cName : classNames) {
            String theName = cName.trim();
            if (theName.length() != 0) {
                Class<?> cls = loadClass(theName);
                providers.add(createSingletonInstance(cls, servletConfig));
            }
        }
        return providers;
    }
    
    protected Map<Class, ResourceProvider> getResourceProviders(ServletConfig servletConfig,
        List<Class> resourceClasses) throws ServletException {
        String scope = servletConfig.getInitParameter(SERVICE_SCOPE_PARAM);
        if (scope != null && !SERVICE_SCOPE_SINGLETON.equals(scope)
            && !SERVICE_SCOPE_REQUEST.equals(scope)) {
            throw new ServletException("Only singleton and prototype scopes are supported");
        }
        boolean isPrototype = SERVICE_SCOPE_REQUEST.equals(scope);
        Map<Class, ResourceProvider> map = new HashMap<Class, ResourceProvider>();
        for (Class c : resourceClasses) {
            map.put(c, isPrototype ? new PerRequestResourceProvider(c)
                                   : new SingletonResourceProvider(
                                         createSingletonInstance(c, servletConfig), true));
        }
        return map;
    }    
    
    
    protected Object createSingletonInstance(Class<?> cls, ServletConfig sc) throws ServletException {
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
    
    protected void configureSingleton(Object instance) {
        
    }
    
    protected void createServerFromApplication(String cName, ServletConfig servletConfig) 
        throws ServletException {
        Class<?> appClass = loadClass(cName, "Application");
        Application app = (Application)createSingletonInstance(appClass, servletConfig);
        
        String ignoreParam = servletConfig.getInitParameter(IGNORE_APP_PATH_PARAM);
        JAXRSServerFactoryBean bean = ResourceUtils.createApplication(app, MessageUtils.isTrue(ignoreParam));
        setAllInterceptors(bean, servletConfig);
        bean.setBus(getBus());
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

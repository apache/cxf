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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.PerRequestResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

public class CXFNonSpringJaxrsServlet extends CXFNonSpringServlet {

    private static final long serialVersionUID = -8916352798780577499L;

    private static final Logger LOG = LogUtils.getL7dLogger(CXFNonSpringJaxrsServlet.class);

    private static final String USER_MODEL_PARAM = "user.model";
    private static final String SERVICE_ADDRESS_PARAM = "jaxrs.address";
    private static final String IGNORE_APP_PATH_PARAM = "jaxrs.application.address.ignore";
    private static final String SERVICE_CLASSES_PARAM = "jaxrs.serviceClasses";
    private static final String PROVIDERS_PARAM = "jaxrs.providers";
    private static final String FEATURES_PARAM = "jaxrs.features";
    private static final String OUT_INTERCEPTORS_PARAM = "jaxrs.outInterceptors";
    private static final String OUT_FAULT_INTERCEPTORS_PARAM = "jaxrs.outFaultInterceptors";
    private static final String IN_INTERCEPTORS_PARAM = "jaxrs.inInterceptors";
    private static final String INVOKER_PARAM = "jaxrs.invoker";
    private static final String SERVICE_SCOPE_PARAM = "jaxrs.scope";
    private static final String EXTENSIONS_PARAM = "jaxrs.extensions";
    private static final String LANGUAGES_PARAM = "jaxrs.languages";
    private static final String PROPERTIES_PARAM = "jaxrs.properties";
    private static final String SCHEMAS_PARAM = "jaxrs.schemaLocations";
    private static final String DOC_LOCATION_PARAM = "jaxrs.documentLocation";
    private static final String STATIC_SUB_RESOLUTION_PARAM = "jaxrs.static.subresources";
    private static final String SERVICE_SCOPE_SINGLETON = "singleton";
    private static final String SERVICE_SCOPE_REQUEST = "prototype";

    private static final String PARAMETER_SPLIT_CHAR = "class.parameter.split.char";
    private static final String DEFAULT_PARAMETER_SPLIT_CHAR = ",";
    private static final String SPACE_PARAMETER_SPLIT_CHAR = "space";

    private static final String JAXRS_APPLICATION_PARAM = "javax.ws.rs.Application";

    private ClassLoader classLoader;
    private Application application;

    public CXFNonSpringJaxrsServlet() {

    }

    public CXFNonSpringJaxrsServlet(Application app) {
        this.application = app;
    }

    public CXFNonSpringJaxrsServlet(Object singletonService) {
        this(Collections.singleton(singletonService));
    }
    public CXFNonSpringJaxrsServlet(Set<Object> applicationSingletons) {
        this(new ApplicationImpl(applicationSingletons));
    }

    public CXFNonSpringJaxrsServlet(Application app, DestinationRegistry destinationRegistry, Bus bus) {
        super(destinationRegistry, bus);
        this.application = app;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        if (getApplication() != null) {
            createServerFromApplication(servletConfig);
            return;
        }

        String applicationClass = servletConfig.getInitParameter(JAXRS_APPLICATION_PARAM);
        if (applicationClass != null) {
            createServerFromApplication(applicationClass, servletConfig);
            return;
        }

        String splitChar = getParameterSplitChar(servletConfig);
        JAXRSServerFactoryBean bean = new JAXRSServerFactoryBean();
        bean.setBus(getBus());

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
        setDocLocation(bean, servletConfig);
        setSchemasLocations(bean, servletConfig);
        setAllInterceptors(bean, servletConfig, splitChar);
        setInvoker(bean, servletConfig);

        Map<Class<?>, Map<String, List<String>>> resourceClasses =
            getServiceClasses(servletConfig, modelRef != null, splitChar);
        Map<Class<?>, ResourceProvider> resourceProviders =
            getResourceProviders(servletConfig, resourceClasses);

        List<?> providers = getProviders(servletConfig, splitChar);

        bean.setResourceClasses(new ArrayList<Class<?>>(resourceClasses.keySet()));
        bean.setProviders(providers);
        for (Map.Entry<Class<?>, ResourceProvider> entry : resourceProviders.entrySet()) {
            bean.setResourceProvider(entry.getKey(), entry.getValue());
        }
        setExtensions(bean, servletConfig);

        List<? extends Feature> features = getFeatures(servletConfig, splitChar);
        bean.getFeatures().addAll(features);

        bean.create();
    }

    protected String getParameterSplitChar(ServletConfig servletConfig) {
        String param = servletConfig.getInitParameter(PARAMETER_SPLIT_CHAR);
        if (!StringUtils.isEmpty(param) && SPACE_PARAMETER_SPLIT_CHAR.equals(param.trim())) {
            return " ";
        }
        return DEFAULT_PARAMETER_SPLIT_CHAR;
    }
    protected boolean getStaticSubResolutionValue(ServletConfig servletConfig) {
        String param = servletConfig.getInitParameter(STATIC_SUB_RESOLUTION_PARAM);
        if (param != null) {
            return Boolean.valueOf(param.trim());
        }
        return false;
    }

    protected void setExtensions(JAXRSServerFactoryBean bean, ServletConfig servletConfig) {
        bean.setExtensionMappings(
             CastUtils.cast((Map<?, ?>)parseMapSequence(servletConfig.getInitParameter(EXTENSIONS_PARAM))));
        bean.setLanguageMappings(
             CastUtils.cast((Map<?, ?>)parseMapSequence(servletConfig.getInitParameter(LANGUAGES_PARAM))));
        Map<String, Object> properties = CastUtils.cast(
                parseMapSequence(servletConfig.getInitParameter(PROPERTIES_PARAM)),
                String.class, Object.class);
        if (properties != null) {
            bean.getProperties(true).putAll(properties);
        }
    }

    protected void setAllInterceptors(JAXRSServerFactoryBean bean, ServletConfig servletConfig,
                                      String splitChar)
        throws ServletException {
        setInterceptors(bean, servletConfig, OUT_INTERCEPTORS_PARAM, splitChar);
        setInterceptors(bean, servletConfig, OUT_FAULT_INTERCEPTORS_PARAM, splitChar);
        setInterceptors(bean, servletConfig, IN_INTERCEPTORS_PARAM, splitChar);
    }

    protected void setSchemasLocations(JAXRSServerFactoryBean bean, ServletConfig servletConfig) {
        String schemas = servletConfig.getInitParameter(SCHEMAS_PARAM);
        if (schemas == null) {
            return;
        }
        String[] locations = schemas.split(" ");
        List<String> list = new ArrayList<>();
        for (String loc : locations) {
            String theLoc = loc.trim();
            if (!theLoc.isEmpty()) {
                list.add(theLoc);
            }
        }
        if (!list.isEmpty()) {
            bean.setSchemaLocations(list);
        }
    }

    protected void setDocLocation(JAXRSServerFactoryBean bean, ServletConfig servletConfig) {
        String wadlLoc = servletConfig.getInitParameter(DOC_LOCATION_PARAM);
        if (wadlLoc != null) {
            bean.setDocLocation(wadlLoc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void setInterceptors(JAXRSServerFactoryBean bean, ServletConfig servletConfig,
                                   String paramName,
                                   String splitChar) throws ServletException {
        String value = servletConfig.getInitParameter(paramName);
        if (value == null) {
            return;
        }
        String[] values = value.split(splitChar);
        List<Interceptor<? extends Message>> list = new ArrayList<>();
        for (String interceptorVal : values) {
            Map<String, List<String>> props = new HashMap<>();
            String theValue = getClassNameAndProperties(interceptorVal, props);
            if (!theValue.isEmpty()) {
                try {
                    Class<?> intClass = loadClass(theValue, "Interceptor");
                    Object object = intClass.newInstance();
                    injectProperties(object, props);
                    list.add((Interceptor<? extends Message>)object);
                } catch (ServletException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOG.warning("Interceptor class " + theValue + " can not be created");
                    throw new ServletException(ex);
                }
            }
        }
        if (!list.isEmpty()) {
            if (OUT_INTERCEPTORS_PARAM.equals(paramName)) {
                bean.setOutInterceptors(list);
            } else if (OUT_FAULT_INTERCEPTORS_PARAM.equals(paramName)) {
                bean.setOutFaultInterceptors(list);
            } else {
                bean.setInInterceptors(list);
            }
        }
    }

    protected void setInvoker(JAXRSServerFactoryBean bean, ServletConfig servletConfig)
        throws ServletException {
        String value = servletConfig.getInitParameter(INVOKER_PARAM);
        if (value == null) {
            return;
        }
        Map<String, List<String>> props = new HashMap<>();
        String theValue = getClassNameAndProperties(value, props);
        if (!theValue.isEmpty()) {
            try {
                Class<?> intClass = loadClass(theValue, "Invoker");
                Object object = intClass.newInstance();
                injectProperties(object, props);
                bean.setInvoker((Invoker)object);
            } catch (ServletException ex) {
                throw ex;
            } catch (Exception ex) {
                LOG.warning("Invoker class " + theValue + " can not be created");
                throw new ServletException(ex);
            }
        }


    }

    protected Map<Class<?>, Map<String, List<String>>> getServiceClasses(ServletConfig servletConfig,
                                            boolean modelAvailable,
                                            String splitChar) throws ServletException {
        String serviceBeans = servletConfig.getInitParameter(SERVICE_CLASSES_PARAM);
        if (serviceBeans == null) {
            if (modelAvailable) {
                return Collections.emptyMap();
            }
            throw new ServletException("At least one resource class should be specified");
        }
        String[] classNames = serviceBeans.split(splitChar);
        Map<Class<?>, Map<String, List<String>>> map = new HashMap<>();
        for (String cName : classNames) {
            Map<String, List<String>> props = new HashMap<>();
            String theName = getClassNameAndProperties(cName, props);
            if (!theName.isEmpty()) {
                Class<?> cls = loadClass(theName);
                map.put(cls, props);
            }
        }
        if (map.isEmpty()) {
            throw new ServletException("At least one resource class should be specified");
        }
        return map;
    }

    protected List<? extends Feature> getFeatures(ServletConfig servletConfig, String splitChar)
        throws ServletException {

        String featuresList = servletConfig.getInitParameter(FEATURES_PARAM);
        if (featuresList == null) {
            return Collections.< Feature >emptyList();
        }
        String[] classNames = featuresList.split(splitChar);
        List< Feature > features = new ArrayList<>();
        for (String cName : classNames) {
            Map<String, List<String>> props = new HashMap<>();
            String theName = getClassNameAndProperties(cName, props);
            if (!theName.isEmpty()) {
                Class<?> cls = loadClass(theName);
                if (Feature.class.isAssignableFrom(cls)) {
                    features.add((Feature)createSingletonInstance(cls, props, servletConfig));
                }
            }
        }
        return features;
    }

    protected List<?> getProviders(ServletConfig servletConfig, String splitChar) throws ServletException {
        String providersList = servletConfig.getInitParameter(PROVIDERS_PARAM);
        if (providersList == null) {
            return Collections.emptyList();
        }
        String[] classNames = providersList.split(splitChar);
        List<Object> providers = new ArrayList<>();
        for (String cName : classNames) {
            Map<String, List<String>> props = new HashMap<>();
            String theName = getClassNameAndProperties(cName, props);
            if (!theName.isEmpty()) {
                Class<?> cls = loadClass(theName);
                providers.add(createSingletonInstance(cls, props, servletConfig));
            }
        }
        return providers;
    }

    private String getClassNameAndProperties(String cName, Map<String, List<String>> props) {
        String theName = cName.trim();
        int ind = theName.indexOf('(');
        if (ind != -1 && theName.endsWith(")")) {
            props.putAll(parseMapListSequence(theName.substring(ind + 1, theName.length() - 1)));
            theName = theName.substring(0, ind).trim();
        }
        return theName;
    }

    protected static Map<String, List<String>> parseMapListSequence(String sequence) {
        if (sequence != null) {
            sequence = sequence.trim();
            Map<String, List<String>> map = new HashMap<>();
            String[] pairs = sequence.split(" ");
            for (String pair : pairs) {
                String thePair = pair.trim();
                if (thePair.length() == 0) {
                    continue;
                }
                String[] values = thePair.split("=");
                String key;
                String value;
                if (values.length == 2) {
                    key = values[0].trim();
                    value = values[1].trim();
                } else {
                    key = thePair;
                    value = "";
                }
                List<String> list = map.get(key);
                if (list == null) {
                    list = new LinkedList<>();
                    map.put(key, list);
                }
                list.add(value);
            }
            return map;
        }
        return Collections.emptyMap();
    }

    protected Map<Class<?>, ResourceProvider> getResourceProviders(ServletConfig servletConfig,
            Map<Class<?>, Map<String, List<String>>> resourceClasses) throws ServletException {
        String scope = servletConfig.getInitParameter(SERVICE_SCOPE_PARAM);
        if (scope != null && !SERVICE_SCOPE_SINGLETON.equals(scope)
            && !SERVICE_SCOPE_REQUEST.equals(scope)) {
            throw new ServletException("Only singleton and prototype scopes are supported");
        }
        boolean isPrototype = SERVICE_SCOPE_REQUEST.equals(scope);
        Map<Class<?>, ResourceProvider> map = new HashMap<>();
        for (Map.Entry<Class<?>, Map<String, List<String>>> entry : resourceClasses.entrySet()) {
            Class<?> c = entry.getKey();
            map.put(c, isPrototype ? new PerRequestResourceProvider(c)
                                   : new SingletonResourceProvider(
                                         createSingletonInstance(c, entry.getValue(), servletConfig),
                                         true));
        }
        return map;
    }

    protected boolean isAppResourceLifecycleASingleton(Application app, ServletConfig servletConfig) {
        String scope = servletConfig.getInitParameter(SERVICE_SCOPE_PARAM);
        if (scope == null) {
            scope = (String)app.getProperties().get(SERVICE_SCOPE_PARAM);
        }
        return SERVICE_SCOPE_SINGLETON.equals(scope);
    }

    protected Object createSingletonInstance(Class<?> cls, Map<String, List<String>> props, ServletConfig sc)
        throws ServletException {
        Constructor<?> c = ResourceUtils.findResourceConstructor(cls, false);
        if (c == null) {
            throw new ServletException("No valid constructor found for " + cls.getName());
        }
        boolean isApplication = Application.class.isAssignableFrom(c.getDeclaringClass());
        try {
            final ProviderInfo<? extends Object> provider;
            if (c.getParameterTypes().length == 0) {
                if (isApplication) {
                    provider = new ApplicationInfo((Application)c.newInstance(), getBus());
                } else {
                    provider = new ProviderInfo<>(c.newInstance(), getBus(), false, true);
                }
            } else {
                Map<Class<?>, Object> values = new HashMap<>();
                values.put(ServletContext.class, sc.getServletContext());
                values.put(ServletConfig.class, sc);
                provider = ProviderFactory.createProviderFromConstructor(c, values, getBus(), isApplication, true);
            }
            Object instance = provider.getProvider();
            injectProperties(instance, props);
            configureSingleton(instance);
            return isApplication ? provider : instance;
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

    private void injectProperties(Object instance, Map<String, List<String>> props) {
        if (props == null || props.isEmpty()) {
            return;
        }
        Method[] methods = instance.getClass().getMethods();
        Map<String, Method> methodsMap = new HashMap<>();
        for (Method m : methods) {
            methodsMap.put(m.getName(), m);
        }
        for (Map.Entry<String, List<String>> entry : props.entrySet()) {
            Method m = methodsMap.get("set" + StringUtils.capitalize(entry.getKey()));
            if (m != null) {
                Class<?> type = m.getParameterTypes()[0];
                Object value;
                if (InjectionUtils.isPrimitive(type)) {
                    value = PrimitiveUtils.read(entry.getValue().get(0), type);
                } else {
                    value = entry.getValue();
                }
                InjectionUtils.injectThroughMethod(instance, m, value);
            }
        }
    }

    protected void configureSingleton(Object instance) {

    }

    protected void createServerFromApplication(String applicationNames, ServletConfig servletConfig)
        throws ServletException {

        boolean ignoreApplicationPath = isIgnoreApplicationPath(servletConfig);

        String[] classNames = applicationNames.split(getParameterSplitChar(servletConfig));

        if (classNames.length > 1 && ignoreApplicationPath) {
            throw new ServletException("\"" + IGNORE_APP_PATH_PARAM
                + "\" parameter must be set to false for multiple Applications be supported");
        }

        for (String cName : classNames) {
            ApplicationInfo providerApp = createApplicationInfo(cName, servletConfig);

            Application app = providerApp.getProvider();
            JAXRSServerFactoryBean bean = ResourceUtils.createApplication(
                                                app,
                                                ignoreApplicationPath,
                                                getStaticSubResolutionValue(servletConfig),
                                                isAppResourceLifecycleASingleton(app, servletConfig),
                                                getBus());
            String splitChar = getParameterSplitChar(servletConfig);
            setAllInterceptors(bean, servletConfig, splitChar);
            setInvoker(bean, servletConfig);
            setExtensions(bean, servletConfig);
            setDocLocation(bean, servletConfig);
            setSchemasLocations(bean, servletConfig);

            List<?> providers = getProviders(servletConfig, splitChar);
            bean.setProviders(providers);
            List<? extends Feature> features = getFeatures(servletConfig, splitChar);
            bean.getFeatures().addAll(features);

            bean.setBus(getBus());
            bean.setApplicationInfo(providerApp);
            bean.create();
        }
    }

    protected boolean isIgnoreApplicationPath(ServletConfig servletConfig) {
        String ignoreParam = servletConfig.getInitParameter(IGNORE_APP_PATH_PARAM);
        return ignoreParam == null || PropertyUtils.isTrue(ignoreParam);
    }

    protected void createServerFromApplication(ServletConfig servletConfig)
        throws ServletException {

        Application app = getApplication();
        JAXRSServerFactoryBean bean = ResourceUtils.createApplication(
                                          app,
                                          isIgnoreApplicationPath(servletConfig),
                                          getStaticSubResolutionValue(servletConfig),
                                          isAppResourceLifecycleASingleton(app, servletConfig),
                                          getBus());
        String splitChar = getParameterSplitChar(servletConfig);
        setAllInterceptors(bean, servletConfig, splitChar);
        setInvoker(bean, servletConfig);
        setExtensions(bean, servletConfig);
        setDocLocation(bean, servletConfig);
        setSchemasLocations(bean, servletConfig);

        List<?> providers = getProviders(servletConfig, splitChar);
        bean.setProviders(providers);
        List<? extends Feature> features = getFeatures(servletConfig, splitChar);
        bean.getFeatures().addAll(features);

        bean.setBus(getBus());
        bean.setApplication(getApplication());
        bean.create();
    }

    protected Application createApplicationInstance(String appClassName, ServletConfig servletConfig)
        throws ServletException {
        return null;
    }
    protected ApplicationInfo createApplicationInfo(String appClassName, ServletConfig servletConfig)
        throws ServletException {

        Application customApp = createApplicationInstance(appClassName, servletConfig);
        if (customApp != null) {
            return new ApplicationInfo(customApp, getBus());
        }
        Map<String, List<String>> props = new HashMap<>();
        appClassName = getClassNameAndProperties(appClassName, props);
        Class<?> appClass = loadApplicationClass(appClassName);
        ApplicationInfo appInfo = (ApplicationInfo)createSingletonInstance(appClass, props, servletConfig);
        Map<String, Object> servletProps = new HashMap<>();
        ServletContext servletContext = servletConfig.getServletContext();
        for (Enumeration<String> names = servletContext.getInitParameterNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            servletProps.put(name, servletContext.getInitParameter(name));
        }
        for (Enumeration<String> names = servletConfig.getInitParameterNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            servletProps.put(name, servletConfig.getInitParameter(name));
        }
        appInfo.setOverridingProps(servletProps);
        return appInfo;
    }

    protected Class<?> loadApplicationClass(String appClassName) throws ServletException {
        return loadClass(appClassName, "Application");
    }

    protected Class<?> loadClass(String cName) throws ServletException {
        return loadClass(cName, "Resource");
    }

    protected Class<?> loadClass(String cName, String classType) throws ServletException {
        try {
            final Class<?> cls;
            if (classLoader == null) {
                cls = ClassLoaderUtils.loadClass(cName, CXFNonSpringJaxrsServlet.class);
            } else {
                cls = classLoader.loadClass(cName);
            }
            return cls;
        } catch (ClassNotFoundException ex) {
            throw new ServletException("No " + classType + " class " + cName.trim() + " can be found", ex);
        }
    }

    public void setClassLoader(ClassLoader loader) {
        this.classLoader = loader;
    }

    protected Application getApplication() {
        return application;
    }

    private static class ApplicationImpl extends Application {
        private Set<Object> applicationSingletons;
        ApplicationImpl(Set<Object> applicationSingletons) {
            this.applicationSingletons = applicationSingletons;
        }
        public Set<Object> getSingletons() {
            return applicationSingletons;
        }
    }
}

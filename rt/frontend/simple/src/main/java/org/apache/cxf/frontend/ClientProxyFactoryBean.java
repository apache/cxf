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
package org.apache.cxf.frontend;

import java.io.Closeable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

/**
 * This class will create a client for you which implements the specified
 * service class. Example:
 * <pre>
 * ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
 * factory.setServiceClass(YourServiceInterface.class);
 * YourServiceInterface client = (YourServiceInterface) factory.create();
 * </pre>
 * To access the underlying Client object:
 * <pre>
 * Client cxfClient = ClientProxy.getClient(client);
 * </pre>
 */
@NoJSR250Annotations
public class ClientProxyFactoryBean extends AbstractBasicInterceptorProvider {
    protected boolean configured;
    private ClientFactoryBean clientFactoryBean;
    private String username;
    private String password;
    private Map<String, Object> properties;
    private Bus bus;
    private List<Feature> features = new ArrayList<>();
    private DataBinding dataBinding;

    public ClientProxyFactoryBean() {
        this(new ClientFactoryBean());
    }
    public ClientProxyFactoryBean(ClientFactoryBean fact) {
        super();
        this.clientFactoryBean = fact;
    }

    public void initFeatures() {
        this.clientFactoryBean.setFeatures(features);
        this.getServiceFactory().setFeatures(features);
    }

    /**
     * Create a proxy object that implements a specified Service Endpoint Interface. This
     * method is a combination of {@link #setServiceClass(Class)} and {@link #create()}.
     * @param <ProxyServiceType> The type for the SEI.
     * @param serviceClass The Java class object representing the interface you want.
     * @return the proxy.
     */
    public <ProxyServiceType> ProxyServiceType create(Class<ProxyServiceType> serviceClass) {
        setServiceClass(serviceClass);
        return serviceClass.cast(create());
    }
    private void configureObject() {
        if (configured) {
            return;
        }
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        Configurer configurer = bus.getExtension(Configurer.class);
        String name = getConfiguredName();
        if (null != configurer && name != null) {
            configurer.configureBean(name, this);
        }
        configured = true;
    }

    protected String getConfiguredName() {
        QName name = getEndpointName();
        if (name == null) {
            return null;
        }
        return name.toString() + ".client.proxyFactory";
    }

    /**
     * Creates a proxy object that can be used to make remote invocations.
     *
     * @return the proxy. You must cast the returned object to the appropriate class before using it.
     */
    public synchronized Object create() {
        ClassLoaderHolder orig = null;
        ClassLoader loader = null;
        try {
            if (getBus() != null) {
                loader = getBus().getExtension(ClassLoader.class);
                if (loader != null) {
                    orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                }
            }
            configureObject();

            if (properties == null) {
                properties = new HashMap<>();
            }

            if (username != null) {
                AuthorizationPolicy authPolicy = new AuthorizationPolicy();
                authPolicy.setUserName(username);
                authPolicy.setPassword(password);
                properties.put(AuthorizationPolicy.class.getName(), authPolicy);
            }

            initFeatures();
            clientFactoryBean.setProperties(properties);

            if (bus != null) {
                clientFactoryBean.setBus(bus);
            }

            if (dataBinding != null) {
                clientFactoryBean.setDataBinding(dataBinding);
            }

            Client c = clientFactoryBean.create();
            if (getInInterceptors() != null) {
                c.getInInterceptors().addAll(getInInterceptors());
            }
            if (getOutInterceptors() != null) {
                c.getOutInterceptors().addAll(getOutInterceptors());
            }
            if (getInFaultInterceptors() != null) {
                c.getInFaultInterceptors().addAll(getInFaultInterceptors());
            }
            if (getOutFaultInterceptors() != null) {
                c.getOutFaultInterceptors().addAll(getOutFaultInterceptors());
            }

            ClientProxy handler = clientClientProxy(c);

            Class<?>[] classes = getImplementingClasses();
            Object obj = ProxyHelper.getProxy(getClassLoader(clientFactoryBean.getServiceClass()),
                                              classes,
                                              handler);

            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.PROXY_CREATED,
                                               classes, handler, obj);
            return obj;
        } finally {
            if (orig != null) {
                orig.reset();
            }
        }
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }

    protected Class<?>[] getImplementingClasses() {
        Class<?> cls = clientFactoryBean.getServiceClass();
        return new Class[] {Closeable.class, Client.class, cls};
    }

    protected ClientProxy clientClientProxy(Client c) {
        return new ClientProxy(c);
    }

    public ClientFactoryBean getClientFactoryBean() {
        return clientFactoryBean;
    }

    public void setClientFactoryBean(ClientFactoryBean clientFactoryBean) {
        this.clientFactoryBean = clientFactoryBean;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Class<?> getServiceClass() {
        return clientFactoryBean.getServiceClass();
    }

    /**
     * Specifies the class representing the SEI the proxy implements.
     *
     * @param serviceClass the SEI's class
     */
    public void setServiceClass(Class<?> serviceClass) {
        clientFactoryBean.setServiceClass(serviceClass);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWsdlLocation() {
        return getWsdlURL();
    }

    /**
     * Specifies the URL where the proxy can find the WSDL defining the
     * service the proxy implements.
     *
     * @param wsdlURL a string containing the WSDL's URL
     */
    public void setWsdlLocation(String wsdlURL) {
        setWsdlURL(wsdlURL);
    }

    public String getWsdlURL() {
        return clientFactoryBean.getServiceFactory().getWsdlURL();
    }

    /**
     * Specifies the URL where the proxy can find the WSDL defining the
     * service the proxy implements.
     *
     * @param wsdlURL a string containing the WSDL's URL
     */
    public void setWsdlURL(String wsdlURL) {
        clientFactoryBean.getServiceFactory().setWsdlURL(wsdlURL);
    }

    public QName getEndpointName() {
        QName qn = clientFactoryBean.getEndpointName();
        if (qn == null) {
            qn = clientFactoryBean.getServiceFactory().getEndpointName(false);
        }
        return qn;
    }

    public void setEndpointName(QName endpointName) {
        clientFactoryBean.setEndpointName(endpointName);
    }

    /**
     * Returns the QName of the WSDL service the proxy implements
     *
     * @return the WSDL service's QName
     */
    public QName getServiceName() {
        return getServiceFactory().getServiceQName();
    }

    /**
     * Specifies the QName of the WSDL service the proxy implements. The
     * service must exist or an error will result.
     *
     * @param serviceName the QName of the service for the proxy
     */
    public void setServiceName(QName serviceName) {
        getServiceFactory().setServiceName(serviceName);
    }

    public String getAddress() {
        return clientFactoryBean.getAddress();
    }

    public void setAddress(String add) {
        clientFactoryBean.setAddress(add);
    }

    public ConduitSelector getConduitSelector() {
        return clientFactoryBean.getConduitSelector();
    }

    public void setConduitSelector(ConduitSelector selector) {
        clientFactoryBean.setConduitSelector(selector);
    }

    public void setBindingId(String bind) {
        clientFactoryBean.setBindingId(bind);
    }

    public String getBindingId() {
        return clientFactoryBean.getBindingId();
    }

    public void setTransportId(String transportId) {
        clientFactoryBean.setTransportId(transportId);
    }

    public String getTransportId() {
        return clientFactoryBean.getTransportId();
    }

    public ReflectionServiceFactoryBean getServiceFactory() {
        return clientFactoryBean.getServiceFactory();
    }

    public void setServiceFactory(ReflectionServiceFactoryBean sf) {
        clientFactoryBean.setServiceFactory(sf);
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
        clientFactoryBean.setBus(bus);
    }

    /**
     * Returns the property map for the proxy factory.
     *
     * @return the property map
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Specifies a set of properties used to configure the proxies
     * provided by the factory. These properties include things like
     * adding a namespace map to the JAXB databinding.
     *
     * @param properties the property map
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<? extends Feature> f) {
        this.features = CastUtils.cast(f);
    }

    public DataBinding getDataBinding() {
        return dataBinding;
    }

    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }

    public void setBindingConfig(BindingConfiguration config) {
        getClientFactoryBean().setBindingConfig(config);
    }

    public BindingConfiguration getBindingConfig() {
        return getClientFactoryBean().getBindingConfig();
    }
}

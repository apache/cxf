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
package org.apache.cxf.jaxrs.client;

import java.net.URI;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.FactoryBeanListener;

public class JAXRSClientFactoryBean extends AbstractJAXRSFactoryBean {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSClientFactoryBean.class);
    
    private String username;
    private String password;
    private boolean inheritHeaders; 
    private MultivaluedMap<String, String> headers;
    private ClientState initialState;
    private boolean threadSafe;
    private long timeToKeepState;
    private Class serviceClass;
    private ClassLoader proxyLoader;
    
    public JAXRSClientFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }
    
    public JAXRSClientFactoryBean(JAXRSServiceFactoryBean serviceFactory) {
        super(serviceFactory);
        serviceFactory.setEnableStaticResolution(true);
        
    }
    
    /**
     * Sets the custom class loader to be used 
     * for creating proxies 
     * @param loader
     */
    public void setClassLoader(ClassLoader loader) {
        proxyLoader = loader;
    }
    
    /**
     * Indicates if a single proxy or WebClient instance can be reused 
     * by multiple threads.
     *   
     * @param threadSafe if true then multiple threads can invoke on
     *        the same proxy or WebClient instance.
     */
    public void setThreadSafe(boolean threadSafe) {
        this.threadSafe = threadSafe;
    }

    /**
     * Sets the time a thread-local client state will be kept.
     * This property is ignored for thread-unsafe clients
     * @param secondsToKeepState
     */
    public void setSecondsToKeepState(long time) {
        this.timeToKeepState = time;
    }

    /**
     * Gets the user name
     * @return the name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username. 
     * Setting the username and password is a simple way to 
     * create a Basic Authentication token.
     * 
     * @param username the user name
     */
    public void setUsername(String username) {        
        this.username = username;
    }
    
    /**
     * Gets the password
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password. 
     * Setting the username and password is a simple way to 
     * create a Basic Authentication token.
     * 
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /**
     * Indicates if the headers set by a current proxy will be inherited
     * when a subresource proxy is created
     * vice versa.
     * 
     * @param ih if set to true then the current headers will be inherited
     */
    public void setInheritHeaders(boolean ih) {
        inheritHeaders = ih;
    }
    
    /**
     * Sets the resource class
     * @param cls the resource class
     */
    public void setResourceClass(Class<?> cls) {
        setServiceClass(cls);
    }
    
    /**
     * Sets the resource class, may be called from a Spring handler 
     * @param cls the resource class
     */
    public void setServiceClass(Class<?> cls) {
        this.serviceClass = cls;
        serviceFactory.setResourceClass(cls);
    }
    
    /**
     * Returns the service class 
     * @param cls the service class
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }
    
    /**
     * Sets the headers new proxy or WebClient instances will be
     * initialized with.
     * 
     * @param map the headers
     */
    public void setHeaders(Map<String, String> map) {
        headers = new MetadataMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] values = entry.getValue().split(",");
            for (String v : values) {
                if (v.length() != 0) {
                    headers.add(entry.getKey(), v);
                }
            }
        }
    }
    
    /**
     * Gets the initial headers
     * @return the headers
     */
    public Map getHeaders() {
        return headers;
    }
    
    /**
     * Creates a WebClient instance
     * @return WebClient instance
     */
    public WebClient createWebClient() {
        
        Service service = new JAXRSServiceImpl(getAddress(), getServiceName());
        getServiceFactory().setService(service);
        
        try {
            Endpoint ep = createEndpoint();
            ClientState actualState = getActualState();
            WebClient client = actualState == null ? new WebClient(getAddress())
                : new WebClient(actualState);
            initClient(client, ep, actualState == null);
    
            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.CLIENT_CREATED, client, ep);
            
            return client;
        } catch (Exception ex) {
            LOG.severe(ex.getClass().getName() + " : " + ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
    }

    
    
    private ClientState getActualState() {
        if (threadSafe) {
            initialState = new ThreadLocalClientState(getAddress(), timeToKeepState);
        }
        if (initialState != null) {
            return headers != null
                ? initialState.newState(URI.create(getAddress()), headers, null) : initialState;
        } else {
            return null;
        }
    }
    
    /**
     * Creates a proxy
     * @param cls the proxy class
     * @param varValues optional list of values which will be used to substitute
     *        template variables specified in the class-level JAX-RS Path annotations
     * @return the proxy
     */
    public <T> T create(Class<T> cls, Object... varValues) {
        return cls.cast(createWithValues(varValues));
    }
    
    /**
     * Create a Client instance. Proxies and WebClients are Clients.
     * @return the client
     */
    public Client create() { 
        return createWithValues();
    }
    
    /**
     * Create a Client instance. Proxies and WebClients are Clients.
     * @param varValues optional list of values which will be used to substitute
     *        template variables specified in the class-level JAX-RS Path annotations
     *        
     * @return the client
     */
    public Client createWithValues(Object... varValues) {
        serviceFactory.setBus(getBus());
        checkResources(false);
        ClassResourceInfo cri = null;
        try {
            Endpoint ep = createEndpoint();
            if (getServiceClass() != null) {
                for (ClassResourceInfo info : serviceFactory.getClassResourceInfo()) {
                    if (info.getServiceClass().isAssignableFrom(getServiceClass())
                        || getServiceClass().isAssignableFrom(info.getServiceClass())) {
                        cri = info;
                        break;
                    }
                }
                if (cri == null) {
                    // can not happen in the reality
                    throw new RuntimeException("Service class " + getServiceClass().getName()
                                               + " is not recognized");
                }
            } else {
                cri = serviceFactory.getClassResourceInfo().get(0);
            }
            
            boolean isRoot = cri.getURITemplate() != null;
            ClientProxyImpl proxyImpl = null;
            ClientState actualState = getActualState();
            if (actualState == null) {
                proxyImpl = 
                    new ClientProxyImpl(URI.create(getAddress()), proxyLoader, cri, isRoot, 
                                        inheritHeaders, varValues);
            } else {
                proxyImpl = 
                    new ClientProxyImpl(actualState, proxyLoader, cri, isRoot, 
                                        inheritHeaders, varValues);
            }
            initClient(proxyImpl, ep, actualState == null);    
            
            Client actualClient = null;
            try {
                ClassLoader theLoader = proxyLoader == null ? cri.getServiceClass().getClassLoader() 
                                                            : proxyLoader;
                actualClient = (Client)ProxyHelper.getProxy(theLoader,
                                        new Class[]{cri.getServiceClass(), 
                                                    Client.class, 
                                                    InvocationHandlerAware.class}, 
                                        proxyImpl);
            } catch (Exception ex) {
                actualClient = (Client)ProxyHelper.getProxy(Thread.currentThread().getContextClassLoader(),
                                                    new Class[]{cri.getServiceClass(), 
                                                                Client.class, 
                                                                InvocationHandlerAware.class}, 
                                     proxyImpl);
            }
            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.CLIENT_CREATED, actualClient, ep);
            return actualClient;
        } catch (IllegalArgumentException ex) {
            String message = ex.getLocalizedMessage();
            if (cri != null) {
                String expected = cri.getServiceClass().getSimpleName();
                if ((expected + " is not an interface").equals(message)) {
                    message += "; make sure CGLIB is on the classpath";
                }
            }
            LOG.severe(ex.getClass().getName() + " : " + message);
            throw ex;
        } catch (Exception ex) {
            LOG.severe(ex.getClass().getName() + " : " + ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
        
        
    }
    
    protected ConduitSelector getConduitSelector(Endpoint ep) {
        ConduitSelector cs = getConduitSelector();
        cs = cs == null ? new UpfrontConduitSelector() : cs;
        cs.setEndpoint(ep);
        return cs;
    }
    
    protected void initClient(AbstractClient client, Endpoint ep, boolean addHeaders) {
        
        if (username != null) {
            AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setUserName(username);
            authPolicy.setPassword(password);
            ep.getEndpointInfo().addExtensor(authPolicy);
        }
        
        client.getConfiguration().setConduitSelector(getConduitSelector(ep));
        client.getConfiguration().setBus(getBus());
        client.getConfiguration().getOutInterceptors().addAll(getOutInterceptors());
        client.getConfiguration().getOutInterceptors().addAll(ep.getOutInterceptors());
        client.getConfiguration().getInInterceptors().addAll(getInInterceptors());
        client.getConfiguration().getInInterceptors().addAll(ep.getInInterceptors());

        applyFeatures(client);
        
        if (headers != null && addHeaders) {
            client.headers(headers);
        }
        
        setupFactory(ep);
    }
    
    protected void applyFeatures(AbstractClient client) {
        if (getFeatures() != null) {
            for (AbstractFeature feature : getFeatures()) {
                feature.initialize(client.getConfiguration(), getBus());
            }
        }
    }

    /**
     * Sets the initial client state, can be a thread-safe state.
     * @param initialState the state
     */
    public void setInitialState(ClientState initialState) {
        this.initialState = initialState;
    }

    
} 

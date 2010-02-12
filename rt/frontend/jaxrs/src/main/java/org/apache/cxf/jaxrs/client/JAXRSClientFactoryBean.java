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

public class JAXRSClientFactoryBean extends AbstractJAXRSFactoryBean {
    
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSClientFactoryBean.class);
    
    private String username;
    private String password;
    private boolean inheritHeaders; 
    private MultivaluedMap<String, String> headers;
    private ClientState initialState;
    private boolean threadSafe; 
    
    public JAXRSClientFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }
    
    public JAXRSClientFactoryBean(JAXRSServiceFactoryBean serviceFactory) {
        super(serviceFactory);
        serviceFactory.setEnableStaticResolution(true);
        
    }
    
    public void setThreadSafe(boolean threadSafe) {
        this.threadSafe = threadSafe;
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {        
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setInheritHeaders(boolean ih) {
        inheritHeaders = ih;
    }
    
    public void setResourceClass(Class cls) {
        setServiceClass(cls);
    }
    
    public void setServiceClass(Class cls) {
        serviceFactory.setResourceClass(cls);
    }
    
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
    
    public Map getHeaders() {
        return headers;
    }
    
    public WebClient createWebClient() {
        
        Service service = new JAXRSServiceImpl(getAddress(), getServiceName());
        getServiceFactory().setService(service);
        
        try {
            Endpoint ep = createEndpoint();
            ClientState actualState = getActualState();
            WebClient client = actualState == null ? new WebClient(getAddress())
                : new WebClient(actualState);
            initClient(client, ep, actualState == null);
            
            return client;
        } catch (Exception ex) {
            LOG.severe(ex.getClass().getName() + " : " + ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
    }
    
    private ClientState getActualState() {
        if (threadSafe) {
            initialState = new ThreadLocalClientState(getAddress());
        }
        if (initialState != null) {
            return headers != null
                ? initialState.newState(URI.create(getAddress()), headers, null) : initialState;
        } else {
            return null;
        }
    }
    
    public <T> T create(Class<T> cls, Object... varValues) {
        return cls.cast(createWithValues(varValues));
    }
    
    public Client create() { 
        return createWithValues();
    }
    
    public Client createWithValues(Object... varValues) {
        checkResources(false);
        ClassResourceInfo cri = null;
        try {
            Endpoint ep = createEndpoint();
            cri = serviceFactory.getClassResourceInfo().get(0);
            boolean isRoot = cri.getURITemplate() != null;
            ClientProxyImpl proxyImpl = null;
            ClientState actualState = getActualState();
            if (actualState == null) {
                proxyImpl = 
                    new ClientProxyImpl(URI.create(getAddress()), cri, isRoot, inheritHeaders, varValues);
            } else {
                proxyImpl = 
                    new ClientProxyImpl(actualState, cri, isRoot, inheritHeaders, varValues);
            }
            initClient(proxyImpl, ep, actualState == null);    
            
            try {
                return (Client)ProxyHelper.getProxy(cri.getServiceClass().getClassLoader(),
                                        new Class[]{cri.getServiceClass(), Client.class, 
                                                    InvocationHandlerAware.class}, 
                                        proxyImpl);
            } catch (Exception ex) {
                return (Client)ProxyHelper.getProxy(Thread.currentThread().getContextClassLoader(),
                                                    new Class[]{cri.getServiceClass(), Client.class, 
                                                                InvocationHandlerAware.class}, 
                                     proxyImpl);
            }
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
        
        applyFeatures(client);
        client.getConfiguration().setConduitSelector(getConduitSelector(ep));
        client.getConfiguration().setBus(getBus());
        client.getConfiguration().getOutInterceptors().addAll(getOutInterceptors());
        client.getConfiguration().getInInterceptors().addAll(getInInterceptors());

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

    public void setInitialState(ClientState initialState) {
        this.initialState = initialState;
    }

    
} 

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
package org.apache.cxf.jaxrs.client.spec;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;

public class ClientImpl implements Client {
    private Configurable<Client> configImpl;
    private TLSConfiguration secConfig;
    private boolean closed;
    private Set<WebClient> baseClients = new HashSet<WebClient>();
    public ClientImpl(Configuration config,
                      TLSConfiguration secConfig) {
        configImpl = new ClientConfigurableImpl<Client>(this, config);
        this.secConfig = secConfig;
    }
    
    @Override
    public void close() {
        if (!closed) {
            for (WebClient wc : baseClients) {
                wc.close();
            }
            baseClients = null;
            closed = true;
        }
        
    }

    @Override
    public Builder invocation(Link link) {
        checkClosed();
        return target(link.getUriBuilder()).request();
    }

    @Override
    public WebTarget target(UriBuilder builder) {
        checkClosed();
        return new WebTargetImpl(builder, getConfiguration());
    }
    
    
    @Override
    public WebTarget target(String address) {
        return target(UriBuilder.fromUri(address));
    }
    
    @Override
    public WebTarget target(Link link) {
        return target(link.getUriBuilder());
    }

    @Override
    public WebTarget target(URI uri) {
        return target(UriBuilder.fromUri(uri));
    }

    
    @Override
    public HostnameVerifier getHostnameVerifier() {
        checkClosed();
        return secConfig.getVerifier();
    }

    @Override
    public SSLContext getSslContext() {
        checkClosed();
        return secConfig.getSslContext();
    }
    
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException();
        }
    }

    @Override
    public Configuration getConfiguration() {
        return configImpl.getConfiguration();
    }

    @Override
    public Client property(String name, Object value) {
        return configImpl.property(name, value);
    }

    @Override
    public Client register(Class<?> cls) {
        return configImpl.register(cls);
    }

    @Override
    public Client register(Object object) {
        return configImpl.register(object);
    }

    @Override
    public Client register(Class<?> cls, int index) {
        return configImpl.register(cls, index);
    }

    @Override
    public Client register(Class<?> cls, Class<?>... contracts) {
        return configImpl.register(cls, contracts);
    }

    @Override
    public Client register(Class<?> cls, Map<Class<?>, Integer> map) {
        return configImpl.register(cls, map);
    }

    @Override
    public Client register(Object object, int index) {
        return configImpl.register(object, index);
    }

    @Override
    public Client register(Object object, Class<?>... contracts) {
        return configImpl.register(object, contracts);
    }

    @Override
    public Client register(Object object, Map<Class<?>, Integer> map) {
        return configImpl.register(object, map);
    }
    
    class WebTargetImpl implements WebTarget {
        private Configurable<WebTarget> configImpl;
        private UriBuilder uriBuilder;
        private WebClient targetClient;
        
        
        public WebTargetImpl(UriBuilder uriBuilder, 
                             Configuration config) {
            this(uriBuilder, config, null);
        }
        
        public WebTargetImpl(UriBuilder uriBuilder, 
                             Configuration config,
                             WebClient targetClient) {
            this.configImpl = new ClientConfigurableImpl<WebTarget>(this, config);
            this.uriBuilder = uriBuilder.clone();
            this.targetClient = targetClient;
        }
        
        @Override
        public Builder request() {
            ClientImpl.this.checkClosed();
            
            initTargetClientIfNeeded(); 
            
            ClientProviderFactory pf = 
                ClientProviderFactory.getInstance(WebClient.getConfig(targetClient).getEndpoint());
            List<Object> providers = new LinkedList<Object>();
            Configuration cfg = configImpl.getConfiguration();
            for (Object p : cfg.getInstances()) {
                Map<Class<?>, Integer> contracts = cfg.getContracts(p.getClass());
                if (contracts == null || contracts.isEmpty()) {
                    providers.add(p);
                } else {
                    providers.add(
                        new FilterProviderInfo<Object>(p, pf.getBus(), null, contracts));
                }
            }
            
            pf.setUserProviders(providers);
            pf.setDynamicConfiguration(getConfiguration());
            WebClient.getConfig(targetClient).getRequestContext().putAll(getConfiguration().getProperties());
            
            // start building the invocation
            return new InvocationBuilderImpl(WebClient.fromClient(targetClient));
        }
        
        private void initTargetClientIfNeeded() {
            URI uri = uriBuilder.build();
            if (targetClient == null) {
                JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
                bean.setAddress(uri.toString());
                targetClient = bean.createWebClient();
                ClientImpl.this.baseClients.add(targetClient);
            } else if (!targetClient.getCurrentURI().equals(uri)) {
                targetClient.to(uri.toString(), false);
            }
        }

        @Override
        public Builder request(String... accept) {
            return request().accept(accept);
        }

        @Override
        public Builder request(MediaType... accept) {
            return request().accept(accept);
        }

        @Override
        public URI getUri() {
            ClientImpl.this.checkClosed();
            return uriBuilder.build();
        }

        @Override
        public UriBuilder getUriBuilder() {
            ClientImpl.this.checkClosed();
            return uriBuilder.clone();
        }

        @Override
        public WebTarget matrixParam(String name, Object... values) {
            return newWebTarget(getUriBuilder().matrixParam(name, values));
        }
        
        @Override
        public WebTarget path(String path) {
            return newWebTarget(getUriBuilder().path(path));
        }

        @Override
        public WebTarget queryParam(String name, Object... values) {
            return newWebTarget(getUriBuilder().queryParam(name, values));
        }

        @Override
        public WebTarget resolveTemplate(String name, Object value) {
            return resolveTemplate(name, value, true);
        }

        @Override
        public WebTarget resolveTemplate(String name, Object value, boolean encodeSlash) {
            return newWebTarget(getUriBuilder().resolveTemplate(name, value, encodeSlash));
        }

        @Override
        public WebTarget resolveTemplateFromEncoded(String name, Object value) {
            return newWebTarget(getUriBuilder().resolveTemplateFromEncoded(name, value));
        }

        @Override
        public WebTarget resolveTemplates(Map<String, Object> templatesMap) {
            return resolveTemplates(templatesMap, true);
        }

        @Override
        public WebTarget resolveTemplates(Map<String, Object> templatesMap, boolean encodeSlash) {
            ClientImpl.this.checkClosed();
            if (templatesMap.isEmpty()) {
                return this;
            }
            return newWebTarget(getUriBuilder().resolveTemplates(templatesMap, encodeSlash));
        }

        @Override
        public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templatesMap) {
            ClientImpl.this.checkClosed();
            if (templatesMap.isEmpty()) {
                return this;
            }
            return newWebTarget(getUriBuilder().resolveTemplatesFromEncoded(templatesMap));
        }
        
        private WebTarget newWebTarget(UriBuilder newBuilder) {
            boolean complete = false;
            if (targetClient != null) {
                try {
                    newBuilder.build();
                    complete = true;
                } catch (UriBuilderException ex) {
                    //the builder still has unresolved vars
                }
            }
            if (!complete) {
                return new WebTargetImpl(newBuilder, getConfiguration());
            }
            WebClient newClient = WebClient.fromClient(targetClient);
            return new WebTargetImpl(newBuilder, getConfiguration(), newClient);
        }
        
        @Override
        public Configuration getConfiguration() {
            return configImpl.getConfiguration();
        }

        @Override
        public WebTarget property(String name, Object value) {
            return configImpl.property(name, value);
        }

        @Override
        public WebTarget register(Class<?> cls) {
            return configImpl.register(cls);
        }

        @Override
        public WebTarget register(Object object) {
            return configImpl.register(object);
        }

        @Override
        public WebTarget register(Class<?> cls, int index) {
            return configImpl.register(cls, index);
        }

        @Override
        public WebTarget register(Class<?> cls, Class<?>... contracts) {
            return configImpl.register(cls, contracts);
        }

        @Override
        public WebTarget register(Class<?> cls, Map<Class<?>, Integer> map) {
            return configImpl.register(cls, map);
        }

        @Override
        public WebTarget register(Object object, int index) {
            return configImpl.register(object, index);
        }

        @Override
        public WebTarget register(Object object, Class<?>... contracts) {
            return configImpl.register(object, contracts);
        }

        @Override
        public WebTarget register(Object object, Map<Class<?>, Integer> map) {
            return configImpl.register(object, map);
        }
    }
}

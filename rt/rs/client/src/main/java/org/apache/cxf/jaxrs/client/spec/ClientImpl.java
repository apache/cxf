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
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;

public class ClientImpl extends ConfigurableImpl<Client> implements Client {
    private TLSConfiguration secConfig;
    private boolean closed;
    private WebClient template;
    public ClientImpl(Configuration config,
                      TLSConfiguration secConfig) {
        super(config);
        this.secConfig = secConfig;
    }
    
    @Override
    public void close() {
        if (!closed) {
            if (template != null) {
                template.close();
                template = null;
            }
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
        initWebClientTemplateIfNeeded();
        return new WebTargetImpl(builder, getConfiguration(), template);
    }

    private void initWebClientTemplateIfNeeded() {
        // This is done to make the creation of individual targets really easy
        if (template == null) {
            JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
            bean.setAddress("http://tempuri");
            // handle Configuration providers
            this.template = bean.createWebClient();
        }
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
    
    class WebTargetImpl extends ConfigurableImpl<WebTarget> implements WebTarget {
        private UriBuilder uriBuilder;
        private WebClient template; 
        
        public WebTargetImpl(UriBuilder uriBuilder, 
                              Configuration config, 
                              WebClient template) {
            super(config);
            this.uriBuilder = uriBuilder.clone();
            this.template = template;
        }
        
        @Override
        public Builder request() {
            checkClosed();
            WebClient wc = WebClient.fromClient(template).to(uriBuilder.build().toString(), false);
            return new InvocationBuilderImpl(wc);
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
            checkClosed();
            return uriBuilder.build();
        }

        @Override
        public UriBuilder getUriBuilder() {
            checkClosed();
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
            checkClosed();
            if (templatesMap.isEmpty()) {
                return this;
            }
            return newWebTarget(getUriBuilder().resolveTemplates(templatesMap, encodeSlash));
        }

        @Override
        public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templatesMap) {
            checkClosed();
            if (templatesMap.isEmpty()) {
                return this;
            }
            return newWebTarget(getUriBuilder().resolveTemplatesFromEncoded(templatesMap));
        }
        
        private WebTarget newWebTarget(UriBuilder newBuilder) {
            return new WebTargetImpl(newBuilder, getConfiguration(), template);
        }
    }
}

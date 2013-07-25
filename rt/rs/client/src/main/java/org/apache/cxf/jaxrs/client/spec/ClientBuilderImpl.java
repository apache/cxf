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

import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;

public class ClientBuilderImpl extends ClientBuilder {

    private Configurable<ClientBuilder> configImpl;
    private TLSConfiguration secConfig = new TLSConfiguration();
    
    public ClientBuilderImpl() {
        configImpl = new ClientConfigurableImpl<ClientBuilder>(this);
    }
    
    @Override
    public Configuration getConfiguration() {
        return configImpl.getConfiguration();
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        return configImpl.property(name, value);
    }

    @Override
    public ClientBuilder register(Class<?> cls) {
        return configImpl.register(cls);
    }

    @Override
    public ClientBuilder register(Object object) {
        return configImpl.register(object);
    }

    @Override
    public ClientBuilder register(Class<?> cls, int index) {
        return configImpl.register(cls, index);
    }

    @Override
    public ClientBuilder register(Class<?> cls, Class<?>... contracts) {
        return configImpl.register(cls, contracts);
    }

    @Override
    public ClientBuilder register(Class<?> cls, Map<Class<?>, Integer> map) {
        return configImpl.register(cls, map);
    }

    @Override
    public ClientBuilder register(Object object, int index) {
        return configImpl.register(object, index);
    }

    @Override
    public ClientBuilder register(Object object, Class<?>... contracts) {
        return configImpl.register(object, contracts);
    }

    @Override
    public ClientBuilder register(Object object, Map<Class<?>, Integer> map) {
        return configImpl.register(object, map);
    }

    @Override
    public Client build() {
        return new ClientImpl(configImpl.getConfiguration(), secConfig);
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        secConfig.setVerifier(verifier);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        secConfig.setSslContext(sslContext);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore store, char[] password) {
        // TODO Auto-generated method stub
        return this;
    }
    
    @Override
    public ClientBuilder trustStore(KeyStore store) {
        secConfig.setTrustStore(store);
        return this;
    }

    @Override
    public ClientBuilder withConfig(Configuration cfg) {
        configImpl = new ClientConfigurableImpl<ClientBuilder>(this, cfg);
        return this;
    }

}

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Configuration;
import org.apache.cxf.jaxrs.client.AbstractClient;

import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_CONNECTION_TIMEOUT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_RECEIVE_TIMEOUT_PROP;

public class ClientBuilderImpl extends ClientBuilder {

    private ClientConfigurableImpl<ClientBuilder> configImpl;
    private TLSConfiguration secConfig = new TLSConfiguration();

    public ClientBuilderImpl() {
        configImpl = new ClientConfigurableImpl<>(this);
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
        return new ClientImpl(configImpl.getConfiguration(), secConfig) {
            @Override
            public void close() {
                super.close();
                configImpl.close();
            }
        };
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        secConfig.getTlsClientParams().setHostnameVerifier(verifier);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        secConfig.getTlsClientParams().setKeyManagers(null);
        secConfig.getTlsClientParams().setTrustManagers(null);
        secConfig.setSslContext(sslContext);
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore store, char[] password) {
        secConfig.setSslContext(null);
        try {
            KeyManagerFactory tmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(store, password);
            secConfig.getTlsClientParams().setKeyManagers(tmf.getKeyManagers());
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore store) {
        secConfig.setSslContext(null);
        try {
            TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            secConfig.getTlsClientParams().setTrustManagers(tmf.getTrustManagers());
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }

        return this;
    }

    @Override
    public ClientBuilder withConfig(Configuration cfg) {
        if (cfg.getRuntimeType() != RuntimeType.CLIENT) {
            throw new IllegalArgumentException();
        }
        configImpl = new ClientConfigurableImpl<>(this, cfg);
        return this;
    }

    @Override
    public ClientBuilder executorService(ExecutorService executorService) {
        return configImpl.property(AbstractClient.EXECUTOR_SERVICE_PROPERTY, executorService);
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        return configImpl.property("scheduledExecutorService", scheduledExecutorService);
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit timeUnit) {
        validateTimeout(timeout);
        return property(HTTP_CONNECTION_TIMEOUT_PROP, timeUnit.toMillis(timeout));
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit timeUnit) {
        validateTimeout(timeout);
        return property(HTTP_RECEIVE_TIMEOUT_PROP, timeUnit.toMillis(timeout));
    }

    private void validateTimeout(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout is not allowed.");
        }
    }
}


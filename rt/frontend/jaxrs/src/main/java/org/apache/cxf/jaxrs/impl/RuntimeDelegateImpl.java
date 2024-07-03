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

package org.apache.cxf.jaxrs.impl;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Configuration.Builder;
import jakarta.ws.rs.SeBootstrap.Configuration.SSLClientAuthentication;
import jakarta.ws.rs.SeBootstrap.Instance;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.bootstrap.ConfigurationBuilderImpl;
import org.apache.cxf.jaxrs.bootstrap.InstanceImpl;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class RuntimeDelegateImpl extends RuntimeDelegate {
    protected Map<Class<?>, HeaderDelegate<?>> headerProviders = new HashMap<>();

    public RuntimeDelegateImpl() {
        headerProviders.put(MediaType.class, new MediaTypeHeaderProvider());
        headerProviders.put(CacheControl.class, new CacheControlHeaderProvider());
        headerProviders.put(EntityTag.class, new EntityTagHeaderProvider());
        headerProviders.put(Cookie.class, new CookieHeaderProvider());
        headerProviders.put(NewCookie.class, new NewCookieHeaderProvider());
        headerProviders.put(Link.class, new LinkHeaderProvider());
        headerProviders.put(Date.class, new DateHeaderProvider());
    }



    public <T> T createInstance(Class<T> type) {
        if (type.isAssignableFrom(ResponseBuilder.class)) {
            return type.cast(new ResponseBuilderImpl());
        }
        if (type.isAssignableFrom(UriBuilder.class)) {
            return type.cast(new UriBuilderImpl());
        }
        if (type.isAssignableFrom(VariantListBuilder.class)) {
            return type.cast(new VariantListBuilderImpl());
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("HeaderDelegate type is null");
        }
        return (HeaderDelegate<T>)headerProviders.get(type);
    }



    @Override
    public ResponseBuilder createResponseBuilder() {
        return new ResponseBuilderImpl();
    }



    @Override
    public UriBuilder createUriBuilder() {
        return new UriBuilderImpl();
    }



    @Override
    public VariantListBuilder createVariantListBuilder() {
        return new VariantListBuilderImpl();
    }



    @Override
    public <T> T createEndpoint(Application app, Class<T> endpointType)
        throws IllegalArgumentException, UnsupportedOperationException {
        if (app == null || (!Server.class.isAssignableFrom(endpointType)
            && !JAXRSServerFactoryBean.class.isAssignableFrom(endpointType))) {
            throw new IllegalArgumentException();
        }
        JAXRSServerFactoryBean bean = ResourceUtils.createApplication(app, false, false, false, null);
        if (JAXRSServerFactoryBean.class.isAssignableFrom(endpointType)) {
            return endpointType.cast(bean);
        }
        bean.setStart(false);
        Server server = bean.create();
        return endpointType.cast(server);
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return new LinkBuilderImpl();
    }

    @Override
    public Builder createConfigurationBuilder() {
        return new ConfigurationBuilderImpl();
    }

    @Override
    public CompletionStage<Instance> bootstrap(Application application, Configuration configuration) {
        final JAXRSServerFactoryBean factory = ResourceUtils.createApplication(application, false, false, false, null);
        final URI address = configuration.baseUriBuilder().path(factory.getAddress()).build();
        factory.setAddress(address.toString());
        factory.setStart(true);
        
        if ("https".equalsIgnoreCase(configuration.protocol())) {
            final TLSServerParameters parameters = new TLSServerParameters();

            final SSLClientAuthentication sslClientAuthentication = configuration.sslClientAuthentication();
            if (sslClientAuthentication != null) {
                final ClientAuthentication clientAuthentication = new ClientAuthentication();

                if (sslClientAuthentication == SSLClientAuthentication.OPTIONAL) {
                    clientAuthentication.setWant(true);
                } else if (sslClientAuthentication == SSLClientAuthentication.MANDATORY) {
                    clientAuthentication.setRequired(true);
                }

                parameters.setClientAuthentication(clientAuthentication);
            }

            final SSLContext sslContext = configuration.sslContext();
            if (sslContext != null) {
                parameters.setSecureSocketProtocol(sslContext.getProtocol());
            }

            // TODO: Support SSL context propagation down to HTTP engine
        }

        return CompletableFuture
            .supplyAsync(() -> factory.create())
            .thenApply(s -> new InstanceImpl(s, configuration));
    }

    @Override
    public CompletionStage<Instance> bootstrap(Class<? extends Application> clazz, Configuration configuration) {
        try {
            final Application application = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Application>() {
                    @Override
                    public Application run() throws Exception {
                        return clazz.getDeclaredConstructor().newInstance();
                    }
                }
            );
            return bootstrap(application, configuration);
        } catch (final Exception ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    @Override
    public EntityPart.Builder createEntityPartBuilder(String partName) throws IllegalArgumentException {
        return new EntityPartBuilderImpl(partName);
    }
}

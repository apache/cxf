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
package org.apache.cxf.microprofile.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.core.Configuration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProviders;

public class CxfTypeSafeClientBuilder implements RestClientBuilder {
    private String baseUri;
    private Map<String, Object> properties;
    private List<Object> jaxrsProviders;

    public CxfTypeSafeClientBuilder() {
        this.properties = new HashMap<>();
        this.jaxrsProviders = new ArrayList<>();
    }

    @Override
    public RestClientBuilder baseUrl(URL url) {
        this.baseUri = Objects.requireNonNull(url).toExternalForm();
        return this;
    }

    @Override
    public <T> T build(Class<T> aClass) {
        RegisterProviders providers = aClass.getAnnotation(RegisterProviders.class);
        List<Object> providerClasses = new ArrayList<>();
        providerClasses.addAll(this.jaxrsProviders);
        if (providers != null) {
            providerClasses.addAll(Arrays.asList(providers.value()));
        }
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(baseUri);
        bean.setServiceClass(aClass);
        bean.setProviders(providerClasses);
        bean.setProperties(properties);
        return bean.create(aClass);
    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public RestClientBuilder property(String s, Object o) {
        this.properties.put(s, o);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> providerClass) {
        this.jaxrsProviders.add(providerClass);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, int i) {
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Class<?>... classes) {
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> aClass, Map<Class<?>, Integer> map) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object o) {
        this.jaxrsProviders.add(o);
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, int i) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, Class<?>... classes) {
        return this;
    }

    @Override
    public RestClientBuilder register(Object o, Map<Class<?>, Integer> map) {
        return this;
    }
}

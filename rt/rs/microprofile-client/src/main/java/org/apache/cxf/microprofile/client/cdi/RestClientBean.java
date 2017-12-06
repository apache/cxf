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
package org.apache.cxf.microprofile.client.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import org.apache.cxf.microprofile.client.CxfTypeSafeClientBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;

public class RestClientBean implements Bean<Object>, PassivationCapable {
    public static final String REST_URL_FORMAT = "%s/mp-rest/url";
    public static final String REST_SCOPE_FORMAT = "%s/mp-rest/scope";
    private final Class<?> clientInterface;
    private final Class<? extends Annotation> scope;
    private final BeanManager beanManager;
    private final Config config;

    RestClientBean(Class<?> clientInterface, BeanManager beanManager) {
        this.clientInterface = clientInterface;
        this.beanManager = beanManager;
        this.config = ConfigProvider.getConfig();
        this.scope = this.readScope();
    }
    @Override
    public String getId() {
        return clientInterface.getName();
    }

    @Override
    public Class<?> getBeanClass() {
        return clientInterface;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Object create(CreationalContext<Object> creationalContext) {
        CxfTypeSafeClientBuilder builder = new CxfTypeSafeClientBuilder();
        String baseUrl = getBaseUrl();
        try {
            return builder.baseUrl(new URL(baseUrl)).build(clientInterface);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl);
        }
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(clientInterface);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.singleton(RestClient.LITERAL);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return clientInterface.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    private String getBaseUrl() {
        String property = String.format(REST_URL_FORMAT, clientInterface.getName());
        return config.getValue(property, String.class);
    }

    private Class<? extends Annotation> readScope() {
        // first check to see if the value is set
        String property = String.format(REST_SCOPE_FORMAT, clientInterface.getName());
        String configuredScope = config.getOptionalValue(property, String.class).orElse(null);
        if (configuredScope != null) {
            try {
                return (Class<? extends Annotation>)Class.forName(configuredScope);
            } catch (Exception e) {
                throw new IllegalArgumentException("The scope " + configuredScope + " is invalid", e);
            }
        }

        List<Annotation> possibleScopes = new ArrayList<>();
        Annotation[] annotations = clientInterface.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (beanManager.isScope(annotation.annotationType())) {
                possibleScopes.add(annotation);
            }
        }
        if (possibleScopes.isEmpty()) {
            return Dependent.class;
        } else if (possibleScopes.size() == 1) {
            return possibleScopes.get(0).annotationType();
        } else {
            throw new IllegalArgumentException("The client interface " + clientInterface
                    + " has multiple scopes defined " + possibleScopes);
        }
    }
}

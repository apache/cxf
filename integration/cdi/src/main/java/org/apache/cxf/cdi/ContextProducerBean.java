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
package org.apache.cxf.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.PassivationCapable;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class ContextProducerBean extends AbstractCXFBean<Object> implements PassivationCapable {
    private final Type type;
    private final Set<Annotation> qualifiers;

    ContextProducerBean(Type type, boolean defaultQualifier) {
        this.type = type;
        this.qualifiers = new HashSet<>(defaultQualifier ? 2 : 1);
        this.qualifiers.add(ContextResolved.LITERAL);
        if (defaultQualifier) {
            this.qualifiers.add(DEFAULT);
        }
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @Override
    public Class<?> getBeanClass() {
        return (Class<?>)type;
    }

    @Override
    public Object create(CreationalContext<Object> creationalContext) {
        return createContextValue();
    }

    @Override
    public Set<Type> getTypes() {
        Set<Type> types = super.getTypes();
        types.add(type);
        return types;
    }

    @Override
    public String getName() {
        return "CxfContextProducer" + type;
    }

    private Object createContextValue() {
        Message currentMessage = PhaseInterceptorChain.getCurrentMessage();
        Type genericType = null;
        Class<?> contextType;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            genericType = parameterizedType.getActualTypeArguments()[0];
            contextType = (Class<?>)parameterizedType.getRawType();
        } else {
            contextType = (Class<?>)type;
        }
        return JAXRSUtils.createContextValue(currentMessage, genericType, contextType);
    }

    @Override
    public String getId() {
        return getName();
    }
}

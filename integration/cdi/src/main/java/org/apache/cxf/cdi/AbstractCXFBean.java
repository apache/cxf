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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.AnnotationLiteral;

abstract class AbstractCXFBean<T> implements Bean<T> {
    static final Any ANY = new AnyLiteral();
    static final Default DEFAULT = new DefaultLiteral();
    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(ANY);
        qualifiers.add(DEFAULT);
        return qualifiers;
    }

    @Override
    public Set<Type> getTypes() {
        final Set< Type > types = new HashSet<>();
        types.add(Object.class);
        return types;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }


    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        creationalContext.release();
    }

    @Override
    public Set< Class< ? extends Annotation > > getStereotypes() {
        return Collections.emptySet();
    }

    private static final class DefaultLiteral extends AnnotationLiteral<Default> implements Default {
        private static final long serialVersionUID = 1L;
    }

    private static final class AnyLiteral extends AnnotationLiteral<Any> implements Any {
        private static final long serialVersionUID = 1L;
    }
}

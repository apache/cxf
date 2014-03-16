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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;

public final class JAXRSCdiServerFactoryBean implements Bean< JAXRSServerFactoryBean > {
    private static final String FACTORY_BEAN = "factoryBean";
    
    private final Bean< ? > applicationBean;
    private final InjectionTarget<JAXRSServerFactoryBean> injectionTarget;
    
    JAXRSCdiServerFactoryBean(final Bean< ? > applicationBean,
            final InjectionTarget<JAXRSServerFactoryBean> injectionTarget) {
        this.applicationBean = applicationBean;
        this.injectionTarget = injectionTarget;
    }
    
    @Override
    public Class< ? > getBeanClass() {
        return JAXRSServerFactoryBean.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionTarget.getInjectionPoints();
    }

    @Override
    public String getName() {
        return FACTORY_BEAN + applicationBean.getName();
    }

    @SuppressWarnings("serial")
    @Override
    public Set< Annotation > getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral< Default >() { });
        qualifiers.add(new AnnotationLiteral< Any >() { });
        return qualifiers;
    }

    @Override
    public Set<Type> getTypes() {
        final Set< Type > types = new HashSet< Type >();
        types.add(JAXRSServerFactoryBean.class);
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
    public boolean isNullable() {
        return false;
    }
    
    @Override
    public Set< Class< ? extends Annotation > > getStereotypes() {
        return Collections.< Class< ? extends Annotation > >emptySet();
    }

    @Override
    public JAXRSServerFactoryBean create(final CreationalContext< JAXRSServerFactoryBean > ctx) {
        final JAXRSServerFactoryBean instance = injectionTarget.produce(ctx);
        injectionTarget.inject(instance, ctx);
        injectionTarget.postConstruct(instance);
        return instance;
    }

    @Override
    public void destroy(final JAXRSServerFactoryBean instance, 
            final CreationalContext< JAXRSServerFactoryBean > ctx) {
        injectionTarget.preDestroy(instance);
        injectionTarget.dispose(instance);
        ctx.release();
    }
}

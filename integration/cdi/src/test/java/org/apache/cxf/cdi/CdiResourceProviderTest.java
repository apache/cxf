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

import java.lang.annotation.Documented;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.MessageImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(MockitoJUnitRunner.class)
public class CdiResourceProviderTest {
    @Mock
    private BeanManager beanManager;

    @Mock
    private Bean<?> bean;

    @Before
    public void setUp() {
        final Class beanClass = Object.class;
        when(bean.getBeanClass()).thenReturn(beanClass);
        when(beanManager.isNormalScope(any())).thenAnswer(invocationOnMock ->
                Class.class.cast(invocationOnMock.getArguments()[0]).isAnnotationPresent(NormalScope.class));
        when(beanManager.getReference(eq(bean), eq(beanClass), any()))
                .thenAnswer(i -> new Object()); // ensure to create one instance per call, this is what we test
    }

    @Test
    public void normalScoped() {
        when(bean.getScope()).thenReturn(Class.class.cast(ApplicationScoped.class));
        assertSingleton();
    }

    @Test
    public void singleton() {
        when(bean.getScope()).thenReturn(Class.class.cast(Singleton.class));
        assertSingleton();
    }

    @Test
    public void dependent() {
        when(bean.getScope()).thenReturn(Class.class.cast(Singleton.class));
        assertSingleton();
    }

    @Test
    public void requestScoped() {
        when(bean.getScope()).thenReturn(Class.class.cast(RequestScoped.class));
        assertSingleton(); // yes, this is a singleton since we look up the proxy
    }

    @Test
    public void perRequest() {
        // not a scope so will be considered as a not singleton bean
        when(bean.getScope()).thenReturn(Class.class.cast(Documented.class));
        assertNotSingleton();
    }

    private void assertNotSingleton() {
        final ResourceProvider provider = new PerRequestResourceProvider(
        () -> new Lifecycle(beanManager, bean), Object.class);
        assertFalse(new JAXRSCdiResourceExtension().isCxfSingleton(beanManager, bean));
        assertFalse(provider.isSingleton());

        final MessageImpl message1 = new MessageImpl();
        final MessageImpl message2 = new MessageImpl();
        final Object instance = provider.getInstance(message1);
        assertNotNull(instance);
        assertNotEquals(provider.getInstance(message1), provider.getInstance(message2));

        // ensure we can close the lifecycle
        final Lifecycle lifecycle1 = message1.get(Lifecycle.class);
        assertNotNull(lifecycle1);
        assertNotNull(message2.get(Lifecycle.class));
    }

    private void assertSingleton() {
        final ResourceProvider provider = new SingletonResourceProvider(new Lifecycle(beanManager, bean), Object.class);
        assertTrue(new JAXRSCdiResourceExtension().isCxfSingleton(beanManager, bean));
        assertTrue(provider.isSingleton());

        final Object instance = provider.getInstance(new MessageImpl());
        assertNotNull(instance);
        assertEquals(instance, provider.getInstance(new MessageImpl()));
    }
}

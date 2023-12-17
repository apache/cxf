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
package org.apache.cxf.systest.jaxrs.cdi.unwrapper;

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ClassUnwrapper;
import org.apache.cxf.systests.cdi.base.BookStorePreMatchingRequestFilter;
import org.apache.cxf.systests.cdi.base.BookStoreRequestFilter;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;


public class ClassUnwrapperTest {
    private static Bus bus;
    private static ContainerLifecycle lifecycle;
    private static ServletContextEvent event;
    
    @BeforeClass
    public static void setUp() {
        event = new ServletContextEvent(mock(ServletContext.class));
        lifecycle = WebBeansContext.currentInstance().getService(ContainerLifecycle.class);
        lifecycle.startApplication(event);
        bus = getBeanReference(Bus.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getBeanReference(Class<T> clazz) {
        final BeanManager beanManager = lifecycle.getBeanManager();
        final Set<Bean<?>> beans = beanManager.getBeans(clazz);
        final Bean<?> bean = beanManager.resolve(beans);
        return (T)beanManager.getReference(bean, clazz, beanManager.createCreationalContext(bean));
    }

    @AfterClass
    public static void tearDown() {
        lifecycle.stopApplication(event);
    }
    
    @Test
    public void testProxyClassIsProperlyUnwrapped() {
        final BookStorePreMatchingRequestFilter filter = getBeanReference(BookStorePreMatchingRequestFilter.class);
        final ClassUnwrapper unwrapper = (ClassUnwrapper)bus.getProperty(ClassUnwrapper.class.getName());
        
        assertThat(unwrapper, notNullValue());
        assertThat(filter.getClass(), not(equalTo(BookStorePreMatchingRequestFilter.class)));
        assertThat(unwrapper.getRealClass(filter), equalTo(BookStorePreMatchingRequestFilter.class));
    }
    
    @Test
    public void testRealClassIsProperlyUnwrapped() {
        final BookStoreRequestFilter filter = getBeanReference(BookStoreRequestFilter.class);
        final ClassUnwrapper unwrapper = (ClassUnwrapper)bus.getProperty(ClassUnwrapper.class.getName());
        
        assertThat(unwrapper, notNullValue());
        assertThat(filter.getClass(), equalTo(BookStoreRequestFilter.class));
        assertThat(unwrapper.getRealClass(filter), equalTo(BookStoreRequestFilter.class));
    }
}

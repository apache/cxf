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

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ClassUnwrapper;
import org.apache.cxf.systests.cdi.base.BookStorePreMatchingRequestFilter;
import org.apache.cxf.systests.cdi.base.BookStoreRequestFilter;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassUnwrapperTest {
    private Bus bus;
    private WeldContainer container;
    
    @Before
    public void setUp() {
        final Weld weld = new Weld();
        container = weld.initialize();
        bus = getBeanReference(Bus.class);
    }

    private<T> T getBeanReference(Class<T> clazz) {
        return container.select(clazz).get();
    }

    @After
    public void tearDown() {
        container.close();
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

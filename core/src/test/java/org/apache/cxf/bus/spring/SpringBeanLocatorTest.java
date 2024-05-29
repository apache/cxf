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

package org.apache.cxf.bus.spring;

import java.util.List;

import org.apache.cxf.Bus;
import org.springframework.context.ApplicationContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SpringBeanLocatorTest {

    private Bus bus;
    private SpringBeanLocator locator;
    private ApplicationContext ctx;

    @Before
    public void setup() {
        bus = new SpringBusFactory().createBus();
        ctx = Mockito.mock(ApplicationContext.class);
    }

    @Test
    public void testGetBeanNamesOfType() {
        String[] beanNamesForType = new String[1];
        beanNamesForType[0] = "BeanName";
        when(ctx.getBeanNamesForType(TestForSpringBeanLocator.class,
                false, false)).thenReturn(beanNamesForType);
        String[] beanDefNames = new String[0];
        when(ctx.getBeanDefinitionNames()).thenReturn(beanDefNames);

        locator = new SpringBeanLocator(ctx, bus);

        List<String> beanNames = locator.getBeanNamesOfType(TestForSpringBeanLocator.class);
        assertEquals(1, beanNames.size());
        assertEquals(beanNamesForType[0], beanNames.get(0));
    }

    @Test
    public void testHasBeanOfName() {
        String[] beanNamesForType = new String[1];
        beanNamesForType[0] = "BeanName";
        when(ctx.getBeanNamesForType(TestForSpringBeanLocator.class,
                false, false)).thenReturn(beanNamesForType);
        String[] beanDefNames = new String[0];
        when(ctx.getBeanDefinitionNames()).thenReturn(beanDefNames);
        when(ctx.containsBean("BeanName")).thenReturn(true);

        locator = new SpringBeanLocator(ctx, bus);
        assertTrue(locator.hasBeanOfName("BeanName"));
        assertFalse(locator.hasBeanOfName("IDoNotExist"));
    }

    static class TestForSpringBeanLocator {

        TestForSpringBeanLocator() {
            //
        }
    }
}

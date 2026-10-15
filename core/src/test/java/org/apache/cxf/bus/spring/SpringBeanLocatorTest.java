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

import java.util.Collections;
import java.util.List;

import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpringBeanLocatorTest {

    @Test
    public void testHasConfiguredPropertyValueInParentContext() {
        GenericApplicationContext parent = new GenericApplicationContext();
        parent.registerBeanDefinition("parentFactory",
            BeanDefinitionBuilder.genericBeanDefinition(TestFactory.class)
                .addPropertyValue("activationNamespaces", Collections.singletonList("urn:test"))
                .getBeanDefinition());
        parent.refresh();

        GenericApplicationContext child = new GenericApplicationContext(parent);
        child.refresh();

        ExtensionManagerBus bus = new ExtensionManagerBus();
        try {
            SpringBeanLocator locator = new SpringBeanLocator(child, bus);
            assertTrue(locator.hasConfiguredPropertyValue("parentFactory",
                                                          "activationNamespaces", "urn:test"));
            assertFalse(locator.hasConfiguredPropertyValue("parentFactory",
                                                           "activationNamespaces", "urn:other"));
        } finally {
            bus.shutdown(true);
            child.close();
            parent.close();
        }
    }

    public static class TestFactory {
        private List<String> activationNamespaces;

        public List<String> getActivationNamespaces() {
            return activationNamespaces;
        }

        public void setActivationNamespaces(List<String> activationNamespaces) {
            this.activationNamespaces = activationNamespaces;
        }
    }
}

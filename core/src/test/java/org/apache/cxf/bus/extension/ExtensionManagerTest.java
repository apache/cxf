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

package org.apache.cxf.bus.extension;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ExtensionManagerTest {

    private static final String EXTENSIONMANAGER_TEST_RESOURECE_NAME = "extensionManagerTest";
    private ExtensionManagerImpl manager;
    private Map<Class<?>, Object> extensions;

    @Before
    public void setUp() {
        ResourceResolver resolver = new SinglePropertyResolver(EXTENSIONMANAGER_TEST_RESOURECE_NAME, this);
        ResourceManager rm = new DefaultResourceManager(resolver);

        extensions = new HashMap<>();
        extensions.put(Integer.class, Integer.valueOf(0));

        manager = new ExtensionManagerImpl("test-extension.xml",
            Thread.currentThread().getContextClassLoader(), extensions, rm, null);
    }

    @Test
    public void testLoadAndRegister() {
        Extension e = new Extension();
        e.setClassname("java.lang.String");
        e.setDeferred(false);
        manager.loadAndRegister(e);


        String interfaceName = "java.lang.Runnable";
        e.setDeferred(false);
        e.setClassname("java.lang.Thread");
        e.setInterfaceName(interfaceName);
        assertNull("Object is registered.", extensions.get(Runnable.class));
        manager.loadAndRegister(e);
        assertNotNull("Object was not registered.", extensions.get(Runnable.class));

        interfaceName = "java.lang.Integer";
        e.setInterfaceName(interfaceName);
        e.setClassname("no.such.Class");
        Object obj = extensions.get(Integer.class);
        assertNotNull("Object is not registered.", obj);
        manager.loadAndRegister(e);
        assertSame("Registered object was replaced.", obj, extensions.get(Integer.class));

    }



}
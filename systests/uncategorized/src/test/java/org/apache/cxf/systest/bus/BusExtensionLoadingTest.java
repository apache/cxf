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

package org.apache.cxf.systest.bus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.transports.http.QueryHandlerRegistry;
import org.apache.cxf.wsdl.WSDLManager;
import org.junit.Assert;
import org.junit.Test;

public class BusExtensionLoadingTest extends Assert {

    /**
     * Tests the ExtensionManagerBus can be built using a given classloader
     * 
     * @throws Exception
     */
    @Test
    public void testBusConstructionWithoutTCCL() throws Exception {
        ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new TestClassLoader());
            BusFactory factory = new CXFBusFactory() {
                public Bus createBus(Map<Class, Object> e, Map<String, Object> properties) {
                    return new ExtensionManagerBus(e, properties, this.getClass().getClassLoader());
                }
            };
            Bus bus = factory.createBus();
            assertNotNullExtensions(bus);
            bus.shutdown(true);
        } finally {
            Thread.currentThread().setContextClassLoader(origClassLoader);
        }
    }

    /**
     * Test for checking the ExtensionManagerBus is built using the TCCL by default
     * 
     * @throws Exception
     */
    @Test
    public void testDefaultBusConstruction() throws Exception {
        BusFactory factory = new CXFBusFactory();
        Bus bus = factory.createBus();
        assertNotNullExtensions(bus);
        bus.shutdown(true);
    }



    private static void assertNotNullExtensions(Bus bus) {
        assertNotNull(bus.getExtension(WSDLManager.class));
        assertNotNull(bus.getExtension(QueryHandlerRegistry.class));
        assertNotNull(bus.getExtension(ServerRegistry.class));
        assertNotNull(bus.getExtension(HeaderManager.class));
    }

    private static class TestClassLoader extends ClassLoader {
        @Override
        public Class<?> loadClass(final String className) throws ClassNotFoundException {
            if (className.contains("cxf")) {
                throw new ClassNotFoundException("TestClassLoader does not load CXF classes: " +  className);
            } else {
                return super.loadClass(className);
            }
        }
        
        @Override
        public URL getResource(final String name) {
            if (name.contains("cxf") || name.contains("bus")) {
                return null;
            }
            return super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            if (name.contains("cxf") || name.contains("bus")) {
                return Collections.enumeration(new ArrayList<URL>());
            }
            return super.getResources(name);
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            if (name.contains("cxf") || name.contains("bus")) {
                return null;
            }
            return super.getResourceAsStream(name);
        }
    }
}

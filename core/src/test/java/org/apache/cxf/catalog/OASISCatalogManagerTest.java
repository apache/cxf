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

package org.apache.cxf.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class OASISCatalogManagerTest {

    private Bus bus;
    private OASISCatalogManager manager;

    @Before
    public void setup() {
        bus = BusFactory.getDefaultBus();
    }

    @Test
    public void testHasCatalogs() {
        manager = new OASISCatalogManager();
        assertFalse(manager.hasCatalogs());
    }

    @Test
    public void testGetBus() {
        manager = new OASISCatalogManager(bus);
        assertEquals(bus, manager.getBus());
        Bus differentBus = BusFactory.newInstance().createBus();
        manager.setBus(differentBus);
        assertEquals(differentBus, manager.getBus());
    }

    @Test
    public void testGetCatalogManager() {
        OASISCatalogManager actual = OASISCatalogManager.getCatalogManager(bus);
        assertNotNull(actual);
        actual = OASISCatalogManager.getCatalogManager(null);
        assertNotNull(actual);

        bus.setExtension(null, OASISCatalogManager.class);
        actual = OASISCatalogManager.getCatalogManager(bus);
        assertNotNull(actual);
    }

    @Test
    public void testResolveSystem() throws IOException {
        OASISCatalogManager testManager = new OASISCatalogManager();
        assertNull(testManager.resolveSystem("sys"));
    }

    @Test
    public void testResolveURI() throws IOException {
        OASISCatalogManager testManager = new OASISCatalogManager();
        assertNull(testManager.resolveURI("uri"));
    }

    @Test
    public void testResolvePublic() throws IOException {
        OASISCatalogManager testManager = new OASISCatalogManager();
        assertNull(testManager.resolvePublic("uri", "parent"));
    }

    @Test
    public void testLoadCatalogs() {
        try {
            manager = new OASISCatalogManager(bus);
            assertFalse(manager.hasCatalogs());
            String workingDir = System.getProperty("user.dir");
            URL url = new URL("file:" + workingDir + "/src/test/resources/catalog/example.xml");
            ClassLoader classLoader = Mockito.mock(ClassLoader.class);
            Enumeration catalogs = Mockito.mock(Enumeration.class);
            when(catalogs.hasMoreElements()).thenReturn(true, false);
            when(catalogs.nextElement()).thenReturn(url);
            when(classLoader.getResources(url.getPath())).thenReturn(catalogs);
            manager.loadCatalogs(classLoader, url.getPath());
            assertTrue(manager.hasCatalogs());
        } catch (IOException ioException) {
            fail();
        }
    }

    @Test
    public void testLoadCatalog() {
        try {
            manager = new OASISCatalogManager(bus);
            String workingDir = System.getProperty("user.dir");
            URL url = new URL("file:" + workingDir + "/src/test/resources/catalog/example.xml");
            manager.loadCatalog(url);
        } catch (IOException ioException) {
            fail();
        }

        try {
            manager = new OASISCatalogManager(bus);
            String workingDir = System.getProperty("user.dir");
            URL url = new URL("file:" + workingDir + "/src/test/resources/catalog/doesNotExist.xml");
            manager.loadCatalog(url);
            fail();
        } catch (IOException ioException) {
            //
        }
    }
}

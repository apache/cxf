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
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.ws.commons.schema.XmlSchemaException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class CatalogXmlSchemaURIResolverTest {

    private Bus bus;
    private CatalogXmlSchemaURIResolver resolver;
    private OASISCatalogManager catalog = Mockito.mock(OASISCatalogManager.class);

    @Before
    public void setup() {
        bus = BusFactory.getDefaultBus();
        bus.setExtension(catalog, OASISCatalogManager.class);
        resolver = new CatalogXmlSchemaURIResolver(bus);
    }

    @Test
    public void testGetResolvedMap() {
        Map<String, String> expected = new HashMap<>();
        assertEquals(expected, resolver.getResolvedMap());
    }

    @Test
    public void testResolveEntity() throws IOException {
        String targetNamespace = "ns1";
        String baseUrl = "file:" + System.getProperty("user.dir") + "/src/test/resources/catalog/";
        String schemaLocation = "example.xml";
        when(catalog.resolveSystem(schemaLocation)).thenReturn("example.xml");
        resolver = new CatalogXmlSchemaURIResolver(bus);
        InputSource is = resolver.resolveEntity(targetNamespace, schemaLocation, baseUrl);
        assertNotNull(is);
        assertFalse(is.isEmpty());
    }

    @Test
    public void testResolveEntityFailedInputStreamConstruction() throws IOException {
        String targetNamespace = "ns1";
        String baseUrl = "file:" + System.getProperty("user.dir") + "/src/test/resources/catalog/";
        String schemaLocation = "doesNotExist.xml";
        when(catalog.resolveSystem(schemaLocation)).thenReturn("doesNotExist.xml");
        resolver = new CatalogXmlSchemaURIResolver(bus);
        try {
            resolver.resolveEntity(targetNamespace, schemaLocation, baseUrl);
            fail();
        } catch (XmlSchemaException xse) {
            String msg = "Unable to locate imported document at 'doesNotExist.xml', relative to '" + baseUrl + "'.";
            assertEquals(msg, xse.getMessage());
        }
    }

    @Test
    public void testResolveEntityEndsInXmlSchemaException() throws IOException {
        String targetNamespace = "ns1";
        String schemaLocation = "/schemas/foo";
        when(catalog.resolveSystem(schemaLocation)).thenReturn("resolvedLocationToCauseException");

        resolver = new CatalogXmlSchemaURIResolver(bus);
        try {
            resolver.resolveEntity(targetNamespace, schemaLocation, null);
            fail();
        } catch (XmlSchemaException xse) {
            assertEquals("Unable to locate imported document at '/schemas/foo'.", xse.getMessage());
        }
    }

}

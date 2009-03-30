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

package org.apache.cxf.tools.wsdlto.frontend.jaxws;

import java.net.URL;

import org.xml.sax.InputSource;

import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.junit.Assert;
import org.junit.Test;

public class CatalogTest extends Assert {
    @Test
    public void testCatalog() throws Exception {
        OASISCatalogManager catalogManager = new OASISCatalogManager();

        URL jaxwscatalog = getClass().getResource("/META-INF/jax-ws-catalog.xml");
        assertNotNull(jaxwscatalog);

        catalogManager.loadCatalog(jaxwscatalog);

        String xsd = "http://www.w3.org/2005/08/addressing/ws-addr.xsd";
        String resolvedSchemaLocation = catalogManager.resolveSystem(xsd);
        assertEquals("classpath:/schemas/wsdl/ws-addr.xsd", resolvedSchemaLocation);

        ExtendedURIResolver resolver = new ExtendedURIResolver();
        InputSource in = resolver.resolve(resolvedSchemaLocation, null);
        assertTrue(in.getSystemId().indexOf("common") != -1);
        assertTrue(in.getSystemId().indexOf("/schemas/wsdl/ws-addr.xsd") != -1);
    }
}
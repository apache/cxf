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
package org.apache.cxf.frontend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.wsdl.extensions.schema.SchemaReference;

import com.ibm.wsdl.extensions.schema.SchemaReferenceImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WSDLGetUtilsTest {

    private static final String BASE = "http://localhost:8080/service";
    private static final String DOCUMENT = "http://localhost/schemas/a.xsd";

    @Test
    public void testImportKeyReused() throws Exception {
        // b.xsd can only be found relative to the document it is imported from, not by its own key
        Map<String, SchemaReference> smp = new ConcurrentHashMap<>();
        smp.put("http://localhost/schemas/b.xsd", new SchemaReferenceImpl());
        smp.put("http://localhost/schemas/c.xsd", new SchemaReferenceImpl());
        WSDLGetUtils utils = new WSDLGetUtils();

        String link = utils.mapUri(null, BASE, smp, "b.xsd", null, DOCUMENT);
        assertEquals(BASE + "?xsd=_import1.xsd", link);

        // serving the same import again uses the same key, rather than adding a new one each time
        for (int i = 0; i < 10; i++) {
            assertEquals(link, utils.mapUri(null, BASE, smp, "b.xsd", null, DOCUMENT));
        }
        assertEquals(3, smp.size());

        // a different schema gets its own key
        assertEquals(BASE + "?xsd=_import2.xsd", utils.mapUri(null, BASE, smp, "c.xsd", null, DOCUMENT));
        assertEquals(BASE + "?xsd=_import2.xsd", utils.mapUri(null, BASE, smp, "c.xsd", null, DOCUMENT));
        assertEquals(4, smp.size());
    }
}

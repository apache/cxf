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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.wsdl.extensions.schema.SchemaReference;

import com.ibm.wsdl.extensions.schema.SchemaReferenceImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testImportKeysConcurrent() throws Exception {
        // schemas served concurrently must never be given the same key
        int schemas = 20;
        Map<String, SchemaReference> smp = new ConcurrentHashMap<>();
        List<SchemaReference> refs = new ArrayList<>();
        for (int i = 0; i < schemas; i++) {
            SchemaReference ref = new SchemaReferenceImpl();
            refs.add(ref);
            smp.put("http://localhost/schemas/s" + i + ".xsd", ref);
        }
        WSDLGetUtils utils = new WSDLGetUtils();

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<List<String>>> results = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                results.add(executor.submit(() -> {
                    start.await();
                    List<String> links = new ArrayList<>();
                    for (int i = 0; i < schemas; i++) {
                        links.add(utils.mapUri(null, BASE, smp, "s" + i + ".xsd", null, DOCUMENT));
                    }
                    return links;
                }));
            }
            start.countDown();

            for (Future<List<String>> result : results) {
                List<String> links = result.get();
                for (int i = 0; i < schemas; i++) {
                    String key = links.get(i).substring((BASE + "?xsd=").length());
                    assertSame(refs.get(i), smp.get(key));
                }
            }
        } finally {
            executor.shutdown();
        }
        // at most a few extra keys from requests that raced to create the first one for a schema
        assertTrue(smp.size() <= schemas + schemas * threads);
    }
}

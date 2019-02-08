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

package org.apache.cxf.systest.jaxrs;

import java.util.Collections;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.aegis.AegisElementProvider;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSDataBindingTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookDataBindingServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookDataBindingServer.class, true));
        createStaticBus();
    }


    @Test
    public void testGetBookJAXB() throws Exception {
        WebClient client = WebClient.create("http://localhost:"
                                            + PORT + "/databinding/jaxb/bookstore/books/123");
        Book book = client.accept("application/xml").get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action", book.getName());
    }

    @Test
    public void testGetBookAegis() throws Exception {
        WebClient client = WebClient.create("http://localhost:"
                                            + PORT + "/databinding/aegis/bookstore/books/123",
                                            Collections.singletonList(new AegisElementProvider<Book>()));
        Book book = client.accept("application/xml").get(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("CXF in Action", book.getName());
    }



}

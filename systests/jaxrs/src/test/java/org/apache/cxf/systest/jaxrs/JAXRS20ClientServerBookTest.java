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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRS20ClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer20.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServer20.class, true));
    }
    
    @Test
    public void testGetBook() {
        String address = "http://localhost:" + PORT + "/bookstore/bookheaders/simple";
        WebClient wc = WebClient.create(address);
        Book book = wc.get(Book.class);
        assertEquals(123L, book.getId());
    }
}

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

package org.apache.cxf.systest.jaxrs.restr;

import java.util.Collections;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class JAXRSClientServerBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;
    public static final String PORT2 = allocatePort(JAXRSClientServerBookTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        // Test that javax.annotation.Priority is not on the classpath
        try {
            Class.forName("javax.annotation.Priority");
            fail("Failure as we are testing that javax.annotation is not available");
        } catch (ClassNotFoundException ex) {
            // expected
        }
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServer.class, true));
        createStaticBus();

    }

    @Test
    public void testGetBookRoot() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore";
        WebClient wc = WebClient.create(address, Collections.singletonList(new JacksonJsonProvider()));
        wc.accept(MediaType.APPLICATION_JSON);
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        assertEquals("root", book.getName());
    }

}
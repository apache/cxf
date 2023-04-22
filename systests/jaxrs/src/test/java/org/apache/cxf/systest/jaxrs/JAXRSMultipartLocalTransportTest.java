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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class JAXRSMultipartLocalTransportTest {
    private static final String ENDPOINT_ADDRESS = "local://test-multipart";
    private static final String TEMPLATE = "{ "
        + "\"id\": %s, "
        + "\"name\": \"Book #%s\", "
        + "\"class\": \"org.apache.cxf.systest.jaxrs.Book\" }";
    private static Server server;
    
    @BeforeClass
    public static void startServers() throws Exception {
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(MultipartStore.class);
        sf.setResourceProvider(MultipartStore.class, new SingletonResourceProvider(new MultipartStore(), true));
        sf.setAddress(ENDPOINT_ADDRESS);
        server = sf.create();
    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        if (server != null) {
            server.stop();
            server.destroy();
        }
    }

    @Test
    public void testBookAsMultipleInputStreams() throws Exception {
        final MultipartStore store = JAXRSClientFactory.create(ENDPOINT_ADDRESS, MultipartStore.class, 
            Collections.singletonList(new JacksonXmlBindJsonProvider()));
        
        final byte[] books1 = generateBooks(40000).getBytes(StandardCharsets.UTF_8); 
        final byte[] books2 = generateBooks(2000).getBytes(StandardCharsets.UTF_8);
        
        final List<Book> books = store.addBookJsonTypeFromStreams(new ByteArrayInputStream(books1), 
            new ByteArrayInputStream(books2));
        
        assertThat(books.size(), equalTo(42000));
    }

    private String generateBooks(int size) {
        return "[" + IntStream
            .range(0, size)
            .mapToObj(id -> String.format(TEMPLATE, id, id))
            .collect(Collectors.joining(",")) + "]";
    }
}

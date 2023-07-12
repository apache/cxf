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
package org.apache.cxf.transport.http_jaxws_spi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.ws.spi.http.HttpExchange;
import jakarta.xml.ws.spi.http.HttpHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;

public class HttpHandlerImplTest {

    private HttpHandler handler;
    private JAXWSHttpSpiDestination destination;
    private HttpExchange exchange;

    @Before
    public void setUp() {
        destination = mock(JAXWSHttpSpiDestination.class);
        handler = new HttpHandlerImpl(destination);
        exchange = mock(HttpExchange.class);
    }

    @After
    public void tearDown() {
        exchange = null;
        handler = null;
        destination = null;
    }

    @Test
    public void testHttpHandlerImpl() throws Exception {
        exchange.close();
        destination.doService(isA(HttpServletRequest.class),
                              isA(HttpServletResponse.class));
        handler.handle(exchange);
    }

}

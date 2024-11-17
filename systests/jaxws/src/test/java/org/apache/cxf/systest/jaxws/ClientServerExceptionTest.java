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

package org.apache.cxf.systest.jaxws;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ClientServerExceptionTest extends AbstractBusClientServerTestBase {
    private ExceptionService port;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(ClientServerExceptionServer.class, true));
    }

    @Before
    public void setUp() throws Exception {
        URL wsdlURL = new URL("http://localhost:" + ClientServerExceptionServer.PORT + "/ExceptionService?wsdl");
        QName qname = new QName("http://cxf.apache.org/", "ExceptionService");
        Service service = Service.create(wsdlURL, qname);
        port = service.getPort(ExceptionService.class);
    }

    @Test
    public void exceptionMessageIsPreserved() throws MalformedURLException {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> port.saySomething("Hello World!"));
        assertEquals("Simulated!", ex.getMessage());
    }

    @Test
    public void exceptionCustomExceptionMessageIsPreserved() throws MalformedURLException {
        final SayException ex = assertThrows(SayException.class,
            () -> port.sayNothing("Hello World!"));
        assertEquals("Simulated!", ex.getMessage());
        assertEquals(100, ex.getCode());
    }
}

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



package org.apache.cxf.systest.soapfault.details;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world_soap12_http.Greeter;
import org.apache.hello_world_soap12_http.PingMeFault;
import org.apache.hello_world_soap12_http.SOAPService;
import org.apache.hello_world_soap12_http.types.FaultDetail;

import org.junit.BeforeClass;
import org.junit.Test;

public class Soap12ClientServerTest extends AbstractBusClientServerTestBase {    
    public static final String PORT = Server12.PORT;

    private final QName serviceName = new QName("http://apache.org/hello_world_soap12_http",
                                                "SOAPService");
    private final QName portName = new QName("http://apache.org/hello_world_soap12_http", "SoapPort");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server12.class));
    }

    @Test
    public void testFaultMessage() throws Exception {
        Greeter greeter = getGreeter();
        try {
            greeter.sayHi();
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("sayHiFault Caused by: Get a wrong name <sayHi>", ex.getMessage());
            StackTraceElement[] element = ex.getCause().getStackTrace();
            assertEquals("org.apache.cxf.systest.soapfault.details.GreeterImpl12", element[0].getClassName());
        }
    }

    @Test
    public void testPingMeFault() throws Exception {
        Greeter greeter = getGreeter();
        try {
            greeter.pingMe();
            fail("Should throw Exception!");
        } catch (PingMeFault ex) {
            FaultDetail detail = ex.getFaultInfo();
            assertEquals((short)2, detail.getMajor());
            assertEquals((short)1, detail.getMinor());
            assertEquals("PingMeFault raised by server", ex.getMessage());
            StackTraceElement[] element = ex.getStackTrace();
            assertEquals("org.apache.cxf.systest.soapfault.details.GreeterImpl12", element[0].getClassName());
        }
    }
    

    private Greeter getGreeter() throws NumberFormatException, MalformedURLException {
        URL wsdl = getClass().getResource("/wsdl/hello_world_soap12.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is ull ", service);
        
        Greeter g = service.getPort(portName, Greeter.class);
        updateAddressPort(g, PORT);
        return g;
    }

}


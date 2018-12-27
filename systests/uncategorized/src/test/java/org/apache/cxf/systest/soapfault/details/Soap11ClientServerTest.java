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

import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.types.FaultDetail;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Soap11ClientServerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server11.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server11.class, true));
    }

    @Test
    public void testFaultMessage() throws Exception {
        Greeter greeter = getGreeter();
        try {
            greeter.sayHi();
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("sayHiFault Caused by: Get a wrong name <sayHi>", ex.getMessage());
            StackTraceElement[] elements = ex.getCause().getStackTrace();
            assertEquals("org.apache.cxf.systest.soapfault.details.GreeterImpl11",
                         elements[0].getClassName());
        }

        // testing Fault(new NullPointerException())
        try {
            greeter.greetMe("Anya");
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals(NullPointerException.class.getName(), ex.getMessage());
        }

        // testing Fault(new IllegalArgumentException("Get a wrong name for greetMe"))
        try {
            greeter.greetMe("Banya");
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("Get a wrong name for greetMe", ex.getMessage());
        }

        // testing Fault("unexpected null", LOG, new NullPointerException())
        try {
            greeter.greetMe("Canya");
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("unexpected null", ex.getMessage());
        }

        // testing Fault("greetMeFault", LOG, new IllegalArgumentException("Get a wrong name greetMe"))
        try {
            greeter.greetMe("Danya");
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("greetMeFault Caused by: Get a wrong name greetMe", ex.getMessage());
        }

        // testing Fault("invalid", LOG)
        try {
            greeter.greetMe("Eanna");
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("invalid", ex.getMessage());
        }
    }


    @Test
    public void testNewLineInExceptionMessage() throws Exception {
        Greeter greeter = getGreeter();

        try {
            greeter.greetMe("newline");
            fail("Should throw Exception!");
        } catch (SOAPFaultException ex) {
            assertEquals("greetMeFault Caused by: Get a wrong name <greetMe>", ex.getMessage());
            StackTraceElement[] elements = ex.getCause().getStackTrace();
            assertEquals("org.apache.cxf.systest.soapfault.details.GreeterImpl11",
                         elements[0].getClassName());
            assertTrue(ex.getCause().getCause().getMessage().endsWith("Test \n cause."));
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
            assertEquals("org.apache.cxf.systest.soapfault.details.GreeterImpl11",
                         element[0].getClassName());
        }
    }


    private Greeter getGreeter() throws NumberFormatException, MalformedURLException {
        GreeterService service = new GreeterService();
        assertNotNull(service);
        Greeter greeter = service.getGreeterPort();
        updateAddressPort(greeter, PORT);
        return greeter;
    }

}

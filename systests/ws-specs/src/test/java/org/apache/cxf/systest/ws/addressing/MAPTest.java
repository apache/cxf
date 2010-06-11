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

package org.apache.cxf.systest.ws.addressing;

import java.lang.reflect.UndeclaredThrowableException;

import javax.jws.WebService;

import org.apache.hello_world_soap_http.BadRecordLitFault;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the addition of WS-Addressing Message Addressing Properties.
 */
public class MAPTest extends MAPTestBase {
    static final String ADDRESS = "http://localhost:" + PORT + "/SoapContext/SoapPort";

    private static final String CONFIG;
    static {
        CONFIG = "org/apache/cxf/systest/ws/addressing/cxf" 
            + (("HP-UX".equals(System.getProperty("os.name"))
                || "Windows XP".equals(System.getProperty("os.name"))) ? "-hpux" : "")
            + ".xml";
    }
    
    public String getConfigFileName() {
        return CONFIG;
    }
    public String getAddress() {
        return ADDRESS;
    }
    

    @BeforeClass
    public static void startServers() throws Exception {
        // special case handling for WS-Addressing system test to avoid
        // UUID related issue when server is run as separate process
        // via maven on Win2k
        boolean inProcess = "Windows 2000".equals(System.getProperty("os.name"));
        assertTrue("server did not launch correctly", 
                   launchServer(Server.class, null,
                                new String[] {ADDRESS, GreeterImpl.class.getName()},  inProcess));
    }
    @WebService(serviceName = "SOAPServiceAddressing", 
                portName = "SoapPort", 
                endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
                targetNamespace = "http://apache.org/hello_world_soap_http",
                wsdlLocation = "testutils/hello_world.wsdl")
    public static class GreeterImpl extends org.apache.cxf.systest.ws.addressing.AbstractGreeterImpl {
        
    }
    
    @Test
    public void testUsingKeepAliveConnection() throws Exception {
        if (!"HP-UX".equals(System.getProperty("os.name"))) {
            return;
        }
        int n = 100;
        for (int i = 0; i < n; i++) {
            greeter.greetMeOneWay("oneway on keep-alive connection");
        }
        for (int i = 0; i < n; i++) {
            assertNotNull(greeter.greetMe("twoway on keep-alive connection"));
        }
        for (int i = 0; i < 0; i++) {
            try {
                greeter.testDocLitFault("BadRecordLitFault");
                fail("expected fault from service");
            } catch (BadRecordLitFault brlf) {
                //checkVerification();
            } catch (UndeclaredThrowableException ex) {
                throw (Exception)ex.getCause();
            }
        }
    }
}


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

package org.apache.cxf.systest.http;

import java.net.URL;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.Test;

/**
 * This test is meant to run against a spring-loaded HTTP/S service. It tests the certificate
 * constraints logic.
 */
public class CertConstraintsTest extends AbstractBusClientServerTestBase {
    public static final String PORT0 = BusServer.PORT0;
    public static final String PORT1 = BusServer.PORT1;
    public static final String PORT2 = BusServer.PORT2;
    public static final String PORT3 = BusServer.PORT3;
    public static final String PORT4 = BusServer.PORT4;
    public static final String PORT5 = BusServer.PORT5;
    public static final String PORT6 = BusServer.PORT6;
    public static final String PORT7 = BusServer.PORT7;
    public static final String PORT8 = BusServer.PORT8;
    //
    // data
    //
    
    /**
     * the package path used to locate resources specific to this test
     */
    private void setTheConfiguration(String config) {
        //System.setProperty("javax.net.debug", "all");
        try {
            System.setProperty(
                Configurer.USER_CFG_FILE_PROPERTY_URL,
                CertConstraintsTest.class.getResource(config).toString()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
          
    public void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork a new process
            launchServer(BusServer.class, true)
        );
    }
    
    
    public void stopServers() throws Exception {
        stopAllServers();
        System.clearProperty(Configurer.USER_CFG_FILE_PROPERTY_URL);
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }    
    
    
    //
    // tests
    //
    public final void testSuccessfulCall(String address) throws Exception {
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        BindingProvider provider = (BindingProvider)port;
        provider.getRequestContext().put(
              BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
              address);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
    }
    
    public final void testFailedCall(String address) throws Exception {
        URL url = SOAPService.WSDL_LOCATION;
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        BindingProvider provider = (BindingProvider)port;
        provider.getRequestContext().put(
                BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                address);

        try {
            assertEquals(port.greetMe("Kitty"), "Hello Kitty");
            fail("Failure expected");
        } catch (javax.xml.ws.soap.SOAPFaultException ex) {
            // expected
        } catch (javax.xml.ws.WebServiceException ex) {
            // expected
        }
    }
    
    @Test
    public final void testCertConstraints() throws Exception {
        setTheConfiguration("resources/jaxws-server-constraints.xml");
        startServers();
        
        //
        // Good Subject DN
        //
        testSuccessfulCall("https://localhost:" + PORT0 + "/SoapContext/HttpsPort");
        //
        // Bad Subject DN
        //
        testFailedCall("https://localhost:" + PORT1 + "/SoapContext/HttpsPort");
        //
        // Mixed Subject DN (ALL)
        //
        testFailedCall("https://localhost:" + PORT2 + "/SoapContext/HttpsPort");
        //
        // Mixed Subject DN (ANY)
        //
        testSuccessfulCall("https://localhost:" + PORT3 + "/SoapContext/HttpsPort");
        //
        // Mixed Issuer DN (ALL)
        //
        testFailedCall("https://localhost:" + PORT4 + "/SoapContext/HttpsPort");
        //
        // Mixed Issuer DN (ANY)
        //
        testSuccessfulCall("https://localhost:" + PORT5 + "/SoapContext/HttpsPort");
        //
        // Bad server Subject DN
        //
        testFailedCall("https://localhost:" + PORT6 + "/SoapContext/HttpsPort");
        //
        // Bad server Issuer DN
        //
        testFailedCall("https://localhost:" + PORT7 + "/SoapContext/HttpsPort");
        
        stopServers();
    }
    
}

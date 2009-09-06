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
 * This test is meant to run against a spring-loaded
 * HTTP/S service.
 */
public class HTTPSClientTest extends AbstractBusClientServerTestBase {
    
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
                HTTPSClientTest.class.getResource(config).toString()
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
    public final void testSuccessfulCall(String configuration,
                                         String address) throws Exception {
        testSuccessfulCall(configuration, address, null);
    }
    public final void testSuccessfulCall(String configuration,
                                         String address,
                                         URL url) throws Exception {
        setTheConfiguration(configuration);
        startServers();
        if (url == null) {
            url = SOAPService.WSDL_LOCATION;
        }
        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);   
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);
        
        BindingProvider provider = (BindingProvider)port;
        provider.getRequestContext().put(
              BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
              address);
        
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        stopServers();
    }
    
    @Test
    public final void testJaxwsServer() throws Exception {
        testSuccessfulCall("resources/jaxws-server.xml", 
                           "https://localhost:9002/SoapContext/HttpsPort");        
    }
    @Test
    public final void testJaxwsServerChangeHttpsToHttp() throws Exception {
        testSuccessfulCall("resources/jaxws-server.xml", 
                            "http://localhost:9003/SoapContext/HttpPort");        
    }    
    @Test
    public final void testJaxwsEndpoint() throws Exception {
        testSuccessfulCall("resources/jaxws-publish.xml",
                           "https://localhost:9001/SoapContext/HttpsPort");
    }
    
    @Test
    public final void testPKCS12Endpoint() throws Exception {
        testSuccessfulCall("resources/pkcs12.xml",
                           "https://localhost:9003/SoapContext/HttpsPort");
    }
    
    @Test
    public final void testResourceKeySpecEndpoint() throws Exception {
        testSuccessfulCall("resources/resource-key-spec.xml",
                           "https://localhost:9004/SoapContext/HttpsPort");
    }
    @Test
    public final void testResourceKeySpecEndpointURL() throws Exception {
        testSuccessfulCall("resources/resource-key-spec-url.xml",
                           "https://localhost:9005/SoapContext/HttpsPort",
                           new URL("https://localhost:9005/SoapContext/HttpsPort?wsdl"));
    }
}

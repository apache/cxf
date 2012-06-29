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
package org.apache.cxf.systest.ws.addr_feature;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.AddressingFeature;

import org.apache.cxf.systest.ws.AbstractWSATestBase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WSAFaultToClientServerTest  extends AbstractWSATestBase {
    private final QName serviceName = new QName("http://apache.org/cxf/systest/ws/addr_feature/",
                                                "AddNumbersService");

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("FaultTo server did not launch correctly", launchServer(FaultToEndpointServer.class, true));
    }

    @Test
    public void testJaxwsWsaFeature() throws Exception {
        ByteArrayOutputStream input = setupInLogging();
        AddNumbersPortType port = getPort();

        EndpointReferenceType faultTo = new EndpointReferenceType();
        AddressingProperties addrProperties = new AddressingPropertiesImpl();
        AttributedURIType epr = new AttributedURIType();
        epr.setValue("http://localhost:" + FaultToEndpointServer.FAULT_PORT);
        faultTo.setAddress(epr);
        addrProperties.setFaultTo(faultTo);
        
        BindingProvider provider = (BindingProvider) port;
        Map<String, Object> requestContext = provider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                           "http://localhost:" + FaultToEndpointServer.PORT + "/jaxws/add");
        requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);

        try {
            port.addNumbers(-1, -2);
            fail("Exception is expected");
        } catch (Exception e) {
            //do nothing
        }
               
        assertTrue("The response from faultTo endpoint is expected and actual response is " 
                   + new String(input.toByteArray()) , 
                   new String(input.toByteArray()).indexOf("The server sent HTTP status code :200") > -1);
        assertTrue("WS addressing header is expected", 
                   new String(input.toByteArray()).indexOf("http://www.w3.org/2005/08/addressing") > -1);       
        assertTrue("Fault deatil is expected", 
                   new String(input.toByteArray()).indexOf("Negative numbers cant be added") > -1);
    }
     
    private AddNumbersPortType getPort() throws Exception {
        URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
        assertNotNull("WSDL is null", wsdl);

        AddNumbersService service = new AddNumbersService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        AddNumbersPortType port = service.getAddNumbersPort(new AddressingFeature());
        //updateAddressPort(port, PORT);
        return port;
    }
}

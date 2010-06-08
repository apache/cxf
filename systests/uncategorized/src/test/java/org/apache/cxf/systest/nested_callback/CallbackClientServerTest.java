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

package org.apache.cxf.systest.nested_callback;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.nested_callback.NestedCallback;
import org.apache.nested_callback.SOAPService;
import org.apache.nested_callback.ServerPortType;

import org.junit.BeforeClass;
import org.junit.Test;

public class CallbackClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    public static final String CB_PORT = allocatePort(CallbackClientServerTest.class);
    
    private static final QName SERVICE_NAME 
        = new QName("http://apache.org/nested_callback", "SOAPService");
    private static final QName SERVICE_NAME_CALLBACK 
        = new QName("http://apache.org/nested_callback", "CallbackService");

    private static final QName PORT_NAME 
        = new QName("http://apache.org/nested_callback", "SOAPPort");

    private static final QName PORT_NAME_CALLBACK 
        = new QName("http://apache.org/nested_callback", "CallbackPort");
    
    private static final QName PORT_TYPE_CALLBACK
        = new QName("http://apache.org/nested_callback", "CallbackPortType");
    
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testCallback() throws Exception {

                    
        Object implementor = new CallbackImpl();
        String address = "http://localhost:" + CB_PORT + "/CallbackContext/NestedCallbackPort";
        Endpoint.publish(address, implementor);
    
        URL wsdlURL = getClass().getResource("/wsdl/nested_callback.wsdl");
    
        SOAPService ss = new SOAPService(wsdlURL, SERVICE_NAME);
        ServerPortType port = ss.getPort(PORT_NAME, ServerPortType.class);
        updateAddressPort(port, PORT);
   
        EndpointReferenceType ref = null;
        try {
            ref = EndpointReferenceUtils.getEndpointReference(wsdlURL, 
                                                              SERVICE_NAME_CALLBACK, 
                                                              PORT_NAME_CALLBACK.getLocalPart());
            EndpointReferenceUtils.setInterfaceName(ref, PORT_TYPE_CALLBACK);
            EndpointReferenceUtils.setAddress(ref, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        NestedCallback callbackObject = new NestedCallback();

        Source source = EndpointReferenceUtils.convertToXML(ref);
        W3CEndpointReference  w3cEpr = new W3CEndpointReference(source);
        
        
        callbackObject.setCallback(w3cEpr);
        String resp = port.registerCallback(callbackObject);

        assertEquals("registerCallback called", resp);
            
    }
    
    
}

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

import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerGreeterNoWsdlTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(ServerGreeterNoWsdl.class, true));
    }
    
    @Test    
    public void testInvocation() throws Exception {

        GreeterService service = new GreeterService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getGreeterPort();
            
            String greeting = greeter.greetMe("Bonjour");
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour", greeting);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    // test the http get invocation 
    
    @Test
    public void testGetGreetMe() throws Exception {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:9020/SoapContext/GreeterPort/greetMe/requestType/cxf");    
        httpConnection.connect();        
        
        assertEquals(200, httpConnection.getResponseCode());
    
        assertEquals("text/xml; charset=utf-8", httpConnection.getContentType());
        assertEquals("OK", httpConnection.getResponseMessage());
        
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);
        
        Document doc = XMLUtils.parse(in);
        assertNotNull(doc);
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("soap", Soap11.SOAP_NAMESPACE);
        ns.put("ns2", "http://cxf.apache.org/greeter_control/types");
        XPathUtils xu = new XPathUtils(ns);
        Node body = (Node) xu.getValue("/soap:Envelope/soap:Body", doc, XPathConstants.NODE);
        assertNotNull(body);
        String response = (String) xu.getValue("//ns2:greetMeResponse/ns2:responseType/text()", 
                                               body, 
                                               XPathConstants.STRING);
        assertEquals("Hello cxf", response);
    }

}

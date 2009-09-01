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

import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;


import org.apache.cxf.systest.jaxws.ServerMixedStyle.MixedTest;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.hello_world_mixedstyle.Greeter;
import org.apache.hello_world_mixedstyle.SOAPService;
import org.apache.hello_world_mixedstyle.types.GreetMe1;
import org.apache.hello_world_mixedstyle.types.GreetMeResponse;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClientServerMixedStyleTest extends AbstractClientServerTestBase {

    private final QName portName = new QName("http://apache.org/hello_world_mixedstyle", "SoapPort");


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(ServerMixedStyle.class));
    }
    
    @Test
    public void testMixedStyle() throws Exception {

        SOAPService service = new SOAPService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getPort(portName, Greeter.class);
            
            GreetMe1 request = new GreetMe1();
            request.setRequestType("Bonjour");
            GreetMeResponse greeting = greeter.greetMe(request);
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour", greeting.getResponseType());

            String reply = greeter.sayHi();
            assertNotNull("no response received from service", reply);
            assertEquals("Bonjour", reply);
            
            try {
                greeter.pingMe();
                fail("expected exception not caught");
            } catch (org.apache.hello_world_mixedstyle.PingMeFault f) {
                //ignore, expected
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    @Test
    public void testCXF885() throws Exception {
        Service serv = Service.create(new QName("http://example.com", "MixedTest"));
        MixedTest test = serv.getPort(MixedTest.class);
        ((BindingProvider)test).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                        "http://localhost:9027/cxf885");
        String ret = test.hello("A", "B");
        assertEquals("Hello A and B", ret);
        
        String ret2 = test.simple("Dan");
        assertEquals("Hello Dan", ret2);
        
        String ret3 = test.tripple("A", "B", "C");
        assertEquals("Tripple: A B C", ret3);
        
        String ret4 = test.simple2(24);
        assertEquals("Int: 24", ret4);
        
        serv = Service.create(new URL("http://localhost:9027/cxf885?wsdl"),
                              new QName("http://example.com", "MixedTestImplService"));
        test = serv.getPort(new QName("http://example.com", "MixedTestImplPort"),
                            MixedTest.class);
        
        ret = test.hello("A", "B");
        assertEquals("Hello A and B", ret);
        
        ret2 = test.simple("Dan");
        assertEquals("Hello Dan", ret2);
        
        ret3 = test.tripple("A", "B", "C");
        assertEquals("Tripple: A B C", ret3);
        
        ret4 = test.simple2(24);
        assertEquals("Int: 24", ret4);
    }

}

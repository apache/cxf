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

package org.apache.cxf.systest.http.auth;


import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class DigestAuthTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(DigestServer.class);

    private final QName serviceName = 
        new QName("http://apache.org/hello_world", "SOAPService");
    private final QName mortimerQ = 
        new QName("http://apache.org/hello_world", "Mortimer");
    public DigestAuthTest() {
    }
    
    @BeforeClass
    public static void startServer() throws Exception {
        launchServer(DigestServer.class);
        createStaticBus();
    }
    
    @Test    
    public void testDigestAuth() throws Exception {
        URL wsdl = getClass().getResource("../resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);
        
        setAddress(mortimer, "http://localhost:" + PORT + "/digestauth/greeter");
        
        Client client = ClientProxy.getClient(mortimer);
        
        HTTPConduit http = 
            (HTTPConduit) client.getConduit();
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setAuthorizationType("Digest");
        authPolicy.setUserName("foo");
        authPolicy.setPassword("bar");
        http.setAuthorization(authPolicy);

        String answer = mortimer.sayHi();
        assertEquals("Unexpected answer: " + answer, 
                "Hi", answer);

    }
    
    @Test    
    public void testNoAuth() throws Exception {
        URL wsdl = getClass().getResource("../resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);
        
        setAddress(mortimer, "http://localhost:" + PORT + "/digestauth/greeter");

        try {
            String answer = mortimer.sayHi();
            Assert.fail("Unexpected reply (" + answer + "). Should throw exception");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            Assert.assertEquals(HTTPException.class, cause.getClass());
            HTTPException he = (HTTPException)cause;
            Assert.assertEquals(401, he.getResponseCode());
        }
    }

}


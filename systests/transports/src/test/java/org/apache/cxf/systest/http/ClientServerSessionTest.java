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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ClientServerSessionTest extends AbstractBusClientServerTestBase {
    public static final String PORT = SessionServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        
        assertTrue("server did not launch correctly",
                       launchServer(SessionServer.class));
        
    }
    
    
    @Test    
    public void testInvocationWithSession() throws Exception {

        GreeterService service = new GreeterService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getGreeterPort();
            
            BindingProvider bp = (BindingProvider)greeter;
            updateAddressPort(bp, PORT);
            bp.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
            
            
            Map<String, List<String>> headers 
                = CastUtils.cast((Map<?, ?>)bp.getRequestContext().get("javax.xml.ws.http.request.headers"));

            if (headers == null) {
                headers = new HashMap<String, List<String>>();
                bp.getRequestContext()
                    .put("javax.xml.ws.http.request.headers", headers);
            }

            List<String> cookies = Arrays.asList(new String[] {"a=a", "b=b"});
            headers.put("Cookie", cookies);
            
            String greeting = greeter.greetMe("Bonjour");
            String cookie = "";
            if (greeting.indexOf(';') != -1) {
                cookie = greeting.substring(greeting.indexOf(';'));
                greeting = greeting.substring(0, greeting.indexOf(';'));
            }
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour", greeting);
            assertTrue(cookie.contains("a=a"));
            assertTrue(cookie.contains("b=b"));

            greeting = greeter.greetMe("Hello");
            cookie = "";
            if (greeting.indexOf(';') != -1) {
                cookie = greeting.substring(greeting.indexOf(';'));
                greeting = greeting.substring(0, greeting.indexOf(';'));
            }

            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour", greeting);
            assertTrue(cookie.contains("a=a"));
            assertTrue(cookie.contains("b=b"));
            
            
            greeting = greeter.greetMe("NiHao");
            cookie = "";
            if (greeting.indexOf(';') != -1) {
                cookie = greeting.substring(greeting.indexOf(';'));
                greeting = greeting.substring(0, greeting.indexOf(';'));
            }
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Hello", greeting);
            assertTrue(cookie.contains("a=a"));
            assertTrue(cookie.contains("b=b"));
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test    
    public void testInvocationWithoutSession() throws Exception {

        GreeterService service = new GreeterService();
        assertNotNull(service);

        try {
            Greeter greeter = service.getGreeterPort();
            updateAddressPort(greeter, PORT);

            String greeting = greeter.greetMe("Bonjour");
            
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Bonjour", greeting);
            
            greeting = greeter.greetMe("Hello");
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello Hello", greeting);
            
            
            greeting = greeter.greetMe("NiHao");
            assertNotNull("no response received from service", greeting);
            assertEquals("Hello NiHao", greeting);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    @Ignore("seem to get random failures on everything except Linux with this."
            + " Maybe a jetty issue.")
    public void testPublishOnBusyPort() {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        
        GreeterSessionImpl implementor = new GreeterSessionImpl();
        String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
        try {
            Endpoint.publish(address, implementor);
            if (!isWindows) {
                fail("Should have failed to publish as the port is busy");
            } else {
                System.err.println("Should have failed to publish as the port is busy, but certains "
                                   + "of Windows allow this.");
            }
        } catch (WebServiceException ex) {
            //ignore            
        }
        try {
            //CXF-1589
            Endpoint.publish(address, implementor);
            if (!isWindows) {
                fail("Should have failed to publish as the port is busy");
            } else {
                System.err.println("Should have failed to publish as the port is busy, but certains "
                                   + "of Windows allow this.");
            }
        } catch (WebServiceException ex) {
            //ignore
        }
        
    }
    
    @Test    
    public void testInvocationWithSessionFactory() throws Exception {
        doSessionsTest("http://localhost:" + PORT + "/Stateful1");
    }
    @Test    
    public void testInvocationWithSessionAnnotation() throws Exception {
        doSessionsTest("http://localhost:" + PORT + "/Stateful2");
    }
    @Test    
    public void testInvocationWithPerRequestAnnotation() throws Exception {
        GreeterService service = new GreeterService();
        assertNotNull(service);

        Greeter greeter = service.getGreeterPort();
        BindingProvider bp = (BindingProvider)greeter;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                   "http://localhost:" + PORT + "/PerRequest");
        bp.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        String result = greeter.greetMe("World");
        assertEquals("Hello World", result);
        assertEquals("Bonjour default", greeter.sayHi());
    }
    @Test    
    public void testInvocationWithSpringBeanAnnotation() throws Exception {
        GreeterService service = new GreeterService();
        assertNotNull(service);

        Greeter greeter = service.getGreeterPort();
        BindingProvider bp = (BindingProvider)greeter;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                                   "http://localhost:" + PORT + "/SpringBean");
        bp.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        String result = greeter.greetMe("World");
        assertEquals("Hello World", result);
        assertEquals("Bonjour World", greeter.sayHi());
    }
    
    private void doSessionsTest(String url) {
        GreeterService service = new GreeterService();
        assertNotNull(service);

        Greeter greeter = service.getGreeterPort();
        Greeter greeter2 = service.getGreeterPort();
        
        BindingProvider bp = (BindingProvider)greeter;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bp.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        bp = (BindingProvider)greeter2;

        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
        bp.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

        String result = greeter.greetMe("World");
        assertEquals("Hello World", result);
        assertEquals("Bonjour World", greeter.sayHi());
        
        result = greeter2.greetMe("Universe");
        assertEquals("Hello Universe", result);
        assertEquals("Bonjour Universe", greeter2.sayHi());
        
        //make sure session 1 was maintained
        assertEquals("Bonjour World", greeter.sayHi());
    }
}

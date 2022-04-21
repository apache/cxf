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


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class tests several issues and Conduit policies based
 * on a set up of redirecting servers.
 * <pre>
 *
 * Http Redirection:
 *
 * Rethwel(http:9004) ----> Mortimer (http:9000)
 *
 * Redirect Loop:
 *
 * Hurlon (http:9006) ----> Abost(http:9007) ----\
 *   ^                                            |
 *   |-------------------------------------------/
 */
public class HTTPConduitTest extends AbstractBusClientServerTestBase {
    private static final boolean IN_PROCESS = true;

    private static List<String> servers = new ArrayList<>();

    private static Map<String, String> addrMap = new TreeMap<>();

    private final QName serviceName =
        new QName("http://apache.org/hello_world", "SOAPService");
    private final QName rethwelQ =
        new QName("http://apache.org/hello_world", "Rethwel");
    private final QName mortimerQ =
        new QName("http://apache.org/hello_world", "Mortimer");
    private final QName hurlonQ =
        new QName("http://apache.org/hello_world", "Hurlon");

    public HTTPConduitTest() {
    }


    public static String getPort(String s) {
        return BusServer.PORTMAP.get(s);
    }

    @BeforeClass
    public static void allocatePorts() {
        BusServer.resetPortMap();
        addrMap.clear();
        addrMap.put("Mortimer", "http://localhost:" + getPort("PORT0") + "/");
        addrMap.put("Rethwel",  "http://localhost:" + getPort("PORT1") + "/");
        addrMap.put("Abost",    "http://localhost:" + getPort("PORT2") + "/");
        addrMap.put("Hurlon",   "http://localhost:" + getPort("PORT3") + "/");
        servers.clear();
    }


    /**
     * This function is used to start up a server. It only "starts" a
     * server if it hasn't been started before, hence its static nature.
     * <p>
     * This approach is used to start the needed servers for a particular test
     * instead of starting them all in "startServers". This single needed
     * server approach allieviates the pain in starting them all just to run
     * a particular test in the debugger.
     */
    public synchronized boolean startServer(String name) {
        if (servers.contains(name)) {
            return true;
        }
        Bus bus = BusFactory.getThreadDefaultBus(false);
        URL serverC =
            Server.class.getResource(name + ".cxf");
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
        boolean server = launchServer(Server.class, null,
                new String[] {
                    name,
                    addrMap.get(name),
                    serverC.toString() },
                IN_PROCESS);
        if (server) {
            servers.add(name);
        }
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(bus);
        return server;
    }

    @AfterClass
    public static void cleanUp() {
        Bus b = BusFactory.getDefaultBus(false);
        if (b != null) {
            b.shutdown(true);
        }
        b = BusFactory.getThreadDefaultBus(false);
        if (b != null) {
            b.shutdown(true);
        }
    }

    //methods that a subclass can override to inject a Proxy into the flow
    //and assert the proxy was appropriately called
    protected void configureProxy(Client c) {
    }
    protected void resetProxyCount() {
    }
    protected void assertProxyRequestCount(int i) {
    }


    private Greeter getMortimerGreeter() throws MalformedURLException {
        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);
        updateAddressPort(mortimer, getPort("PORT0"));

        configureProxy(ClientProxy.getClient(mortimer));
        return mortimer;
    }

    @Test
    public void testResponseMessage() throws Exception {
        startServer("Mortimer");
        Greeter mortimer = getMortimerGreeter();
        Client client = ClientProxy.getClient(mortimer);
        client.getRequestContext().put(HTTPConduit.SET_HTTP_RESPONSE_MESSAGE, true);
        
        String answer = mortimer.sayHi();
        answer = mortimer.sayHi();
        answer = mortimer.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Mortimer".equals(answer));
        assertProxyRequestCount(3);
        assertThat(client.getResponseContext().get(HTTPConduit.HTTP_RESPONSE_MESSAGE), equalTo("OK"));
    }
    
    @Test
    public void testBasicConnection() throws Exception {
        startServer("Mortimer");
        Greeter mortimer = getMortimerGreeter();
        Client client = ClientProxy.getClient(mortimer);

        String answer = mortimer.sayHi();
        answer = mortimer.sayHi();
        answer = mortimer.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Mortimer".equals(answer));
        assertProxyRequestCount(3);
        assertThat(client.getResponseContext().get(HTTPConduit.HTTP_RESPONSE_MESSAGE), nullValue());
    }

    @Test
    public void testLogLevelIssueCXF3466() throws Exception {
        startServer("Mortimer");
        Greeter mortimer = getMortimerGreeter();
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Level oldLevel = rootLogger.getLevel();
        rootLogger.setLevel(Level.FINE);
        try {
            // Will throw exception Stream is closed if bug is present
            mortimer.sayHi();
        } finally {
            rootLogger.setLevel(oldLevel);
        }
        assertProxyRequestCount(1);
    }

    /**
     * This methods tests that a conduit that is not configured
     * to follow redirects will not. The default is not to
     * follow redirects.
     * Rethwel redirects to Mortimer.
     *
     * Note: Unfortunately, the invocation will
     * "fail" for any number of other reasons.
     */
    @Test
    public void testHttp2HttpRedirectFail() throws Exception {
        startServer("Mortimer");
        startServer("Rethwel");

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter rethwel = service.getPort(rethwelQ, Greeter.class);
        assertNotNull("Port is null", rethwel);
        updateAddressPort(rethwel, getPort("PORT1"));
        configureProxy(ClientProxy.getClient(rethwel));

        String answer = null;
        try {
            answer = rethwel.sayHi();
            fail("Redirect didn't fail. Got answer: " + answer);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        assertProxyRequestCount(1);

    }

    /**
     * We use this class to reset the default bus.
     * Note: This may not always work in the future.
     * I was lucky in that "defaultBus" is actually a
     * protected static.
     */
    class DefaultBusFactory extends SpringBusFactory {
        public Bus createBus(URL config) {
            Bus bus = super.createBus(config, true);
            BusFactory.setDefaultBus(bus);
            BusFactory.setThreadDefaultBus(bus);
            return bus;
        }
    }

    /**
     * This method tests if http to http redirects work.
     * Rethwel redirects to Mortimer.
     */
    @Test
    public void testHttp2HttpRedirect() throws Exception {
        startServer("Mortimer");
        startServer("Rethwel");

        URL config = getClass().getResource("Http2HttpRedirect.cxf");

        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter rethwel = service.getPort(rethwelQ, Greeter.class);
        updateAddressPort(rethwel, getPort("PORT1"));
        assertNotNull("Port is null", rethwel);
        configureProxy(ClientProxy.getClient(rethwel));

        String answer = rethwel.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Mortimer".equals(answer));
        assertProxyRequestCount(2);
    }

    /**
     * This methods tests that a redirection loop will fail.
     * Hurlon redirects to Abost, which redirects to Hurlon.
     *
     * Note: Unfortunately, the invocation may "fail" for any
     * number of reasons.
     */
    @Test
    public void testHttp2HttpLoopRedirectFail() throws Exception {
        startServer("Abost");
        startServer("Hurlon");

        URL config = getClass().getResource("Http2HttpLoopRedirectFail.cxf");

        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter hurlon = service.getPort(hurlonQ, Greeter.class);
        assertNotNull("Port is null", hurlon);
        updateAddressPort(hurlon, getPort("PORT3"));
        configureProxy(ClientProxy.getClient(hurlon));

        String answer = null;
        try {
            answer = hurlon.sayHi();
            fail("Redirect didn't fail. Got answer: " + answer);
        } catch (Exception e) {
            // This exception will be one of not being able to
            // read from the StreamReader
            //e.printStackTrace();
        }
        assertProxyRequestCount(2);
    }

}


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

package org.apache.cxf.systest.ws.wssec10;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.common.SecurityTestUtil;
import org.apache.cxf.systest.ws.wssec10.server.Server;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.security.SecurityConstants;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import wssec.wssec10.IPingService;
import wssec.wssec10.PingService;


/**
 * It tests both DOM + StAX clients against the DOM server
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSecurity10Test extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String SSL_PORT = allocatePort(Server.class, 1);

    private static final String INPUT = "foo";
    private static boolean unrestrictedPoliciesInstalled;
    
    static {
        unrestrictedPoliciesInstalled = SecurityTestUtil.checkUnrestrictedPoliciesInstalled();
    };    
    
    final TestParam test;
    
    
    public WSSecurity10Test(TestParam type) {
        this.test = type;
    }
    
    static class TestParam {
        final String prefix;
        final boolean streaming;
        
        public TestParam(String p, boolean b) {
            prefix = p;
            streaming = b;
        }
        public String toString() {
            return prefix + ":" + (streaming ? "streaming" : "dom");
        }
    }
    
    @Parameters(name = "{0}")
    public static Collection<TestParam[]> data() {
       
        return Arrays.asList(new TestParam[][] {{new TestParam("UserName", false)},
                                                {new TestParam("UserNameOverTransport", false)},
                                                {new TestParam("MutualCertificate10SignEncrypt", false)},
                                                {new TestParam("MutualCertificate10SignEncryptRsa15TripleDes", false)},
                                                {new TestParam("UserName", true)},
                                                {new TestParam("UserNameOverTransport", true)},
                                                {new TestParam("MutualCertificate10SignEncrypt", true)},
                                                {new TestParam("MutualCertificate10SignEncryptRsa15TripleDes", true)}
        
        });
    }

    @BeforeClass
    public static void startServers() throws Exception {

        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        if (unrestrictedPoliciesInstalled) {
            createStaticBus("org/apache/cxf/systest/ws/wssec10/client.xml");
        } else {
            createStaticBus("org/apache/cxf/systest/ws/wssec10/client_restricted.xml");
        }
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @Test
    public void testClientServerDOM() {
        BusFactory.setDefaultBus(getStaticBus());
        BusFactory.setThreadDefaultBus(getStaticBus());
        URL wsdlLocation = null;

        PingService svc = null; 
        wsdlLocation = getWsdlLocation(test.prefix); 
        svc = new PingService(wsdlLocation);
        final IPingService port = 
            svc.getPort(
                new QName(
                    "http://WSSec/wssec10",
                    test.prefix + "_IPingService"
                ),
                IPingService.class
            );
     
        Client cl = ClientProxy.getClient(port);
        
        if (test.streaming) {
            // Streaming
            ((BindingProvider)port).getRequestContext().put(
                SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
            );
            ((BindingProvider)port).getResponseContext().put(
                SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
            );
        }
        
        HTTPConduit http = (HTTPConduit) cl.getConduit();
         
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(0);
        httpClientPolicy.setReceiveTimeout(0);
         
        http.setClient(httpClientPolicy);
        String output = port.echo(INPUT);
        assertEquals(INPUT, output);
        
        cl.destroy();
    }
        
    private static URL getWsdlLocation(String portPrefix) {
        try {
            if ("UserNameOverTransport".equals(portPrefix)) {
                return new URL("https://localhost:" + SSL_PORT + "/" + portPrefix + "?wsdl");
            } else if ("UserName".equals(portPrefix)) {
                return new URL("http://localhost:" + PORT + "/" + portPrefix + "?wsdl");
            } else if ("MutualCertificate10SignEncrypt".equals(portPrefix)) {
                return new URL("http://localhost:" + PORT + "/" + portPrefix + "?wsdl");
            } else if ("MutualCertificate10SignEncryptRsa15TripleDes".equals(portPrefix)) {
                return new URL("http://localhost:" + PORT + "/" + portPrefix + "?wsdl");
            }
        } catch (MalformedURLException mue) {
            return null;
        }
        return null;
    }

    
}

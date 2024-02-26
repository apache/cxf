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

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.systest.ws.wssec10.server.Server;
import org.apache.cxf.systest.ws.wssec10.server.ServerCustomAlgorithmSuite;
import org.apache.cxf.systest.ws.wssec10.server.StaxServer;
import org.apache.cxf.systest.ws.wssec10.server.StaxServerCustomAlgorithmSuite;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import wssec.wssec10.IPingService;
import wssec.wssec10.PingService;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *  Clone of WSSecurity10Test, whih uses CustomAlgorithmSuite. Test runs successfully on FIPS machines  (with
 *  *  -Dorg.apache.xml.security.securerandom.algorithm=PKCS1).
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class WSSecurity10CustomAlgorithmSuiteTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);
    static final String SSL_PORT = allocatePort(Server.class, 1);
    static final String STAX_SSL_PORT = allocatePort(StaxServer.class, 1);

    private static final String INPUT = "foo";

    final TestParam test;

    public WSSecurity10CustomAlgorithmSuiteTest(TestParam type) {
        this.test = type;
    }

    static class TestParam {
        final String prefix;
        final boolean expectsFailure;
        final String port;

        TestParam(String p, String port, boolean b) {
            prefix = p;
            this.port = port;
            expectsFailure = b;
        }
        public String toString() {
            return prefix + ":" + port + ":" + (expectsFailure ? "expectsFailure" : "");
        }
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {
            //test 01 -> default customAlgSuite -> succeeds and works on FIPS
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt01", PORT, false),
            //test 02 -> server side configured as Basic256, client side has default custom values -> fails
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt02", PORT, true),
            //test 03 -> server has default custom values, client side configured as Basic256 -> fails
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt03", PORT, true),
            //test 04 -> client and server configured to be the same as Basic256, with weak certs -> succeeds
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt04", PORT, false),


            //test 01 -> default customAlgSuite -> succeeds and works on FIPS
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt01", STAX_PORT, false),
            //test 02 -> server side configured as Basic256, client side has default custom values -> fails
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt02", STAX_PORT, true),
            //test 03 -> server has default custom values, client side configured as Basic256 -> fails
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt03", STAX_PORT, true),
            //test 04 -> client and server configured to be the same as Basic256, with weak certs -> succeeds
            new WSSecurity10CustomAlgorithmSuiteTest.TestParam("Customizable10SignEncrypt04", STAX_PORT, false)
        });
    }

    @BeforeClass
    public static void startServers() throws Exception {

        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(ServerCustomAlgorithmSuite.class, true)
        );
        assertTrue(
                "Server failed to launch",
                // run the server in the same process
                // set this to false to fork
                launchServer(StaxServerCustomAlgorithmSuite.class, true)
        );

        createStaticBus("org/apache/cxf/systest/ws/wssec10/client_customAlgorithmSuite.xml");
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testClientServer() throws Exception {
        BusFactory.setDefaultBus(getStaticBus());
        BusFactory.setThreadDefaultBus(getStaticBus());
        URL wsdlLocation = null;

        PingService svc = null;
        wsdlLocation = getWsdlLocation(test.prefix, test.port);
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

        HTTPConduit http = (HTTPConduit) cl.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(0);
        httpClientPolicy.setReceiveTimeout(0);

        http.setClient(httpClientPolicy);

        String output;
        try {
            output = port.echo(INPUT);
            if (test.expectsFailure) {
                Assert.fail("Failure was expected.");
            }
        } catch (Exception e) {
            if (test.expectsFailure) {
                assertTrue(e.getMessage() != null
                        && (e.getMessage().contains("A security error")
                                || e.getMessage().contains("Error reading XMLStreamReader")
                                || e.getMessage().contains("<wsse:Security> header")));
                //test failed "as expected"
                return;
            }

            throw e;
        }
        assertEquals(INPUT, output);

        cl.destroy();
    }

    private static URL getWsdlLocation(String portPrefix, String port) {
        try {
            if ("UserNameOverTransport".equals(portPrefix)) {
                return new URL("https://localhost:" + port + "/" + portPrefix + "?wsdl");
            }
            return new URL("http://localhost:" + port + "/" + portPrefix + "?wsdl");
        } catch (MalformedURLException mue) {
            return null;
        }
    }


}

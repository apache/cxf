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


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssec10.server.AuthorizedServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import wssec.wssec10.IPingService;
import wssec.wssec10.PingService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class WSSecurity10UsernameAuthorizationTest extends AbstractBusClientServerTestBase {
    static final String SSL_PORT = allocatePort(AuthorizedServer.class, 1);
    static final String PORT = allocatePort(AuthorizedServer.class);

    private static final String INPUT = "foo";

    @BeforeClass
    public static void startServers() throws Exception {

        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(AuthorizedServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    @Test
    public void testClientServerUTOnlyAuthorized() throws IOException {

        String configName = "org/apache/cxf/systest/ws/wssec10/client.xml";
        Bus bus = new SpringBusFactory().createBus(configName);
        IPingService port = getUTOnlyPort(bus, false);

        final String output = port.echo(INPUT);
        assertEquals(INPUT, output);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @Test
    public void testClientServerUTOnlyUnauthorized() throws IOException {

        String configName = "org/apache/cxf/systest/ws/wssec10/client_unauthorized.xml";
        Bus bus = new SpringBusFactory().createBus(configName);
        IPingService port = getUTOnlyPort(bus, true);

        try {
            port.echo(INPUT);
            fail("Frank is unauthorized");
        } catch (Exception ex) {
            assertEquals("Unauthorized", ex.getMessage());
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @Test
    public void testClientServerComplexPolicyAuthorized() throws IOException {

        String configName = "org/apache/cxf/systest/ws/wssec10/client.xml";
        Bus bus = new SpringBusFactory().createBus(configName);
        IPingService port = getComplexPolicyPort(bus);

        final String output = port.echo(INPUT);
        assertEquals(INPUT, output);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @Test
    public void testClientServerComplexPolicyUnauthorized() throws IOException {

        String configName = "org/apache/cxf/systest/ws/wssec10/client_unauthorized.xml";
        Bus bus = new SpringBusFactory().createBus(configName);
        IPingService port = getComplexPolicyPort(bus);

        try {
            port.echo(INPUT);
            fail("Frank is unauthorized");
        } catch (Exception ex) {
            assertEquals("Unauthorized", ex.getMessage());
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    private static IPingService getComplexPolicyPort(Bus bus) {

        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        PingService svc = new PingService(getWsdlLocation("UserNameOverTransport"));
        return svc.getPort(
                new QName(
                    "http://WSSec/wssec10",
                    "UserNameOverTransport" + "_IPingService"
                ),
                IPingService.class
            );
    }

    private static IPingService getUTOnlyPort(Bus bus, boolean hashed) {

        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        PingService svc = new PingService(getWsdlLocation(hashed));
        return svc.getPort(
                new QName(
                    "http://WSSec/wssec10",
                    hashed ? "UserName_IPingService_hashed" : "UserName_IPingService"
                ),
                IPingService.class
            );
    }

    private static URL getWsdlLocation(boolean hashed) {
        try {
            return new URL("http://localhost:" + PORT + "/"
                           + (hashed ? "HashedUserName" : "UserName") + "?wsdl");
        } catch (MalformedURLException mue) {
            return null;
        }

    }


    private static URL getWsdlLocation(String portPrefix) {
        try {
            if ("UserNameOverTransport".equals(portPrefix)) {
                return new URL("https://localhost:" + SSL_PORT + "/" + portPrefix + "?wsdl");
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

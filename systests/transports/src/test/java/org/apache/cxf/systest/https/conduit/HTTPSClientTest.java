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

package org.apache.cxf.systest.https.conduit;

import java.net.URL;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.jsse.TLSParameterJaxBUtils;
import org.apache.cxf.configuration.security.KeyManagersType;
import org.apache.cxf.configuration.security.KeyStoreType;
import org.apache.cxf.configuration.security.TrustManagersType;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.systest.https.BusServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test is meant to run against a spring-loaded
 * HTTP/S service.
 */
public class HTTPSClientTest extends AbstractBusClientServerTestBase {
    //
    // data
    //

    /**
     * the package path used to locate resources specific to this test
     */
    private void setTheConfiguration(String config) {
        //System.setProperty("javax.net.debug", "all");
        try {
            System.setProperty(
                Configurer.USER_CFG_FILE_PROPERTY_URL,
                HTTPSClientTest.class.getResource(config).toString()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void setupPorts() {
        BusServer.resetPortMap();
    }

    protected void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork a new process
            launchServer(BusServer.class, true)
        );
    }


    protected void stopServers() throws Exception {
        stopAllServers();
        System.clearProperty(Configurer.USER_CFG_FILE_PROPERTY_URL);
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }


    //
    // tests
    //
    public final void testSuccessfulCall(String configuration,
                                         String address) throws Exception {
        testSuccessfulCall(configuration, address, null);
    }
    public final void testSuccessfulCall(String configuration,
                                         String address,
                                         URL url) throws Exception {
        testSuccessfulCall(configuration, address, url, false);
    }
    public final void testSuccessfulCall(String configuration,
                                         String address,
                                         URL url,
                                         boolean dynamicClient) throws Exception {
        setTheConfiguration(configuration);
        startServers();
        if (url == null) {
            url = SOAPService.WSDL_LOCATION;
        }

        //CXF-4037 - dynamic client isn't using the conduit settings to resolve schemas
        if (dynamicClient) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            JaxWsDynamicClientFactory.newInstance(BusFactory.getDefaultBus())
                .createClient(url.toExternalForm());
            Thread.currentThread().setContextClassLoader(loader);
        }



        SOAPService service = new SOAPService(url, SOAPService.SERVICE);
        assertNotNull("Service is null", service);
        final Greeter port = service.getHttpsPort();
        assertNotNull("Port is null", port);

        BindingProvider provider = (BindingProvider)port;
        provider.getRequestContext().put(
              BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
              address);

        //for (int x = 0; x < 100000; x++) {
        assertEquals(port.greetMe("Kitty"), "Hello Kitty");
        //}


        stopServers();
    }

    @Test
    public final void testJaxwsServer() throws Exception {
        testSuccessfulCall("jaxws-server.xml",
                           "https://localhost:" + BusServer.getPort(2) + "/SoapContext/HttpsPort");
    }
    @Test
    public final void testJaxwsServerChangeHttpsToHttp() throws Exception {
        testSuccessfulCall("jaxws-server.xml",
                            "http://localhost:" + BusServer.getPort(3) + "/SoapContext/HttpPort");
    }
    @Test
    public final void testJaxwsEndpoint() throws Exception {
        testSuccessfulCall("jaxws-publish.xml",
                           "https://localhost:" + BusServer.getPort(1) + "/SoapContext/HttpsPort");
    }
    @Test
    public final void testJaxwsEndpointCallback() throws Exception {
        testSuccessfulCall("jaxws-publish-callback.xml",
                           "https://localhost:" + BusServer.getPort(1) + "/SoapContext/HttpsPort");
    }
    @Test
    public final void testJaxwsTLSRefsEndpoint() throws Exception {
        testSuccessfulCall("jaxws-tlsrefs-publish.xml",
                           "https://localhost:" + BusServer.getPort(1) + "/SoapContext/HttpsPort");
    }
    @Test
    public final void testPKCS12Endpoint() throws Exception {
        testSuccessfulCall("pkcs12.xml",
                           "https://localhost:" + BusServer.getPort(6) + "/SoapContext/HttpsPort");
    }

    @Test
    public final void testResourceKeySpecEndpoint() throws Exception {
        testSuccessfulCall("resource-key-spec.xml",
                           "https://localhost:" + BusServer.getPort(4) + "/SoapContext/HttpsPort");
    }
    @Test
    public final void testResourceKeySpecEndpointURL() throws Exception {
        testSuccessfulCall("resource-key-spec-url.xml",
                           "https://localhost:" + BusServer.getPort(5) + "/SoapContext/HttpsPort",
                           new URL("https://localhost:" + BusServer.getPort(5) + "/SoapContext/HttpsPort?wsdl"),
                           true);

    }

    public static class ServerManagersFactory {
        public static KeyManager[] getKeyManagers() {
            KeyManagersType kmt = new KeyManagersType();
            KeyStoreType kst = new KeyStoreType();
            kst.setResource("keys/Bethal.jks");
            kst.setPassword("password");
            kst.setType("JKS");

            kmt.setKeyStore(kst);
            kmt.setKeyPassword("password");
            try {
                return TLSParameterJaxBUtils.getKeyManagers(kmt);
            } catch (Exception e) {
                throw new RuntimeException("failed to retrieve key managers", e);
            }
        }

        public static TrustManager[] getTrustManagers() {
            TrustManagersType tmt = new TrustManagersType();
            KeyStoreType kst = new KeyStoreType();
            kst.setResource("keys/Truststore.jks");
            kst.setPassword("password");
            kst.setType("JKS");

            tmt.setKeyStore(kst);
            try {
                return TLSParameterJaxBUtils.getTrustManagers(tmt, false);
            } catch (Exception e) {
                throw new RuntimeException("failed to retrieve trust managers", e);
            }
        }
    }

    public static class ClientManagersFactory {
        public static KeyManager[] getKeyManagers() {
            KeyManagersType kmt = new KeyManagersType();
            KeyStoreType kst = new KeyStoreType();
            kst.setResource("keys/Morpit.jks");
            kst.setPassword("password");
            kst.setType("JKS");

            kmt.setKeyStore(kst);
            kmt.setKeyPassword("password");
            try {
                return TLSParameterJaxBUtils.getKeyManagers(kmt);
            } catch (Exception e) {
                throw new RuntimeException("failed to retrieve key managers", e);
            }
        }

        public static TrustManager[] getTrustManagers() {
            TrustManagersType tmt = new TrustManagersType();
            KeyStoreType kst = new KeyStoreType();
            kst.setResource("keys/Truststore.jks");
            kst.setPassword("password");
            kst.setType("JKS");

            tmt.setKeyStore(kst);
            try {
                return TLSParameterJaxBUtils.getTrustManagers(tmt, false);
            } catch (Exception e) {
                throw new RuntimeException("failed to retrieve trust managers", e);
            }
        }
    }
}

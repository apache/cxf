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
package org.apache.cxf.transport.http.netty.client.integration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.netty.client.NettyHttpConduit;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.SOAPService;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SSLNettyClientTest extends AbstractBusClientServerTestBase {

    public static final String PORT = allocatePort(SSLNettyClientTest.class);

    static {
        System.setProperty("SSLNettyClientTest.port", PORT);
    }

    static Endpoint ep;

    static Greeter g;

    static String address;

    @BeforeClass
    public static void start() throws Exception {
        Bus b = createStaticBus("/org/apache/cxf/transport/http/netty/client/integration/ServerConfig.xml");
        BusFactory.setThreadDefaultBus(b);
        address = "https://localhost:" + PORT + "/SoapContext/SoapPort";
        ep = Endpoint.publish(address,
                new org.apache.hello_world_soap_http.GreeterImpl());

        URL wsdl = SSLNettyClientTest.class.getResource("/wsdl/hello_world.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl);
        assertNotNull("Service is null", service);

        g = service.getSoapPort();
        assertNotNull("Port is null", g);
    }

    @AfterClass
    public static void stop() throws Exception {
        if (g != null) {
            ((java.io.Closeable)g).close();
        }
        if (ep != null) {
            ep.stop();
        }
        ep = null;
    }

    @Test
    public void testInvocation() throws Exception {
        setupTLS(g);
        setAddress(g, address);
        String response = g.greetMe("test");
        assertEquals("Get a wrong response", "Hello test", response);

        GreetMeResponse resp = (GreetMeResponse)g.greetMeAsync("asyncTest", new AsyncHandler<GreetMeResponse>() {
            public void handleResponse(Response<GreetMeResponse> res) {
                try {
                    res.get().getResponseType();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }).get();
        assertEquals("Hello asyncTest", resp.getResponseType());

        MyLaterResponseHandler handler = new MyLaterResponseHandler();
        g.greetMeLaterAsync(1000, handler).get();
        // need to check the result here
        assertEquals("Hello, finally!", handler.getResponse().getResponseType());

    }

    private static void setupTLS(Greeter port)
        throws FileNotFoundException, IOException, GeneralSecurityException {
        String keyStoreLoc =
            "/keys/clientstore.jks";
        NettyHttpConduit httpConduit = (NettyHttpConduit) ClientProxy.getClient(port).getConduit();

        TLSClientParameters tlsCP = new TLSClientParameters();
        String keyPassword = "ckpass";
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(SSLNettyClientTest.class.getResourceAsStream(keyStoreLoc), "cspass".toCharArray());
        KeyManager[] myKeyManagers = getKeyManagers(keyStore, keyPassword);
        tlsCP.setKeyManagers(myKeyManagers);


        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(SSLNettyClientTest.class.getResourceAsStream(keyStoreLoc), "cspass".toCharArray());
        TrustManager[] myTrustStoreKeyManagers = getTrustManagers(trustStore);
        tlsCP.setTrustManagers(myTrustStoreKeyManagers);


        tlsCP.setDisableCNCheck(true);
        httpConduit.setTlsClientParameters(tlsCP);
    }

    private static TrustManager[] getTrustManagers(KeyStore trustStore)
        throws NoSuchAlgorithmException, KeyStoreException {
        String alg = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory fac = TrustManagerFactory.getInstance(alg);
        fac.init(trustStore);
        return fac.getTrustManagers();
    }

    private static KeyManager[] getKeyManagers(KeyStore keyStore, String keyPassword)
        throws GeneralSecurityException, IOException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        char[] keyPass = keyPassword != null
                     ? keyPassword.toCharArray()
                     : null;
        KeyManagerFactory fac = KeyManagerFactory.getInstance(alg);
        fac.init(keyStore, keyPass);
        return fac.getKeyManagers();
    }

    private final class MyLaterResponseHandler implements AsyncHandler<GreetMeLaterResponse> {
        GreetMeLaterResponse response;
        @Override
        public void handleResponse(Response<GreetMeLaterResponse> res) {
            try {
                response = res.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        GreetMeLaterResponse getResponse() {
            return response;
        }

    }


}

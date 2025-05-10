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


import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusApplicationContext;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.https.BusServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transport.https.HttpsURLConnectionInfo;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;
import org.springframework.context.ApplicationContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
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
 * Poltim(https:9005)  ----> Mortimer (http:9000)
 *
 * HttpS redirection/Trust:
 *
 * Tarpin(https:9003) ----> Gordy(https:9001) ----> Bethal(https:9002)
 *
 * Hostname Verifier Test
 *
 * Morpit (https:9008)
 *
 * </pre>HTTPConduitTest
 * The Bethal server issues 401 with differing realms depending on the
 * User name given in the authorization header.
 * <p>
 * The Morpit has a CN that is not equal to "localhost" to kick in
 * the Hostname Verifier.
 */
public class HTTPSConduitTest extends AbstractBusClientServerTestBase {
    private static final boolean IN_PROCESS = true;

    private static TLSClientParameters tlsClientParameters = new TLSClientParameters();
    private static List<String> servers = new ArrayList<>();

    private static Map<String, Collection<String>> addrMap = new TreeMap<>();

    static {
        try (InputStream key = ClassLoaderUtils.getResourceAsStream("keys/Morpit.jks", HTTPSConduitTest.class);
            InputStream truststore =
                ClassLoaderUtils.getResourceAsStream("keys/Truststore.jks", HTTPSConduitTest.class);) {
            //System.setProperty("javax.net.debug", "all");
            KeyManager[] kmgrs = getKeyManagers(getKeyStore("JKS", key, "password"), "password");

            TrustManager[] tmgrs = getTrustManagers(getKeyStore("JKS", truststore, "password"));

            tlsClientParameters.setKeyManagers(kmgrs);
            tlsClientParameters.setTrustManagers(tmgrs);
        } catch (Exception e) {
            throw new RuntimeException("Static initialization failed", e);
        }
    }

    private final QName serviceName =
        new QName("http://apache.org/hello_world", "SOAPService");
    private final QName bethalQ =
        new QName("http://apache.org/hello_world", "Bethal");
    private final QName gordyQ =
        new QName("http://apache.org/hello_world", "Gordy");
    private final QName tarpinQ =
        new QName("http://apache.org/hello_world", "Tarpin");
    private final QName poltimQ =
        new QName("http://apache.org/hello_world", "Poltim");

    public HTTPSConduitTest() {
    }


    public static String getPort(String s) {
        return BusServer.PORTMAP.get(s);
    }

    @BeforeClass
    public static void allocatePorts() {
        BusServer.resetPortMap();
        addrMap.clear();
        addrMap.put("Mortimer", List.of("http://localhost:" + getPort("PORT0") + "/"));
        addrMap.put("Tarpin",   List.of("https://localhost:" + getPort("PORT1") + "/"));
        addrMap.put("Poltim",   List.of("https://localhost:" + getPort("PORT2") + "/"));
        addrMap.put("Gordy",    List.of("https://localhost:" + getPort("PORT3") + "/"));
        addrMap.put("Bethal",   List.of("https://localhost:" + getPort("PORT4") + "/", 
            "https://localhost:" + getPort("PORT6") + "/"));
        addrMap.put("Morpit",   List.of("https://localhost:" + getPort("PORT5") + "/"));
        tlsClientParameters.setDisableCNCheck(true);
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
                    addrMap.get(name).stream().collect(Collectors.joining(",")),
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

    public static KeyStore getKeyStore(String ksType, InputStream inputStream, String ksPassword)
        throws GeneralSecurityException,
               IOException {

        String type = ksType != null
                    ? ksType
                    : KeyStore.getDefaultType();

        char[] password = ksPassword != null
                    ? ksPassword.toCharArray()
                    : null;

        // We just use the default Keystore provider
        KeyStore keyStore = KeyStore.getInstance(type);

        keyStore.load(inputStream, password);

        return keyStore;
    }

    public static KeyManager[] getKeyManagers(KeyStore keyStore, String keyPassword)
        throws GeneralSecurityException,
               IOException {
        // For tests, we just use the default algorithm
        String alg = KeyManagerFactory.getDefaultAlgorithm();

        char[] keyPass = keyPassword != null
                     ? keyPassword.toCharArray()
                     : null;

        // For tests, we just use the default provider.
        KeyManagerFactory fac = KeyManagerFactory.getInstance(alg);

        fac.init(keyStore, keyPass);

        return fac.getKeyManagers();
    }

    public static TrustManager[] getTrustManagers(KeyStore keyStore)
        throws GeneralSecurityException,
               IOException {
        // For tests, we just use the default algorithm
        String alg = TrustManagerFactory.getDefaultAlgorithm();

        // For tests, we just use the default provider.
        TrustManagerFactory fac = TrustManagerFactory.getInstance(alg);

        fac.init(keyStore);

        return fac.getTrustManagers();
    }

    //methods that a subclass can override to inject a Proxy into the flow
    //and assert the proxy was appropriately called
    protected void configureProxy(Client c) {
    }
    protected void resetProxyCount() {
    }
    protected void assertProxyRequestCount(int i) {
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
     * This methods tests a basic https connection to Bethal.
     * It supplies an authorization policy with preemptive user/pass
     * to avoid the 401.
     */
    @Test
    public void testHttpsBasicConnectionWithConfig() throws Exception {
        startServer("Bethal");

        URL config = getClass().getResource("BethalClientConfig.cxf");

        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);
        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);

        assertNotNull("Port is null", bethal);
        updateAddressPort(bethal, getPort("PORT4"));
        verifyBethalClient(bethal);
    }

    @Test
    public void testGetClientFromSpringContext() throws Exception {
        startServer("Bethal");

        BusFactory.setDefaultBus(null);
        // The client bean configuration file
        URL beans = getClass().getResource("BethalClientBeans.xml");
        // We go through the back door, setting the default bus.
        Bus bus = new DefaultBusFactory().createBus(beans);

        ApplicationContext context = bus.getExtension(BusApplicationContext.class);
        Greeter bethal = (Greeter)context.getBean("Bethal");
        updateAddressPort(bethal, getPort("PORT4"));
        // verify the client side's setting
        verifyBethalClient(bethal);
    }

    // we just verify the configurations are loaded successfully
    private void verifyBethalClient(Greeter bethal) {
        Client client = ClientProxy.getClient(bethal);

        HTTPConduit http =
            (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = http.getClient();
        assertTrue("the httpClientPolicy's autoRedirect should be true",
                     httpClientPolicy.isAutoRedirect());
        TLSClientParameters tlsParameters = http.getTlsClientParameters();
        assertNotNull("the http conduit's tlsParameters should not be null", tlsParameters);


        // If we set any name, but Edward, Mary, or George,
        // and a password of "password" we will get through
        // Bethal.
        AuthorizationPolicy authPolicy = http.getAuthorization();
        assertEquals("Set the wrong user name from the configuration",
                     "Betty", authPolicy.getUserName());
        assertEquals("Set the wrong pass word form the configuration",
                     "password", authPolicy.getPassword());

        configureProxy(ClientProxy.getClient(bethal));

        String answer = bethal.sayHi();
        answer = bethal.sayHi();
        answer = bethal.sayHi();
        answer = bethal.sayHi();
        answer = bethal.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Bethal".equals(answer));

        //With HTTPS, it will just be a CONNECT to the proxy and all the
        //data is encrypted.  Thus, the proxy cannot distinquish the requests
        assertProxyRequestCount(0);
    }

    /**
     * This methods tests a basic https connection to Bethal.
     * It supplies an authorization policy with premetive user/pass
     * to avoid the 401.
     */
    @Test
    public void testHttpsBasicConnection() throws Exception {
        // Use common/shared TLSClientParameters
        testHttpsBasicConnection(tlsClientParameters);
    }

    @Test
    public void testHttpsBasicConnectionCustomSslContext() throws Exception {
        // Use custom SSLContext registered in TLSClientParameters
        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        try (InputStream keyStoreIs = ClassLoaderUtils.getResourceAsStream(
            "keys/Morpit.jks", HTTPSConduitTest.class
        )) {
            KeyManager[] keyManagers = getKeyManagers(getKeyStore(
                "JKS", keyStoreIs, "password"), "password"
            );
            // I need to disable CN verification (as certificate contains Bethal as CN),
            // but I cannot use TLSClientParameters.setDisableCNCheck(), because in this case
            // URLCONNECTION is always used (see HttpClientHTTPConduit.setupConnection())
            // -> I must used own TrustManager without verification
            TrustManager trustManager = new X509ExtendedTrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
                }
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                }
            };
            ctx.init(
                keyManagers,
                new TrustManager[] {trustManager},
                SecureRandom.getInstance("SHA1PRNG")
            );
        }

        // HostnameVerifier (disable host name verification)
        class AllowAllHostnameVerifier implements HostnameVerifier {
            @Override
            public boolean verify(String host, SSLSession session) {
                try {
                    Certificate[] certs = session.getPeerCertificates();
                    return certs != null && certs[0] instanceof X509Certificate;
                } catch (SSLPeerUnverifiedException e) {
                    return false;
                }
            }
        }

        // TLSClientParameters (Custom SSLContext)
        TLSClientParameters tlsClientParams = new TLSClientParameters();
        tlsClientParams.setSslContext(ctx);
        // TLSClientParameters (disable host name verification - now needed only when URLConnection is used)
        tlsClientParams.setHostnameVerifier(new AllowAllHostnameVerifier());

        testHttpsBasicConnection(tlsClientParams);
    }

    private void testHttpsBasicConnection(TLSClientParameters tlsClientParams) throws Exception {
        startServer("Bethal");

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        assertNotNull("Port is null", bethal);
        updateAddressPort(bethal, getPort("PORT4"));

        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(bethal);
        client.getRequestContext().put("share.httpclient.http.conduit", false);

        HTTPConduit http =
            (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setAutoRedirect(false);
        // If we set any name, but Edward, Mary, or George,
        // and a password of "password" we will get through
        // Bethal.
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("Betty");
        authPolicy.setPassword("password");

        http.setClient(httpClientPolicy);
        http.setTlsClientParameters(tlsClientParams);
        http.setAuthorization(authPolicy);

        configureProxy(client);
        String answer = bethal.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Bethal".equals(answer));
        assertProxyRequestCount(0);
    }


    @Test
    public void testHttpsRedirectToHttpFail() throws Exception {
        startServer("Mortimer");
        startServer("Poltim");

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter poltim = service.getPort(poltimQ, Greeter.class);
        assertNotNull("Port is null", poltim);
        updateAddressPort(poltim, getPort("PORT2"));

        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(poltim);
        client.getRequestContext().put("share.httpclient.http.conduit", false);

        HTTPConduit http =
            (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setAutoRedirect(true);

        http.setClient(httpClientPolicy);
        http.setTlsClientParameters(tlsClientParameters);
        configureProxy(client);
        poltim.sayHi();
        //client -> poltim is https and thus not recorded but then redirected to mortimer
        //client -> mortimer is http and recoreded
        assertProxyRequestCount(1);
    }

    class MyHttpsTrustDecider extends MessageTrustDecider {

        private String[] trustName;
        private int      called;

        MyHttpsTrustDecider(String name) {
            trustName = new String[] {name};
        }

        MyHttpsTrustDecider(String[] name) {
            trustName = name;
        }

        public int wasCalled() {
            return called;
        }

        public void establishTrust(
            String            conduitName,
            URLConnectionInfo cinfo,
            Message           message
        ) throws UntrustedURLConnectionIOException {

            called++;

            HttpsURLConnectionInfo ci = (HttpsURLConnectionInfo) cinfo;
            boolean trusted = false;
            for (int i = 0; i < trustName.length; i++) {
                trusted = trusted
                         || ci.getPeerPrincipal()
                                 .toString().contains("OU=" + trustName[i]);
            }
            if (!trusted) {
                throw new UntrustedURLConnectionIOException(
                        "Peer Principal \""
                        + ci.getPeerPrincipal()
                        + "\" does not contain "
                        + getTrustNames());
            }
        }

        private String getTrustNames() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < trustName.length; i++) {
                sb.append("\"OU=");
                sb.append(trustName[i]);
                sb.append('"');
                if (i < trustName.length - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    @Test
    public void testHttpsTrust() throws Exception {
        startServer("Bethal");

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        assertNotNull("Port is null", bethal);
        updateAddressPort(bethal, getPort("PORT4"));

        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(bethal);
        client.getRequestContext().put("share.httpclient.http.conduit", false);

        HTTPConduit http =
            (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setAutoRedirect(false);
        // If we set any name, but Edward, Mary, or George,
        // and a password of "password" we will get through
        // Bethal.
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("Betty");
        authPolicy.setPassword("password");

        http.setClient(httpClientPolicy);
        http.setTlsClientParameters(tlsClientParameters);
        http.setAuthorization(authPolicy);

        // Our expected server should be OU=Bethal
        http.setTrustDecider(new MyHttpsTrustDecider("Bethal"));

        configureProxy(client);
        String answer = bethal.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Bethal".equals(answer));
        assertProxyRequestCount(0);


        // Nobody will not equal OU=Bethal
        MyHttpsTrustDecider trustDecider =
                                 new MyHttpsTrustDecider("Nobody");
        http.setTrustDecider(trustDecider);
        try {
            answer = bethal.sayHi();
            fail("Unexpected answer from Bethal: " + answer);
        } catch (Exception e) {
            //e.printStackTrace();
            //assertTrue("Trust Decider was not called",
            //              0 > trustDecider.wasCalled());
        }
        assertProxyRequestCount(0);
    }

    @Test
    public void testHttpsTrustRedirect() throws Exception {
        startServer("Tarpin");
        startServer("Gordy");
        startServer("Bethal");

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter tarpin = service.getPort(tarpinQ, Greeter.class);
        assertNotNull("Port is null", tarpin);
        updateAddressPort(tarpin, getPort("PORT1"));

        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(tarpin);
        client.getRequestContext().put("share.httpclient.http.conduit", false);

        HTTPConduit http =
            (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setAutoRedirect(true);
        // If we set any name, but Edward, Mary, or George,
        // and a password of "password" we will get through
        // Bethal.
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName("Betty");
        authPolicy.setPassword("password");

        http.setClient(httpClientPolicy);
        http.setTlsClientParameters(tlsClientParameters);
        http.setAuthorization(authPolicy);

        // We get redirected from Tarpin, to Gordy, to Bethal.
        MyHttpsTrustDecider trustDecider =
            new MyHttpsTrustDecider(
                    new String[] {"Tarpin", "Gordy", "Bethal"});
        http.setTrustDecider(trustDecider);

        // We actually get our answer from Bethal at the end of the
        // redirects.
        configureProxy(ClientProxy.getClient(tarpin));

        String answer = tarpin.sayHi();
        assertProxyRequestCount(0);

        assertTrue("Trust Decider wasn't called correctly",
                       3 == trustDecider.wasCalled());
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Bethal".equals(answer));

        // Limit the redirects to 1, since there are two, this should fail.
        http.getClient().setMaxRetransmits(1);

        try {
            String a2 = tarpin.sayHi();
            fail("Unexpected answer from Tarpin: " + a2);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        assertProxyRequestCount(0);

        // Set back to unlimited.
        http.getClient().setMaxRetransmits(-1);

        // Effectively we will not trust Gordy in the middle.
        trustDecider =
                new MyHttpsTrustDecider(
                    new String[] {"Tarpin", "Bethal"});
        http.setTrustDecider(trustDecider);

        try {
            String a2 = tarpin.sayHi();
            fail("Unexpected answer from Tarpin: " + a2);
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue("Trust Decider wasn't called correctly",
                     2 == trustDecider.wasCalled());
        }
        assertProxyRequestCount(0);
    }

    public class MyBasicAuthSupplier implements HttpAuthSupplier {

        String realm;
        String user;
        String pass;

        /**
         * This will loop from Cronus, to Andromeda, to Zorantius
         */
        MyBasicAuthSupplier() {
        }

        MyBasicAuthSupplier(String r, String u, String p) {
            realm = r;
            user = u;
            pass = p;
        }

        /**
         * If we don't have the realm set, then we loop
         * through the realms.
         */
        public String getAuthorization(
                AuthorizationPolicy authPolicy,
                URI     currentURI,
                Message message,
                String fullHeader
        ) {
            String reqestedRealm = new HttpAuthHeader(fullHeader).getRealm();
            if (realm != null && realm.equals(reqestedRealm)) {
                return createUserPass(user, pass);
            }
            if ("Andromeda".equals(reqestedRealm)) {
                // This will get us another 401 to Zorantius
                return createUserPass("Edward", "password");
            }
            if ("Zorantius".equals(reqestedRealm)) {
                // George will get us another 401 to Cronus
                return createUserPass("George", "password");
            }
            if ("Cronus".equals(reqestedRealm)) {
                // Mary will get us another 401 to Andromeda
                return createUserPass("Mary", "password");
            }
            return null;
        }

        private String createUserPass(String usr, String pwd) {
            return DefaultBasicAuthSupplier.getBasicAuthHeader(usr, pwd);
        }

        public boolean requiresRequestCaching() {
            return false;
        }

    }

    /**
     * This tests redirects through Gordy to Bethal. Bethal will
     * supply a series of 401s. See PushBack401.
     */
    @Test
    public void testHttpsRedirect401Response() throws Exception {
        startServer("Gordy");
        startServer("Bethal");

        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter gordy = service.getPort(gordyQ, Greeter.class);
        assertNotNull("Port is null", gordy);
        updateAddressPort(gordy, getPort("PORT3"));

        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(gordy);
        client.getRequestContext().put("share.httpclient.http.conduit", false);

        HTTPConduit http =
            (HTTPConduit) client.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

        httpClientPolicy.setAutoRedirect(true);
        http.setClient(httpClientPolicy);
        http.setTlsClientParameters(tlsClientParameters);

        // We get redirected from Gordy, to Bethal.
        http.setTrustDecider(
                new MyHttpsTrustDecider(
                        new String[] {"Gordy", "Bethal"}));

        // Without preemptive user/pass Bethal returns a
        // 401 for realm Cronus. If we supply any name other
        // than Edward, George, or Mary, with the pass of "password"
        // we should succeed.
        http.setAuthSupplier(
                new MyBasicAuthSupplier("Cronus", "Betty", "password"));

        // We actually get our answer from Bethal at the end of the
        // redirects.
        String answer = gordy.sayHi();
        assertTrue("Unexpected answer: " + answer,
                "Bonjour from Bethal".equals(answer));

        // The loop auth supplier,
        // We should die with looping realms.
        http.setAuthSupplier(new MyBasicAuthSupplier());

        try {
            answer = gordy.sayHi();
            fail("Unexpected answer from Gordy: " + answer);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Test
    public void testUpdateAddress() throws Exception {
        startServer("Bethal");

        URL config = getClass().getResource("BethalClientConfig.cxf");

        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);
        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        updateAddressPort(bethal, getPort("PORT4"));
        verifyBethalClient(bethal);
        
        updateAddressPort(bethal, getPort("PORT6"));
        verifyBethalClient(bethal);

        // setup the feature by using JAXWS front-end API
        final Collection<Future<String>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final Random random = new Random();

        try {
            for (int  i = 0; i < 30; ++i) {
                futures.add(executor.submit(() -> {
                    if (random.nextBoolean()) {
                        updateAddressPort(bethal, getPort("PORT4"));
                    } else {
                        updateAddressPort(bethal, getPort("PORT6"));
                    }
                    return bethal.greetMe("timeout!");
                }));
            }

            for (final Future<String> f: futures) {
                assertThat(f.get(10, TimeUnit.SECONDS), equalTo("Hello timeout!"));
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testUpdateAddressNoClientReset() throws Exception {
        startServer("Bethal");

        URL config = getClass().getResource("BethalClientConfig.cxf");

        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);
        URL wsdl = getClass().getResource("greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        ((BindingProvider)bethal).getRequestContext().put("https.reset.httpclient.http.conduit", false);

        updateAddressPort(bethal, getPort("PORT4"));
        verifyBethalClient(bethal);
        
        updateAddressPort(bethal, getPort("PORT6"));
        verifyBethalClient(bethal);

        // setup the feature by using JAXWS front-end API
        final Collection<Future<String>> futures = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final Random random = new Random();

        try {
            for (int  i = 0; i < 30; ++i) {
                futures.add(executor.submit(() -> {
                    if (random.nextBoolean()) {
                        updateAddressPort(bethal, getPort("PORT4"));
                    } else {
                        updateAddressPort(bethal, getPort("PORT6"));
                    }
                    return bethal.greetMe("timeout!");
                }));
            }

            for (final Future<String> f: futures) {
                assertThat(f.get(10, TimeUnit.SECONDS), equalTo("Hello timeout!"));
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
}


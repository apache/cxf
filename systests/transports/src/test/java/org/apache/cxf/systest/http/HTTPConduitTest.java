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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.namespace.QName;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.BusApplicationContext;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HttpAuthSupplier;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.URLConnectionInfo;
import org.apache.cxf.transport.http.UntrustedURLConnectionIOException;

import org.apache.cxf.transport.https.HttpsURLConnectionInfo;

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.context.ApplicationContext;

/**
 * This class tests several issues and Conduit policies based 
 * on a set up of redirecting servers.
 * <pre>
 * 
 * Http Redirection:
 * 
 * Rethwel(http:9004) ------\
 *                           ----> Mortimer (http:9000)
 * Poltim(https:9005) ------/
 * 
 * HttpS redirection/Trust:
 * 
 * Tarpin(https:9003) ----> Gordy(https:9001) ----> Bethal(https:9002)
 * 
 * Redirect Loop:
 * 
 * Hurlon (http:9006) ----> Abost(http:9007) ----\
 *   ^                                            |
 *   |-------------------------------------------/
 * 
 * Hostname Verifier Test
 * 
 * Morpit (https:9008)
 * 
 * </pre>
 * The Bethal server issues 401 with differing realms depending on the
 * User name given in the authorization header.
 * <p>
 * The Morpit has a CN that is not equal to "localhost" to kick in
 * the Hostname Verifier.
 */
public class HTTPConduitTest extends AbstractBusClientServerTestBase {
    public static final String PORT0 = BusServer.PORT0;
    public static final String PORT1 = BusServer.PORT1;
    public static final String PORT2 = BusServer.PORT2;
    public static final String PORT3 = BusServer.PORT3;
    public static final String PORT4 = BusServer.PORT4;
    public static final String PORT5 = BusServer.PORT5;
    public static final String PORT6 = BusServer.PORT6;
    public static final String PORT7 = BusServer.PORT7;
    public static final String PORT8 = BusServer.PORT8;

    private static final boolean IN_PROCESS = true;
    
    private static TLSClientParameters tlsClientParameters = new TLSClientParameters();
    private static Map<String, String> addrMap = new TreeMap<String, String>();
    private static List<String> servers = new ArrayList<String>();

    static {
        addrMap.put("Mortimer", "http://localhost:" + PORT0 + "/");
        addrMap.put("Tarpin",   "https://localhost:" + PORT3 + "/");
        addrMap.put("Rethwel",  "http://localhost:" + PORT4 + "/");
        addrMap.put("Poltim",   "https://localhost:" + PORT5 + "/");
        addrMap.put("Gordy",    "https://localhost:" + PORT1 + "/");
        addrMap.put("Bethal",   "https://localhost:" + PORT2 + "/");
        addrMap.put("Abost",    "http://localhost:" + PORT7 + "/");
        addrMap.put("Hurlon",   "http://localhost:" + PORT6 + "/");
        addrMap.put("Morpit",   "https://localhost:" + PORT8 + "/");
        tlsClientParameters.setDisableCNCheck(true);
    }
    
    static {
        try {
            //System.setProperty("javax.net.debug", "all");
            String keystore = 
                new File(Server.class.getResource("resources/Morpit.jks").toURI()).getAbsolutePath();
            //System.out.println("Keystore: " + keystore);
            KeyManager[] kmgrs = getKeyManagers(getKeyStore("JKS", keystore, "password"), "password");
            
            String truststore = 
                new File(Server.class.getResource("resources/Truststore.jks").toURI()).getAbsolutePath();
            //System.out.println("Truststore: " + truststore);
            TrustManager[] tmgrs = getTrustManagers(getKeyStore("JKS", truststore, "password"));
            
            tlsClientParameters.setKeyManagers(kmgrs);
            tlsClientParameters.setTrustManagers(tmgrs);
            FiltersType filters = new FiltersType();
            filters.getInclude().add(".*_EXPORT_.*");
            filters.getInclude().add(".*_EXPORT1024_.*");
            filters.getInclude().add(".*_WITH_DES_.*");
            filters.getInclude().add(".*_WITH_NULL_.*");
            filters.getInclude().add(".*_DH_anon_.*");
            tlsClientParameters.setCipherSuitesFilter(filters);
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
    private final QName rethwelQ = 
        new QName("http://apache.org/hello_world", "Rethwel");
    private final QName mortimerQ = 
        new QName("http://apache.org/hello_world", "Mortimer");
    private final QName poltimQ = 
        new QName("http://apache.org/hello_world", "Poltim");
    private final QName hurlonQ = 
        new QName("http://apache.org/hello_world", "Hurlon");
    // PMD Violation because it is not used, but 
    // it is here for completeness.
    //private final QName abostQ = 
        //new QName("http://apache.org/hello_world", "Abost");
    public HTTPConduitTest() {
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
    public static synchronized boolean startServer(String name) {
        if (servers.contains(name)) {
            return true;
        }
        URL serverC =
            Server.class.getResource("resources/" + name + ".cxf");
        boolean server = launchServer(Server.class, null,
                new String[] { 
                    name, 
                    addrMap.get(name),
                    serverC.toString() }, 
                IN_PROCESS);
        if (server) {
            servers.add(name);
        }
        return server;
    }
    
    @BeforeClass
    public static void setProps() {
        // TODO: Do I need this?
        System.setProperty("org.apache.cxf.bus.factory", 
            "org.apache.cxf.bus.CXFBusFactory");
    }
        
    public static KeyStore getKeyStore(String ksType, String file, String ksPassword)
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
        
        keyStore.load(new FileInputStream(file), password);
        
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
    
    
    @Test
    public void testBasicConnection() throws Exception {
        startServer("Mortimer");
        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);
        updateAddressPort(mortimer, PORT0);

        String answer = mortimer.sayHi();
        assertTrue("Unexpected answer: " + answer, 
                "Bonjour from Mortimer".equals(answer));
    }

    /**
     * This methods tests that a conduit that is not configured
     * to follow redirects will not. The default is not to 
     * follow redirects. 
     * Rethwel redirects to Mortimer.
     * 
     * Note: Unfortunately, the invocation will 
     * "fail" for any number of other reasons.
     * 
     */
    @Test
    public void testHttp2HttpRedirectFail() throws Exception {
        startServer("Mortimer");
        startServer("Rethwel");

        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter rethwel = service.getPort(rethwelQ, Greeter.class);
        assertNotNull("Port is null", rethwel);
        updateAddressPort(rethwel, PORT5);

        String answer = null;
        try {
            answer = rethwel.sayHi();
            fail("Redirect didn't fail. Got answer: " + answer);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        
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

        URL config = getClass().getResource("resources/Http2HttpRedirect.cxf");
    
        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);
        
        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter rethwel = service.getPort(rethwelQ, Greeter.class);
        updateAddressPort(rethwel, PORT4);
        assertNotNull("Port is null", rethwel);
        
        String answer = rethwel.sayHi();
        assertTrue("Unexpected answer: " + answer, 
                "Bonjour from Mortimer".equals(answer));
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

        URL config = getClass().getResource(
                    "resources/Http2HttpLoopRedirectFail.cxf");
        
        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);
        
        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter hurlon = service.getPort(hurlonQ, Greeter.class);
        assertNotNull("Port is null", hurlon);
        updateAddressPort(hurlon, PORT6);
        
        String answer = null;
        try {
            answer = hurlon.sayHi();
            fail("Redirect didn't fail. Got answer: " + answer);
        } catch (Exception e) {
            // This exception will be one of not being able to
            // read from the StreamReader
            //e.printStackTrace();
        }
        
    }
    /**
     * This methods tests a basic https connection to Bethal.
     * It supplies an authorization policy with premetive user/pass
     * to avoid the 401.
     */
    @Test
    public void testHttpsBasicConnectionWithConfig() throws Exception {
        startServer("Bethal");

        URL config = getClass().getResource(
                    "resources/BethalClientConfig.cxf");
        
        // We go through the back door, setting the default bus.
        new DefaultBusFactory().createBus(config);
        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        assertNotNull("Port is null", bethal);
        updateAddressPort(bethal, PORT2);
        verifyBethalClient(bethal);        
    }
    
    @Test
    public void testGetClientFromSpringContext() throws Exception {
        startServer("Bethal");        
        
        BusFactory.setDefaultBus(null);
        // The client bean configuration file
        URL beans = getClass().getResource("resources/BethalClientBeans.xml");
        // We go through the back door, setting the default bus.
        Bus bus = new DefaultBusFactory().createBus(beans);
        
        ApplicationContext context = bus.getExtension(BusApplicationContext.class);
        Greeter bethal = (Greeter)context.getBean("Bethal");        
        updateAddressPort(bethal, PORT2);
        // verify the client side's setting
        verifyBethalClient(bethal);         
    }
    
    
    
    // we just verify the configurations are loaded successfully
    private void verifyBethalClient(Greeter bethal) {
        Client client = ClientProxy.getClient(bethal);
        HTTPConduit http = 
            (HTTPConduit) client.getConduit();
        
        HTTPClientPolicy httpClientPolicy = http.getClient();
        assertEquals("the httpClientPolicy's autoRedirect should be true",
                     true, httpClientPolicy.isAutoRedirect());
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
        String answer = bethal.sayHi();
        assertTrue("Unexpected answer: " + answer, 
                "Bonjour from Bethal".equals(answer));
    }
    
    /**
     * This methods tests a basic https connection to Bethal.
     * It supplies an authorization policy with premetive user/pass
     * to avoid the 401.
     */
    @Test
    public void testHttpsBasicConnection() throws Exception {
        startServer("Bethal");

        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        assertNotNull("Port is null", bethal);
        updateAddressPort(bethal, PORT2);
        
        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(bethal);
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
        
        String answer = bethal.sayHi();
        assertTrue("Unexpected answer: " + answer, 
                "Bonjour from Bethal".equals(answer));
    }
    

    @Test
    public void testHttpsRedirectToHttpFail() throws Exception {
        startServer("Mortimer");
        startServer("Poltim");

        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter poltim = service.getPort(poltimQ, Greeter.class);
        assertNotNull("Port is null", poltim);
        updateAddressPort(poltim, PORT5);

        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(poltim);
        HTTPConduit http = 
            (HTTPConduit) client.getConduit();
        
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        
        httpClientPolicy.setAutoRedirect(true);
        
        http.setClient(httpClientPolicy);
        http.setTlsClientParameters(tlsClientParameters);
        poltim.sayHi();
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
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < trustName.length; i++) {
                sb.append("\"OU=");
                sb.append(trustName[i]);
                sb.append("\"");
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

        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter bethal = service.getPort(bethalQ, Greeter.class);
        assertNotNull("Port is null", bethal);
        updateAddressPort(bethal, PORT2);
        
        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(bethal);
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
        
        String answer = bethal.sayHi();
        assertTrue("Unexpected answer: " + answer, 
                "Bonjour from Bethal".equals(answer));
        
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
    }

    @Test
    public void testHttpsTrustRedirect() throws Exception {
        startServer("Tarpin");
        startServer("Gordy");
        startServer("Bethal");

        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter tarpin = service.getPort(tarpinQ, Greeter.class);
        assertNotNull("Port is null", tarpin);
        updateAddressPort(tarpin, PORT3);
        
        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(tarpin);
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
        String answer = tarpin.sayHi();
        
        assertTrue("Trust Decider wasn't called correctly", 
                       3 == trustDecider.wasCalled());
        assertTrue("Unexpected answer: " + answer, 
                "Bonjour from Bethal".equals(answer));
        
        // Limit the redirects to 1, since there are two, this should fail.
        http.getClient().setMaxRetransmits(1);

        try {
            answer = tarpin.sayHi();
            fail("Unexpected answer from Tarpin: " + answer);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        
        // Set back to unlimited.
        http.getClient().setMaxRetransmits(-1);
        
        // Effectively we will not trust Gordy in the middle.
        trustDecider = 
                new MyHttpsTrustDecider(
                    new String[] {"Tarpin", "Bethal"});
        http.setTrustDecider(trustDecider);
        
        try {
            answer = tarpin.sayHi();
            fail("Unexpected answer from Tarpin: " + answer);
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue("Trust Decider wasn't called correctly",
                     2 == trustDecider.wasCalled());
        }
        
    }

    public class MyBasicAuthSupplier extends HttpAuthSupplier {

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
            user  = u;
            pass  = p;
        }
        @Override
        public String getPreemptiveAuthorization(
                HTTPConduit  conduit,
                URL     currentURL,
                Message message
        ) {
            return null;
        }

        /**
         * If we don't have the realm set, then we loop
         * through the realms.
         */
        @Override
        public String getAuthorizationForRealm(
                HTTPConduit  conduit, 
                URL     currentURL,
                Message message, 
                String  reqestedRealm,
                String fullHeader
        ) {
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
            String userpass = usr + ":" + pwd;
            String token = Base64Utility.encode(userpass.getBytes());
            return "Basic " + token;
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

        URL wsdl = getClass().getResource("resources/greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter gordy = service.getPort(gordyQ, Greeter.class);
        assertNotNull("Port is null", gordy);
        updateAddressPort(gordy, PORT1);
        
        // Okay, I'm sick of configuration files.
        // This also tests dynamic configuration of the conduit.
        Client client = ClientProxy.getClient(gordy);
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
    
}


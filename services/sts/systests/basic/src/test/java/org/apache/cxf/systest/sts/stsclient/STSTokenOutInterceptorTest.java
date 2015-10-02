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
package org.apache.cxf.systest.sts.stsclient;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenOutInterceptor;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenOutInterceptor.AuthMode;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenOutInterceptor.AuthParams;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Some tests for STSClient configuration.
 */
public class STSTokenOutInterceptorTest extends AbstractBusClientServerTestBase {    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);
   
    private static final String STS_SERVICE_NAME = 
        "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService";
    private static final String TOKEN_TYPE_SAML_2_0 = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

    private static final String SERVICE_ENDPOINT_ASSYMETRIC =
        "http://localhost:8081/doubleit/services/doubleitasymmetric";
    private static final String STS_X509_WSDL_LOCATION_RELATIVE = "/SecurityTokenService/X509?wsdl";
    private static final String STS_X509_ENDPOINT_NAME = "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}X509_Port";
    private static final String KEY_TYPE_X509 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";

    private static final String SERVICE_ENDPOINT_TRANSPORT =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";
    private static final String STS_TRANSPORT_WSDL_LOCATION_RELATIVE = "/SecurityTokenService/Transport?wsdl";
    private static final String STS_TRANSPORT_ENDPOINT_NAME = 
        "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port";

    private static final String CLIENTSTORE = "/clientstore.jks";
    private static final String KEYSTORE_PASS = "cspass";
    private static final String KEY_PASS = "ckpass";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @Test
    public void testBasicAsymmetricBinding() throws Exception {
        Bus bus = BusFactory.getThreadDefaultBus();        
        
        AuthParams authParams = new AuthParams(
                 AuthMode.X509, 
                 null,
                 "org.apache.cxf.systest.sts.common.CommonCallbackHandler",
                 "mystskey",
                 "clientKeystore.properties");
        
        STSTokenOutInterceptor interceptor = new STSTokenOutInterceptor(
                 authParams,
                 "http://localhost:" + STSPORT2 + STS_X509_WSDL_LOCATION_RELATIVE,
                 bus);
        
        MessageImpl message = prepareMessage(bus, null, SERVICE_ENDPOINT_ASSYMETRIC);
        
        interceptor.handleMessage(message);
        
        SecurityToken token = (SecurityToken)message.getExchange().get(SecurityConstants.TOKEN);
        validateSecurityToken(token);
    }

    @Test
    public void testBasicTransportBinding() throws Exception {
        // Setup HttpsURLConnection to get STS WSDL
        configureDefaultHttpsConnection();
        
        Bus bus = BusFactory.getThreadDefaultBus();  
        AuthParams authParams = new AuthParams(
                   AuthMode.TRANSPORT, 
                   "alice",
                   "org.apache.cxf.systest.sts.common.CommonCallbackHandler",
                   null,
                   null);
                                      
        STSTokenOutInterceptor interceptor = new STSTokenOutInterceptor(
                    authParams,
                    "https://localhost:" + STSPORT + STS_TRANSPORT_WSDL_LOCATION_RELATIVE,
                    bus);
        
        TLSClientParameters tlsParams = prepareTLSParams();
        STSClient stsClient = interceptor.getSTSClient();
        ((HTTPConduit)stsClient.getClient().getConduit()).setTlsClientParameters(tlsParams);
        
        MessageImpl message = prepareMessage(bus, null, SERVICE_ENDPOINT_TRANSPORT); 
        
        interceptor.handleMessage(message);
        
        SecurityToken token = (SecurityToken)message.getExchange().get(SecurityConstants.TOKEN);
        validateSecurityToken(token);
    }

    @Test
    public void testSTSClientAsymmetricBinding() throws Exception {
        Bus bus = BusFactory.getThreadDefaultBus();        
        
        STSClient stsClient = initStsClientAsymmeticBinding(bus);
        STSTokenOutInterceptor interceptor = new STSTokenOutInterceptor(stsClient);
        
        MessageImpl message = prepareMessage(bus, null, SERVICE_ENDPOINT_ASSYMETRIC);
        
        interceptor.handleMessage(message);
        
        SecurityToken token = (SecurityToken)message.getExchange().get(SecurityConstants.TOKEN);
        validateSecurityToken(token);
    }

    @Test
    public void testSTSClientTransportBinding() throws Exception {
        // Setup HttpsURLConnection to get STS WSDL
        configureDefaultHttpsConnection();
        
        Bus bus = BusFactory.getThreadDefaultBus();  
        STSClient stsClient = initStsClientTransportBinding(bus);
                                      
        STSTokenOutInterceptor interceptor = new STSTokenOutInterceptor(stsClient);
        
        TLSClientParameters tlsParams = prepareTLSParams();
        ((HTTPConduit)stsClient.getClient().getConduit()).setTlsClientParameters(tlsParams);
        
        MessageImpl message = prepareMessage(bus, null, SERVICE_ENDPOINT_TRANSPORT); 
        
        interceptor.handleMessage(message);
        
        SecurityToken token = (SecurityToken)message.getExchange().get(SecurityConstants.TOKEN);
        validateSecurityToken(token);
    }

    private STSClient initStsClientAsymmeticBinding(Bus bus) {
        bus.getInInterceptors().add(new LoggingOutInterceptor());
        bus.getOutInterceptors().add(new LoggingInInterceptor());
        bus.getOutFaultInterceptors().add(new LoggingInInterceptor());

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("http://localhost:" + STSPORT2 + STS_X509_WSDL_LOCATION_RELATIVE);
        stsClient.setServiceName(STS_SERVICE_NAME);
        stsClient.setEndpointName(STS_X509_ENDPOINT_NAME);
        stsClient.setTokenType(TOKEN_TYPE_SAML_2_0);
        stsClient.setKeyType(KEY_TYPE_X509);
        stsClient.setAllowRenewingAfterExpiry(true);
        stsClient.setEnableLifetime(true);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        props.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");
        props.put(SecurityConstants.ENCRYPT_PROPERTIES, "clientKeystore.properties");
        props.put(SecurityConstants.SIGNATURE_PROPERTIES, "clientKeystore.properties");
        props.put(SecurityConstants.STS_TOKEN_USERNAME, "mystskey");
        props.put(SecurityConstants.STS_TOKEN_PROPERTIES, "clientKeystore.properties");
        props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");
        stsClient.setProperties(props);
        return stsClient;
    }

    private STSClient initStsClientTransportBinding(Bus bus) {
        bus.getInInterceptors().add(new LoggingOutInterceptor());
        bus.getOutInterceptors().add(new LoggingInInterceptor());
        bus.getOutFaultInterceptors().add(new LoggingInInterceptor());

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("https://localhost:" + STSPORT + STS_TRANSPORT_WSDL_LOCATION_RELATIVE);
        stsClient.setServiceName(STS_SERVICE_NAME);
        stsClient.setEndpointName(STS_TRANSPORT_ENDPOINT_NAME);
        stsClient.setTokenType(TOKEN_TYPE_SAML_2_0);
        stsClient.setAllowRenewingAfterExpiry(true);
        stsClient.setEnableLifetime(true);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConstants.USERNAME, "alice");
        props.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        stsClient.setProperties(props);
        return stsClient;
    }

    private MessageImpl prepareMessage(Bus bus, STSClient stsClient, String serviceAddress) throws EndpointException {
        MessageImpl message = new MessageImpl();
        message.put(SecurityConstants.STS_CLIENT, stsClient);
        message.put(Message.ENDPOINT_ADDRESS, serviceAddress);
        
        Exchange exchange = new ExchangeImpl();
        ServiceInfo si = new ServiceInfo();
        Service s = new ServiceImpl(si);
        EndpointInfo ei = new EndpointInfo();
        Endpoint ep = new EndpointImpl(bus, s, ei);
        ei.setBinding(new BindingInfo(si, null));
        message.setExchange(exchange);
        exchange.put(Endpoint.class, ep);
        return message;
    }

    private void configureDefaultHttpsConnection() throws NoSuchAlgorithmException, KeyStoreException,
        CertificateException, IOException, KeyManagementException {
        // For localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {

            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                return "localhost".equals(hostname);
            }
        });

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory
            .getDefaultAlgorithm());
        KeyStore keyStore = loadClientKeystore();
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustManagers, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
        // Needed to prevent test failure using IBM JDK 
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            System.setProperty("https.protocols", "TLSv1");
        }
    }

    private TLSClientParameters prepareTLSParams() throws KeyStoreException, IOException,
        NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setDisableCNCheck(true);
        KeyStore trustStore = loadClientKeystore();

        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory
            .getDefaultAlgorithm());
        trustFactory.init(trustStore);
        TrustManager[] tm = trustFactory.getTrustManagers();
        tlsParams.setTrustManagers(tm);

        KeyStore keyStore = loadClientKeystore();
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(keyStore, KEY_PASS.toCharArray());
        KeyManager[] km = keyFactory.getKeyManagers();
        tlsParams.setKeyManagers(km);
        return tlsParams;
    }

    private KeyStore loadClientKeystore() throws KeyStoreException, IOException, NoSuchAlgorithmException,
        CertificateException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream keystoreStream = STSTokenOutInterceptorTest.class.getResourceAsStream(CLIENTSTORE);
        try {
            keystore.load(keystoreStream, KEYSTORE_PASS.toCharArray());
        } finally {
            keystoreStream.close();
        }
        return keystore;
    }

    private void validateSecurityToken(SecurityToken token) {
        Assert.assertNotNull(token);
        Assert.assertEquals(TOKEN_TYPE_SAML_2_0, token.getTokenType());
        Assert.assertNotNull(token.getId());
        Assert.assertTrue(token.getExpires().after(new Date()));
        Assert.assertEquals("Assertion", token.getToken().getLocalName());
    }

}

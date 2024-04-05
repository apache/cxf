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
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.systest.sts.TLSClientParametersUtils;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.transport.https.SSLUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractSTSTokenTest extends AbstractClientServerTestBase {
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    static final String SERVICE_ENDPOINT_ASSYMETRIC =
        "http://localhost:8081/doubleit/services/doubleitasymmetric";
    static final String STS_X509_WSDL_LOCATION_RELATIVE = "/SecurityTokenService/X509?wsdl";
    static final String STS_X509_ENDPOINT_NAME = "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}X509_Port";
    static final String KEY_TYPE_X509 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";

    static final String SERVICE_ENDPOINT_TRANSPORT =
        "https://localhost:8081/doubleit/services/doubleittransportsaml1";
    static final String STS_TRANSPORT_WSDL_LOCATION_RELATIVE = "/SecurityTokenService/Transport?wsdl";
    static final String STS_TRANSPORT_ENDPOINT_NAME =
        "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port";

    private static final String STS_SERVICE_NAME =
            "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService";
    private static final String TOKEN_TYPE_SAML_2_0 =
            "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new STSServer(
            "cxf-transport.xml",
            "cxf-x509.xml"
        )));
    }

    static STSClient initStsClientAsymmeticBinding(Bus bus) {
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

        Map<String, Object> props = new HashMap<>();
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

    static STSClient initStsClientTransportBinding(Bus bus) {
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

        Map<String, Object> props = new HashMap<>();
        props.put(SecurityConstants.USERNAME, "alice");
        props.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        stsClient.setProperties(props);
        return stsClient;
    }

    static MessageImpl prepareMessage(Bus bus, STSClient stsClient, String serviceAddress) throws EndpointException {
        MessageImpl message = new MessageImpl();
        message.put(SecurityConstants.STS_CLIENT, stsClient);
        message.put(Message.ENDPOINT_ADDRESS, serviceAddress);

        Exchange exchange = new ExchangeImpl();
        ServiceInfo si = new ServiceInfo();
        si.setName(new QName("http://www.apache.org", "ServiceName"));
        Service s = new ServiceImpl(si);
        EndpointInfo ei = new EndpointInfo();
        ei.setName(new QName("http://www.apache.org", "EndpointName"));
        Endpoint ep = new EndpointImpl(bus, s, ei);
        ei.setBinding(new BindingInfo(si, null));
        message.setExchange(exchange);
        exchange.put(Endpoint.class, ep);
        return message;
    }

    static void configureDefaultHttpsConnection() throws GeneralSecurityException, IOException {
        // For localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {

            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                return "localhost".equals(hostname);
            }
        });

        SSLContext sc = SSLUtils.getSSLContext(TLSClientParametersUtils.getTLSClientParameters());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    static void validateSecurityToken(SecurityToken token) {
        assertNotNull(token);
        assertEquals(TOKEN_TYPE_SAML_2_0, token.getTokenType());
        assertNotNull(token.getId());
        assertTrue(token.getExpires().isAfter(Instant.now()));
        assertEquals("Assertion", token.getToken().getLocalName());
    }

}

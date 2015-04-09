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

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Endpoint;
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
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenHelper;
import org.apache.cxf.ws.security.trust.STSClient;
import org.junit.BeforeClass;

/**
 * Some tests for STSClient configuration.
 */
public class STSTokenHelperTest extends AbstractBusClientServerTestBase {    
    static final String STSPORT = allocatePort(STSServer.class);
    static final String STSPORT2 = allocatePort(STSServer.class, 2);

    private static final String STS_WSDL_LOCATION_RELATIVE = "/SecurityTokenService/X509?wsdl";
    private static final String STS_SERVICE_NAME = 
        "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService";
    private static final String STS_X509_ENDPOINT_NAME = "{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}X509_Port";
    private static final String TOKEN_TYPE_SAML_2_0 = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String KEY_TYPE_X509 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey";
    private static final String SERVICE_ENDPOINT_ASSYMETRIC = 
        "http://localhost:1111/doubleit/services/doubleitasymmetric";    
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(STSServer.class, true)
        );
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        stopAllServers();
    }

    @org.junit.Test
    public void testSTSAssymetric() throws Exception {
        Bus bus = BusFactory.newInstance().createBus();        
        STSClient stsClient = initStsClient(bus);
        
        MessageImpl message = new MessageImpl();
        message.put(SecurityConstants.STS_CLIENT, stsClient);
        message.put(Message.ENDPOINT_ADDRESS, SERVICE_ENDPOINT_ASSYMETRIC);
        
        Exchange exchange = new ExchangeImpl();
        ServiceInfo si = new ServiceInfo();
        Service s = new ServiceImpl(si);
        EndpointInfo ei = new EndpointInfo();
        Endpoint ep = new EndpointImpl(bus, s, ei);
        ei.setBinding(new BindingInfo(si, null));
        message.setExchange(exchange);
        exchange.put(Endpoint.class, ep);
        
        STSTokenHelper.TokenRequestParams params = new STSTokenHelper.TokenRequestParams();
        STSTokenHelper.getToken(message, params);
    }

    private STSClient initStsClient(Bus bus) {
        bus.getInInterceptors().add(new LoggingOutInterceptor());
        bus.getOutInterceptors().add(new LoggingInInterceptor());
        bus.getOutFaultInterceptors().add(new LoggingInInterceptor());

        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("http://localhost:" + STSPORT2 + STS_WSDL_LOCATION_RELATIVE);
        stsClient.setServiceName(STS_SERVICE_NAME);
        stsClient.setEndpointName(STS_X509_ENDPOINT_NAME);
        stsClient.setTokenType(TOKEN_TYPE_SAML_2_0);
        stsClient.setKeyType(KEY_TYPE_X509);
        stsClient.setAllowRenewingAfterExpiry(true);
        stsClient.setEnableLifetime(true);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SecurityConstants.USERNAME, "alice");
        props.put(SecurityConstants.CALLBACK_HANDLER, "org.apache.cxf.systest.sts.common.CommonCallbackHandler");
        props.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");
        props.put(SecurityConstants.ENCRYPT_PROPERTIES, "clientKeystore.properties");
        props.put(SecurityConstants.SIGNATURE_PROPERTIES, "clientKeystore.properties");
        props.put(SecurityConstants.STS_TOKEN_USERNAME, "mystskey");
        props.put(SecurityConstants.STS_TOKEN_PROPERTIES, "clientKeystore.properties");
        props.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, "true");
        props.put(SecurityConstants.IS_BSP_COMPLIANT, "false");
        stsClient.setProperties(props);
        return stsClient;
    }
}

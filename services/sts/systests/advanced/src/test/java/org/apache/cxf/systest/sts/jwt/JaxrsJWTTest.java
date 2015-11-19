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
package org.apache.cxf.systest.sts.jwt;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.systest.sts.common.SecurityTestUtil;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenOutInterceptor;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSTokenRetriever.TokenRequestParams;
import org.junit.BeforeClass;

/**
 * In this test case, a CXF JAX-RS client gets a JWT token from the STS + sends it to the
 * service provider, which validates it.
 */
public class JaxrsJWTTest extends AbstractBusClientServerTestBase {
    
    public static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    static final String STSPORT = allocatePort(STSServer.class);

    private static final String PORT = allocatePort(Server.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
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
    public void testSuccessfulInvocation() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = JaxrsJWTTest.class.getResource("cxf-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        SpringBusFactory.setDefaultBus(bus);
        SpringBusFactory.setThreadDefaultBus(bus);

        final String address = "https://localhost:" + PORT + "/doubleit/services/doubleit-rs";
        final int numToDouble = 25;  
       
        List<Object> providers = Collections.singletonList(new JwtOutFilter());
        
        WebClient client = WebClient.create(address, providers);
        client.type("text/plain").accept("text/plain");
        
        STSClient stsClient = getSTSClient(JWT_TOKEN_TYPE, bus);
        STSTokenOutInterceptor stsInterceptor = 
            new STSTokenOutInterceptor(Phase.PRE_LOGICAL, stsClient, new TokenRequestParams());
        stsInterceptor.getBefore().add(JwtOutFilter.class.getName());
        WebClient.getConfig(client).getOutInterceptors().add(stsInterceptor);
        
        int resp = client.post(numToDouble, Integer.class);
        org.junit.Assert.assertEquals(2 * numToDouble, resp);
        
        bus.shutdown(true);
    }
    
    private STSClient getSTSClient(
        String tokenType, Bus bus
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, 
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setSendKeyType(false);

        return stsClient;
    }
    
    private static class JwtOutFilter implements ClientRequestFilter {
        
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            SecurityToken token = 
                (SecurityToken)requestContext.getProperty(SecurityConstants.TOKEN);
            if (token == null) {
                Message m = PhaseInterceptorChain.getCurrentMessage();
                token = (SecurityToken)m.getContextualProperty(SecurityConstants.TOKEN);
            }
            
            if (token != null && token.getToken() != null) {
                requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, 
                                                      "JWT" + " " + token.getToken().getTextContent());
            }
        }
    }
}

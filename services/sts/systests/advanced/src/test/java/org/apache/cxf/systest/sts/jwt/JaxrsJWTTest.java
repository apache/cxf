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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.systest.sts.deployment.DoubleItServer;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenOutInterceptor;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSTokenRetriever.TokenRequestParams;

import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

/**
 * In this test case, a CXF JAX-RS client gets a JWT token from the STS + sends it to the
 * service provider, which validates it.
 */
public class JaxrsJWTTest extends AbstractBusClientServerTestBase {

    public static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    static final String STSPORT = allocatePort(STSServer.class);

    private static final String PORT = allocatePort(DoubleItServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new DoubleItServer(
            JaxrsJWTTest.class.getResource("cxf-service.xml"))));
        assertTrue(launchServer(new STSServer()));
    }

    @org.junit.Test
    public void testSuccessfulInvocation() throws Exception {
        createBus(getClass().getResource("cxf-client.xml").toString());

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
    }

    private STSClient getSTSClient(
        String tokenType, Bus bus
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);
        String port = STSPORT;

        stsClient.setWsdlLocation("https://localhost:" + port + "/SecurityTokenService/Transport?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}Transport_Port");

        Map<String, Object> properties = new HashMap<>();
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

    private static final class JwtOutFilter implements ClientRequestFilter {

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
                                                      "Bearer" + " " + token.getToken().getTextContent());
            }
        }
    }
}

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
package org.apache.cxf.systest.sts.itests.unit;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.systest.sts.itests.BasicSTSIntegrationTest;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.SAMLTokenProcessor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Some tests to retrieve a SAML token directly from the STS.
 */
@RunWith(PaxExam.class)
public class STSUnitTest extends BasicSTSIntegrationTest {

    private static final String SAML2_TOKEN_TYPE =
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String BEARER_KEYTYPE =
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";

    @Test
    public void testBearerSAML2Token() throws URISyntaxException, Exception {
        Bus bus = BusFactory.getDefaultBus();
        String stsEndpoint = "http://localhost:" 
            + System.getProperty("BasicSTSIntegrationTest.PORT")
            + "/cxf/X509";

        //sts could take a second or two to fully startup, make sure we can get the wsdl
        waitForWSDL(stsEndpoint);

        // Get a token
        SecurityToken token =
            requestSecurityToken(SAML2_TOKEN_TYPE, BEARER_KEYTYPE, bus, stsEndpoint);
        assertEquals(SAML2_TOKEN_TYPE, token.getTokenType());
        assertNotNull(token.getToken());

        // Process the token
        List<WSSecurityEngineResult> results = processToken(token);

        assertTrue(results != null && results.size() == 1);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)results.get(0).get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
        assertNotNull(assertion);
        assertTrue(assertion.getSaml1() == null && assertion.getSaml2() != null);
        assertTrue(assertion.isSigned());

        List<String> methods = assertion.getConfirmationMethods();
        String confirmMethod = null;
        if (methods != null && !methods.isEmpty()) {
            confirmMethod = methods.get(0);
        }
        assertTrue(confirmMethod.contains("bearer"));

        bus.shutdown(true);
    }

    private static void waitForWSDL(String loc) throws Exception {
        final URL url = new URL(loc + "?wsdl");
        for (int x = 0; x < 10; x++) {
            try (InputStream is = url.openStream()) {
                return;
            } catch (Exception e) {
                Thread.sleep(100L);
            }
        }
        System.out.println("WARN: wsdl still unavailable!");
    }

    private SecurityToken requestSecurityToken(
        String tokenType,
        String keyType,
        Bus bus,
        String endpointAddress
    ) throws Exception {
        STSClient stsClient = new STSClient(bus);

        stsClient.setWsdlLocation(endpointAddress + "?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}X509_Port");
        stsClient.setEnableAppliesTo(false);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER, new CommonCallbackHandler()
        );
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "myclientkey");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES, "clientKeystore.properties");
        properties.put(SecurityConstants.ENCRYPT_USERNAME, "mystskey");
        properties.put(SecurityConstants.ENCRYPT_PROPERTIES, "clientKeystore.properties");

        stsClient.setProperties(properties);
        stsClient.setTokenType(tokenType);
        stsClient.setKeyType(keyType);

        return stsClient.requestSecurityToken(endpointAddress);
    }

    private List<WSSecurityEngineResult> processToken(SecurityToken token) throws Exception {
        RequestData requestData = new RequestData();
        CallbackHandler callbackHandler = new CommonCallbackHandler();
        requestData.setCallbackHandler(callbackHandler);
        Crypto crypto = CryptoFactory.getInstance("clientKeystore.properties",
                                                  this.getClass().getClassLoader());
        requestData.setSigVerCrypto(crypto);
        requestData.setWsDocInfo(new WSDocInfo(token.getToken().getOwnerDocument()));

        Processor processor = new SAMLTokenProcessor();
        return processor.handleToken(token.getToken(), requestData);
    }

}

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
package org.apache.cxf.systest.sts.asymmetric_encr;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.systest.sts.common.TestParam;
import org.apache.cxf.systest.sts.deployment.STSServer;
import org.apache.cxf.systest.sts.deployment.StaxSTSServer;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * In this test, a CXF client gets a token from the STS over the Asymmetric Binding. The STS is configured
 * to encrypt the issued token, using the certificate obtained from the received signature.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class AsymmetricEncryptionTest extends AbstractBusClientServerTestBase {

    static final String STSPORT = allocatePort(STSServer.class);
    static final String STAX_STSPORT = allocatePort(StaxSTSServer.class);

    final TestParam test;

    public AsymmetricEncryptionTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(launchServer(new STSServer(
            AsymmetricEncryptionTest.class.getResource("cxf-sts.xml"),
            AsymmetricEncryptionTest.class.getResource("stax-cxf-sts.xml"))));
    }

    @Parameters(name = "{0}")
    public static TestParam[] data() {
        return new TestParam[] {new TestParam("", false, STSPORT),
                                new TestParam("", false, STAX_STSPORT),
        };
    }

    @org.junit.Test
    public void testEncryptedToken() throws Exception {
        createBus();

        SecurityToken token = requestSecurityToken(getBus(), test.getStsPort());
        assertNotNull(token);
    }

    private SecurityToken requestSecurityToken(Bus bus, String stsPort) throws Exception {
        STSClient stsClient = new STSClient(bus);
        stsClient.setWsdlLocation("http://localhost:" + stsPort + "/SecurityTokenService/X509?wsdl");
        stsClient.setServiceName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}SecurityTokenService");
        stsClient.setEndpointName("{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}X509_Port");
        stsClient.setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
        stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer");

        Map<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, "alice");
        properties.put(
            SecurityConstants.CALLBACK_HANDLER,
            "org.apache.cxf.systest.sts.common.CommonCallbackHandler"
        );
        properties.put(SecurityConstants.SIGNATURE_USERNAME, "myclientkey");
        properties.put(SecurityConstants.SIGNATURE_PROPERTIES, "clientKeystore.properties");

        stsClient.setProperties(properties);
        stsClient.setAddressingNamespace("http://www.w3.org/2005/08/addressing");

        return stsClient.requestSecurityToken("https://localhost:8081/doubleit/services/doubleittransport");
    }
}

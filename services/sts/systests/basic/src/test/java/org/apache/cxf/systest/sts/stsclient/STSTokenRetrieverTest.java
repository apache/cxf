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

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.systest.sts.TLSClientParametersUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSTokenRetriever;

import org.junit.Test;

/**
 * Some tests for STSClient configuration.
 */
public class STSTokenRetrieverTest extends AbstractSTSTokenTest {

    @Test
    public void testSTSAsymmetricBinding() throws Exception {
        Bus bus = BusFactory.getThreadDefaultBus();
        STSClient stsClient = initStsClientAsymmeticBinding(bus);

        MessageImpl message = prepareMessage(bus, stsClient, SERVICE_ENDPOINT_ASSYMETRIC);
        STSTokenRetriever.TokenRequestParams params = new STSTokenRetriever.TokenRequestParams();

        SecurityToken token = STSTokenRetriever.getToken(message, params);
        validateSecurityToken(token);
    }

    @Test
    public void testSTSTransportBinding() throws Exception {
        // Setup HttpsURLConnection to get STS WSDL
        configureDefaultHttpsConnection();

        Bus bus = BusFactory.getThreadDefaultBus();
        STSClient stsClient = initStsClientTransportBinding(bus);
        stsClient.setTlsClientParameters(TLSClientParametersUtils.getTLSClientParameters());

        MessageImpl message = prepareMessage(bus, stsClient, SERVICE_ENDPOINT_TRANSPORT);
        STSTokenRetriever.TokenRequestParams params = new STSTokenRetriever.TokenRequestParams();

        SecurityToken token = STSTokenRetriever.getToken(message, params);
        validateSecurityToken(token);
    }

}

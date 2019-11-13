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
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.interceptors.STSTokenOutInterceptor;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSAuthParams;
import org.apache.cxf.ws.security.trust.STSAuthParams.AuthMode;
import org.apache.cxf.ws.security.trust.STSClient;

import org.junit.Test;

/**
 * Some tests for STSClient configuration.
 */
public class STSTokenOutInterceptorTest extends AbstractSTSTokenTest {

    @Test
    public void testBasicAsymmetricBinding() throws Exception {
        Bus bus = BusFactory.getThreadDefaultBus();

        STSAuthParams authParams = new STSAuthParams(
                 AuthMode.X509_ASSYMETRIC,
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
        STSAuthParams authParams = new STSAuthParams(
                   AuthMode.UT_TRANSPORT,
                   "alice",
                   "org.apache.cxf.systest.sts.common.CommonCallbackHandler",
                   null,
                   null);

        STSTokenOutInterceptor interceptor = new STSTokenOutInterceptor(
                    authParams,
                    "https://localhost:" + STSPORT + STS_TRANSPORT_WSDL_LOCATION_RELATIVE,
                    bus);

        interceptor.getSTSClient().setTlsClientParameters(TLSClientParametersUtils.getTLSClientParameters());

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
        stsClient.setTlsClientParameters(TLSClientParametersUtils.getTLSClientParameters());

        STSTokenOutInterceptor interceptor = new STSTokenOutInterceptor(stsClient);

        MessageImpl message = prepareMessage(bus, null, SERVICE_ENDPOINT_TRANSPORT);

        interceptor.handleMessage(message);

        SecurityToken token = (SecurityToken)message.getExchange().get(SecurityConstants.TOKEN);
        validateSecurityToken(token);
    }

}

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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.DefaultSTSTokenCacher;
import org.apache.cxf.ws.security.trust.STSAuthParams;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSTokenCacher;
import org.apache.cxf.ws.security.trust.STSTokenRetriever;
import org.apache.cxf.ws.security.trust.STSTokenRetriever.TokenRequestParams;
import org.apache.cxf.ws.security.trust.STSUtils;

public class STSTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(STSTokenOutInterceptor.class);

    private STSClient stsClient;
    private TokenRequestParams tokenParams;
    private STSTokenCacher tokenCacher = new DefaultSTSTokenCacher();

    public STSTokenOutInterceptor(STSAuthParams authParams, String stsWsdlLocation, Bus bus) {
        this(Phase.PREPARE_SEND, authParams, stsWsdlLocation, bus);
    }

    public STSTokenOutInterceptor(String phase, STSAuthParams authParams, String stsWsdlLocation, Bus bus) {
        super(phase);
        this.stsClient = STSUtils.createSTSClient(authParams, stsWsdlLocation, bus);
        this.tokenParams = new TokenRequestParams();
    }

    public STSTokenOutInterceptor(STSClient stsClient) {
        this(Phase.PREPARE_SEND, stsClient, new TokenRequestParams());
    }

    public STSTokenOutInterceptor(STSClient stsClient, TokenRequestParams tokenParams) {
        this(Phase.PREPARE_SEND, stsClient, tokenParams);
    }

    public STSTokenOutInterceptor(String phase, STSClient stsClient, TokenRequestParams tokenParams) {
        super(phase);
        this.stsClient = stsClient;
        this.tokenParams = tokenParams;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        if (stsClient != null) {
            message.put(SecurityConstants.STS_CLIENT, stsClient);
        }
        SecurityToken tok = STSTokenRetriever.getToken(message, tokenParams, tokenCacher);
        if (tok == null) {
            LOG.warning("Security token was not retrieved from STS");
        }
        processToken(message, tok);
    }

    // An extension point to allow custom processing of the token
    protected void processToken(Message message, SecurityToken tok) {

    }

    public STSClient getSTSClient() {
        return stsClient;
    }

    public STSTokenCacher getTokenCacher() {
        return tokenCacher;
    }

    public void setTokenCacher(STSTokenCacher tokenCacher) {
        this.tokenCacher = tokenCacher;
    }

}

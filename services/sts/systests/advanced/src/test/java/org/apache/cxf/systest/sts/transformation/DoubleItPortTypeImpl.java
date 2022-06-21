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
package org.apache.cxf.systest.sts.transformation;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.feature.Features;
import org.apache.cxf.helpers.CastUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(classes = org.apache.cxf.ext.logging.LoggingFeature.class)
public class DoubleItPortTypeImpl implements DoubleItPortType {

    @Resource
    WebServiceContext wsc;

    public int doubleIt(int numberToDouble) {
        //
        // Get the transformed SAML Assertion from the STS and check it
        //
        MessageContext context = wsc.getMessageContext();
        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>)context.get(WSHandlerConstants.RECV_RESULTS));
        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.UT).get(0);
        SamlAssertionWrapper assertion =
            (SamlAssertionWrapper)actionResult.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
        Assert.assertTrue(assertion != null && "DoubleItSTSIssuer".equals(assertion.getIssuerString()));

        return numberToDouble * 2;
    }

}

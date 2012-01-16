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

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import junit.framework.Assert;

import org.apache.cxf.feature.Features;
import org.apache.cxf.helpers.CastUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.util.WSSecurityUtil;
import org.example.contract.doubleit.DoubleItPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt", 
            serviceName = "DoubleItService", 
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(classes = org.apache.cxf.feature.LoggingFeature.class)              
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
            WSSecurityUtil.fetchActionResult(handlerResults.get(0).getResults(), WSConstants.UT);
        AssertionWrapper assertion = 
            (AssertionWrapper)actionResult.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
        Assert.assertTrue(assertion != null && "DoubleItSTSIssuer".equals(assertion.getIssuerString()));
        
        return numberToDouble * 2;
    }
    
}

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
package org.apache.cxf.systest.ws.tokens;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.feature.Features;
import org.apache.cxf.helpers.CastUtils;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class DoubleItBSTImpl implements DoubleItPortType {

    @Resource
    WebServiceContext wsContext;

    public int doubleIt(int numberToDouble) throws DoubleItFault {
        if (numberToDouble == 0) {
            throw new DoubleItFault("0 can't be doubled!");
        }

        List<WSHandlerResult> results =
            CastUtils.cast((List<?>)wsContext.getMessageContext().get(WSHandlerConstants.RECV_RESULTS));
        Assert.assertNotNull("Security Results cannot be null", results);
        Assert.assertFalse(results.isEmpty());

        WSHandlerResult result = results.get(0);
        List<WSSecurityEngineResult> securityResults = result.getResults();
        Assert.assertNotNull("Security Results cannot be null", securityResults);
        Assert.assertFalse(securityResults.isEmpty());

        WSSecurityEngineResult securityResult = securityResults.get(0);
        BinarySecurity binarySecurityToken =
            (BinarySecurity)securityResult.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
        Assert.assertNotNull(binarySecurityToken);

        Assert.assertArrayEquals(binarySecurityToken.getToken(), "This is a token".getBytes());

        return numberToDouble * 2;
    }

}

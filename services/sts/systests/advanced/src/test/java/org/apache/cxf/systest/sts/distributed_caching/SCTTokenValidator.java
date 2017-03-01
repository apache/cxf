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
package org.apache.cxf.systest.sts.distributed_caching;

import java.util.Collection;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.trust.STSTokenValidator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.policy.SP12Constants;

/**
 * This class validates a SecurityContextToken by dispatching it to an STS. It pauses first to make sure
 * that the SCT is replicated in the distributed cache to the (second) STS instance
 */
public class SCTTokenValidator extends STSTokenValidator {

    public SCTTokenValidator() {
        super();
    }

    public SCTTokenValidator(boolean alwaysValidateToSTS) {
        super(alwaysValidateToSTS);
    }

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        // Sleep to make sure token gets replicated
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Credential validatedCredential = super.validate(credential, data);

        // Hack to verify the IssuedToken assertion, as this is not done by default in CXF for a
        // SecurityContextToken
        SoapMessage soapMessage = (SoapMessage)data.getMsgContext();
        AssertionInfoMap aim = soapMessage.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ISSUED_TOKEN);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }

        return validatedCredential;
    }

}

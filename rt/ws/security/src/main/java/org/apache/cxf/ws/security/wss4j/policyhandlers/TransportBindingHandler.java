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

package org.apache.cxf.ws.security.wss4j.policyhandlers;

import java.util.Collection;

import javax.xml.soap.SOAPMessage;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.TransportBinding;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecTimestamp;

/**
 * 
 */
public class TransportBindingHandler extends AbstractBindingBuilder {
    TransportBinding tbinding;
    
    public TransportBindingHandler(TransportBinding binding,
                                    SOAPMessage saaj,
                                    WSSecHeader secHeader,
                                    AssertionInfoMap aim,
                                    SoapMessage message) {
        super(binding, saaj, secHeader, aim, message);
        this.tbinding = binding;
    }
    
    public void handleBinding() {
        Collection<AssertionInfo> ais;
        WSSecTimestamp timestamp = createTimestamp();
        handleLayout(timestamp);
        
        ais = aim.get(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        if (ais != null) {
            SupportingToken sgndSuppTokens = null;
            for (AssertionInfo ai : ais) {
                sgndSuppTokens = (SupportingToken)ai.getAssertion();
                ai.setAsserted(true);
            }
            if (sgndSuppTokens != null && sgndSuppTokens.getTokens() != null 
                && sgndSuppTokens.getTokens().size() > 0) {
                handleSupportingTokens(sgndSuppTokens);
            }
        }
    }

}

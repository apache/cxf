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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.Collection;
import java.util.HashSet;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractToken;

/**
 * Some abstract functionality for validating a Security Token.
 */
public abstract class AbstractTokenPolicyValidator {
    
    /**
     * Check to see if a token is required or not.
     * @param token the token
     * @param message The message
     * @return true if the token is required
     */
    protected boolean isTokenRequired(
        AbstractToken token,
        Message message
    ) {
        IncludeTokenType inclusion = token.getIncludeTokenType();
        if (inclusion == IncludeTokenType.INCLUDE_TOKEN_NEVER) {
            return false;
        } else if (inclusion == IncludeTokenType.INCLUDE_TOKEN_ALWAYS) {
            return true;
        } else {
            boolean initiator = MessageUtils.isRequestor(message);
            if (initiator && (inclusion == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_INITIATOR)) {
                return true;
            } else if (!initiator && (inclusion == IncludeTokenType.INCLUDE_TOKEN_ONCE
                || inclusion == IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT)) {
                return true;
            }
            return false;
        }
    }
    
    protected boolean assertPolicy(AssertionInfoMap aim, QName name) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(name);
        if (aim != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    
    protected boolean assertPolicy(AssertionInfoMap aim, String localname) {
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, localname);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    
    protected Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim,
        String localname
    ) {
        Collection<AssertionInfo> ais = new HashSet<AssertionInfo>();
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        if (sp11Ais != null && !sp11Ais.isEmpty()) {
            ais.addAll(sp11Ais);
        }

        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
        if (sp12Ais != null && !sp12Ais.isEmpty()) {
            ais.addAll(sp12Ais);
        }

        return ais;
    }
}

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

package org.apache.cxf.sts.token.canceller;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.token.SecurityContextToken;

/**
 * This class cancels a SecurityContextToken.
 */
public class SCTCanceller implements TokenCanceller {

    private static final Logger LOG = LogUtils.getL7dLogger(SCTCanceller.class);
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken targetToken) {
        Object token = targetToken.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((STSUtils.SCT_NS_05_02.equals(namespace) 
                || STSUtils.SCT_NS_05_12.equals(namespace))
                && "SecurityContextToken".equals(localname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancel a Token using the given TokenCancellerParameters.
     */
    public TokenCancellerResponse cancelToken(TokenCancellerParameters tokenParameters) {
        LOG.fine("Trying to cancel a SecurityContextToken");
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        ReceivedToken cancelTarget = tokenRequirements.getCancelTarget();

        TokenCancellerResponse response = new TokenCancellerResponse();
        response.setTokenCancelled(false);
        
        if (tokenParameters.getCache() == null) {
            LOG.log(Level.FINE, "A cache must be configured to use the SCTCanceller");
            return response;
        }
        
        if (cancelTarget != null && cancelTarget.isDOMElement()) {
            try {
                Element cancelTargetElement = (Element)cancelTarget.getToken();
                SecurityContextToken sct = new SecurityContextToken(cancelTargetElement);
                String identifier = sct.getIdentifier();
                byte[] secret = (byte[])tokenParameters.getCache().get(identifier);
                if (secret == null) {
                    LOG.fine("Identifier: " + identifier + " is not found in the cache");
                    return response;
                }
                tokenParameters.getCache().remove(identifier);
                response.setTokenCancelled(true);
            } catch (WSSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
            }
        }
        return response;
    }

}

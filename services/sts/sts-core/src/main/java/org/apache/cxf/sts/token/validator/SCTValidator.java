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
package org.apache.cxf.sts.token.validator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSUtils;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.token.SecurityContextToken;

/**
 * This class validates a SecurityContextToken.
 */
public class SCTValidator implements TokenValidator {
    
    /**
     * This tag refers to the secret key (byte[]) associated with a SecurityContextToken that has been
     * validated. It is inserted into the additional properties map of the response, so that it can be
     * retrieved and inserted into a generated token by a TokenProvider instance.
     */
    public static final String SCT_VALIDATOR_SECRET = "sct-validator-secret";
    
    private static final Logger LOG = LogUtils.getL7dLogger(SCTValidator.class);

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this token Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this token Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
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
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating SecurityContextToken");
        
        if (tokenParameters.getTokenStore() == null) {
            LOG.log(Level.FINE, "A cache must be configured to use the SCTValidator");
            TokenValidatorResponse response = new TokenValidatorResponse();
            response.setValid(false);
            return response;
        }
        
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        ReceivedToken validateTarget = tokenRequirements.getValidateTarget();

        TokenValidatorResponse response = new TokenValidatorResponse();
        response.setValid(false);
        
        if (validateTarget != null && validateTarget.isDOMElement()) {
            try {
                Element validateTargetElement = (Element)validateTarget.getToken();
                SecurityContextToken sct = new SecurityContextToken(validateTargetElement);
                String identifier = sct.getIdentifier();
                SecurityToken token = tokenParameters.getTokenStore().getToken(identifier);
                if (token == null) {
                    LOG.fine("Identifier: " + identifier + " is not found in the cache");
                    return response;
                }
                if (token.isExpired()) {
                    LOG.fine("Token: " + identifier + " is in the cache but expired");
                    return response;
                }
                byte[] secret = token.getSecret();
                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(SCT_VALIDATOR_SECRET, secret);
                response.setAdditionalProperties(properties);
                response.setPrincipal(token.getPrincipal());
                
                Properties props = token.getProperties();
                if (props != null) {
                    String realm = props.getProperty(STSConstants.TOKEN_REALM);
                    response.setTokenRealm(realm);
                }
                response.setValid(true);
            } catch (WSSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
            }
        }
        return response;
    }
    
}

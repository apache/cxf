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

package org.apache.cxf.ws.security.trust;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.XMLUtils;

public class DefaultSTSTokenCacher implements STSTokenCacher {

    public SecurityToken retrieveToken(Message message, boolean retrieveTokenFromEndpoint) throws TokenStoreException {
        SecurityToken tok;
        if (retrieveTokenFromEndpoint) {
            tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
            if (tok == null) {
                String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                if (tokId != null) {
                    tok = TokenStoreUtils.getTokenStore(message).getToken(tokId);
                }
            }
        } else {
            tok = (SecurityToken)message.get(SecurityConstants.TOKEN);
            if (tok == null) {
                String tokId = (String)message.get(SecurityConstants.TOKEN_ID);
                if (tokId != null) {
                    tok = TokenStoreUtils.getTokenStore(message).getToken(tokId);
                }
            }
        }
        return tok;
    }

    public SecurityToken retrieveToken(Message message, Element delegationToken, String cacheKey)
            throws TokenStoreException {
        if (delegationToken == null) {
            return null;
        }
        TokenStore tokenStore = TokenStoreUtils.getTokenStore(message);

        // See if the token corresponding to the delegation Token is stored in the cache
        // and if it points to an issued token
        String id = getIdFromToken(delegationToken);
        SecurityToken cachedToken = tokenStore.getToken(id);
        if (cachedToken != null) {
            Map<String, Object> properties = cachedToken.getProperties();
            if (properties != null && properties.containsKey(cacheKey)) {
                String associatedToken = (String)properties.get(cacheKey);
                SecurityToken issuedToken = tokenStore.getToken(associatedToken);
                if (issuedToken != null) {
                    return issuedToken;
                }
            }
        }

        return null;
    }

    public void storeToken(Message message, SecurityToken securityToken, boolean storeTokenInEndpoint)
            throws TokenStoreException {
        if (storeTokenInEndpoint && !isOneTimeUse(securityToken)) {
            message.getExchange().getEndpoint().put(SecurityConstants.TOKEN, securityToken);
            message.getExchange().put(SecurityConstants.TOKEN, securityToken);
            message.getExchange().put(SecurityConstants.TOKEN_ID, securityToken.getId());
            message.getExchange().getEndpoint().put(SecurityConstants.TOKEN_ID,
                                                    securityToken.getId());
        } else {
            message.put(SecurityConstants.TOKEN, securityToken);
            message.put(SecurityConstants.TOKEN_ID, securityToken.getId());
            message.put(SecurityConstants.TOKEN_ELEMENT, securityToken.getToken());
        }
        TokenStoreUtils.getTokenStore(message).add(securityToken);
    }

    public void storeToken(Message message, Element delegationToken, String secTokenId, String cacheKey)
            throws TokenStoreException {
        if (secTokenId == null || delegationToken == null) {
            return;
        }

        TokenStore tokenStore = TokenStoreUtils.getTokenStore(message);

        String id = getIdFromToken(delegationToken);
        SecurityToken cachedToken = tokenStore.getToken(id);
        if (cachedToken == null) {
            cachedToken = new SecurityToken(id);
            cachedToken.setToken(delegationToken);
        }
        Map<String, Object> properties = cachedToken.getProperties();
        if (properties == null) {
            properties = new HashMap<>();
            cachedToken.setProperties(properties);
        }
        properties.put(cacheKey, secTokenId);
        tokenStore.add(cachedToken);
    }

    public void removeToken(Message message, SecurityToken securityToken) throws TokenStoreException {
        // Remove token from cache
        message.getExchange().getEndpoint().remove(SecurityConstants.TOKEN);
        message.getExchange().getEndpoint().remove(SecurityConstants.TOKEN_ID);
        message.getExchange().remove(SecurityConstants.TOKEN_ID);
        message.getExchange().remove(SecurityConstants.TOKEN);
        message.resetContextCache();
        if (securityToken != null) {
            TokenStoreUtils.getTokenStore(message).remove(securityToken.getId());
        }
    }

    // Check to see if the received token is a SAML2 Token with "OneTimeUse" set. If so,
    // it should not be cached on the endpoint, but only on the message.
    private static boolean isOneTimeUse(SecurityToken issuedToken) {
        Element token = issuedToken.getToken();
        if (token != null && "Assertion".equals(token.getLocalName())
            && WSS4JConstants.SAML2_NS.equals(token.getNamespaceURI())) {
            try {
                SamlAssertionWrapper assertion = new SamlAssertionWrapper(token);

                if (assertion.getSaml2().getConditions() != null
                    && assertion.getSaml2().getConditions().getOneTimeUse() != null) {
                    return true;
                }
            } catch (WSSecurityException ex) {
                throw new Fault(ex);
            }
        }

        return false;
    }

    // Get an id from the token that is unique to that token
    private static String getIdFromToken(Element token) {
        if (token != null) {
            // For SAML tokens get the ID/AssertionID
            if ("Assertion".equals(token.getLocalName())
                && WSS4JConstants.SAML2_NS.equals(token.getNamespaceURI())) {
                return token.getAttributeNS(null, "ID");
            } else if ("Assertion".equals(token.getLocalName())
                && WSS4JConstants.SAML_NS.equals(token.getNamespaceURI())) {
                return token.getAttributeNS(null, "AssertionID");
            }

            // For UsernameTokens get the username
            if (WSS4JConstants.USERNAME_TOKEN_LN.equals(token.getLocalName())
                && WSS4JConstants.WSSE_NS.equals(token.getNamespaceURI())) {
                Element usernameElement =
                    XMLUtils.getDirectChildElement(token, WSS4JConstants.USERNAME_LN, WSS4JConstants.WSSE_NS);
                if (usernameElement != null) {
                    return XMLUtils.getElementText(usernameElement);
                }
            }

            // For BinarySecurityTokens take the hash of the value
            if (WSS4JConstants.BINARY_TOKEN_LN.equals(token.getLocalName())
                && WSS4JConstants.WSSE_NS.equals(token.getNamespaceURI())) {
                String text = XMLUtils.getElementText(token);
                if (text != null && !"".equals(text)) {
                    try {
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] bytes = digest.digest(text.getBytes());
                        return org.apache.xml.security.utils.XMLUtils.encodeToString(bytes);
                    } catch (NoSuchAlgorithmException e) {
                        // SHA-256 must be supported so not going to happen...
                    }
                }
            }
        }
        return "";
    }

}
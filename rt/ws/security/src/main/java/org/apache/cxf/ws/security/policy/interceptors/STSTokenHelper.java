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

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.IssuedToken;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;

public final class STSTokenHelper {
    private static final Logger LOG = LogUtils.getL7dLogger(STSTokenHelper.class);
    private static final String ASSOCIATED_TOKEN =
        STSTokenHelper.class.getName() + "-" + "Associated_Token";
    
    private STSTokenHelper() {
    }

    public static SecurityToken getTokenByWSPolicy(Message message, IssuedToken issuedToken,
                                            AssertionInfoMap aim) {
        TokenRequestParams params = new TokenRequestParams();
        params.setIssuer(issuedToken.getIssuer());
        params.setClaims(issuedToken.getClaims());
        if (issuedToken.getPolicy() != null) {
            params.setWspNamespace(issuedToken.getPolicy().getNamespace());
        }
        params.setTrust10(getTrust10(aim));
        params.setTrust13(getTrust13(aim));
        params.setTokenTemplate(issuedToken.getRequestSecurityTokenTemplate());

        return getToken(message, params);
    }

    public static SecurityToken getToken(Message message, TokenRequestParams params) {
        SecurityToken tok = retrieveCachedToken(message);
        if (tok == null) {
            tok = issueToken(message, params);
        } else {
            tok = renewToken(message, tok, params);
        }

        boolean cacheIssuedToken =
            MessageUtils.getContextualBoolean(
                                              message,
                                              SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT,
                                              true)
                && !isOneTimeUse(tok);
        if (cacheIssuedToken) {
            message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN, tok);
            message.getExchange().put(SecurityConstants.TOKEN, tok);
            message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getId());
            message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID,
                                                          tok.getId());
        } else {
            message.put(SecurityConstants.TOKEN, tok);
            message.put(SecurityConstants.TOKEN_ID, tok.getId());
        }
        // ?
        WSS4JUtils.getTokenStore(message).add(tok);

        return tok;
    }

    private static SecurityToken retrieveCachedToken(Message message) {
        boolean cacheIssuedToken =
            MessageUtils.getContextualBoolean(
                                              message,
                                              SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT,
                                              true);
        SecurityToken tok = null;
        if (cacheIssuedToken) {
            tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
            if (tok == null) {
                String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                if (tokId != null) {
                    tok = WSS4JUtils.getTokenStore(message).getToken(tokId);
                }
            }
        } else {
            tok = (SecurityToken)message.get(SecurityConstants.TOKEN);
            if (tok == null) {
                String tokId = (String)message.get(SecurityConstants.TOKEN_ID);
                if (tokId != null) {
                    tok = WSS4JUtils.getTokenStore(message).getToken(tokId);
                }
            }
        }
        return tok;
    }

    private static SecurityToken issueToken(Message message, TokenRequestParams params) {
        AddressingProperties maps =
            (AddressingProperties)message
                .get("javax.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message
                .get("javax.xml.ws.addressing.context");
        }
        STSClient client = STSUtils.getClientWithIssuer(message, "sts", params.getIssuer());
        synchronized (client) {
            try {
                // Transpose ActAs/OnBehalfOf info from original request to the STS client.
                Object token =
                    message.getContextualProperty(SecurityConstants.STS_TOKEN_ACT_AS);
                if (token != null) {
                    client.setActAs(token);
                }
                token =
                    message.getContextualProperty(SecurityConstants.STS_TOKEN_ON_BEHALF_OF);
                if (token != null) {
                    client.setOnBehalfOf(token);
                }
                Map<String, Object> ctx = client.getRequestContext();
                mapSecurityProps(message, ctx);

                Object o = message.getContextualProperty(SecurityConstants.STS_APPLIES_TO);
                String appliesTo = o == null ? null : o.toString();
                appliesTo = appliesTo == null
                    ? message.getContextualProperty(Message.ENDPOINT_ADDRESS).toString()
                    : appliesTo;
                boolean enableAppliesTo = client.isEnableAppliesTo();

                client.setMessage(message);
                Element onBehalfOfToken = client.getOnBehalfOfToken();
                Element actAsToken = client.getActAsToken();

                SecurityToken secToken =
                    handleDelegation(
                                     message, onBehalfOfToken, actAsToken, appliesTo,
                                     enableAppliesTo
                    );
                if (secToken == null) {
                    secToken = getTokenFromSTS(message, client, maps, appliesTo, params);
                }
                storeDelegationTokens(
                                      message, secToken, onBehalfOfToken, actAsToken, appliesTo,
                                      enableAppliesTo);
                return secToken;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new Fault(e);
            } finally {
                client.setTrust((Trust10)null);
                client.setTrust((Trust13)null);
                client.setTemplate(null);
                client.setAddressingNamespace(null);
            }
        }
    }

    private static SecurityToken renewToken(
                                     Message message,
                                     SecurityToken tok,
                                     TokenRequestParams params) {
        String imminentExpiryValue =
            (String)message
                .getContextualProperty(SecurityConstants.STS_TOKEN_IMMINENT_EXPIRY_VALUE);
        long imminentExpiry = 10L;
        if (imminentExpiryValue != null) {
            imminentExpiry = Long.parseLong(imminentExpiryValue);
        }

        // If the token has not expired then we don't need to renew it
        if (!(tok.isExpired() || tok.isAboutToExpire(imminentExpiry))) {
            return tok;
        }

        // Remove token from cache
        message.getExchange().get(Endpoint.class).remove(SecurityConstants.TOKEN);
        message.getExchange().get(Endpoint.class).remove(SecurityConstants.TOKEN_ID);
        message.getExchange().remove(SecurityConstants.TOKEN_ID);
        message.getExchange().remove(SecurityConstants.TOKEN);
        NegotiationUtils.getTokenStore(message).remove(tok.getId());

        // If the user has explicitly disabled Renewing then we can't renew a token,
        // so just get a new one
        STSClient client = STSUtils.getClientWithIssuer(message, "sts", params.getIssuer());
        if (!client.isAllowRenewing()) {
            return issueToken(message, params);
        }

        AddressingProperties maps =
            (AddressingProperties)message
                .get("javax.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message
                .get("javax.xml.ws.addressing.context");
        }
        synchronized (client) {
            try {
                Map<String, Object> ctx = client.getRequestContext();
                mapSecurityProps(message, ctx);

                client.setMessage(message);

                if (maps != null) {
                    client.setAddressingNamespace(maps.getNamespaceURI());
                }

                client.setTrust(params.getTrust10());
                client.setTrust(params.getTrust13());

                client.setTemplate(params.getTokenTemplate());
                return client.renewSecurityToken(tok);
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "Error renewing a token", ex);
                boolean issueAfterFailedRenew =
                    MessageUtils
                        .getContextualBoolean(
                                              message,
                                              SecurityConstants.STS_ISSUE_AFTER_FAILED_RENEW, true);
                if (issueAfterFailedRenew) {
                    // Perhaps the STS does not support renewing, so try to issue a new token
                    return issueToken(message, params);
                } else {
                    throw ex;
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Error renewing a token", ex);
                boolean issueAfterFailedRenew =
                    MessageUtils
                        .getContextualBoolean(
                                              message,
                                              SecurityConstants.STS_ISSUE_AFTER_FAILED_RENEW, true);
                if (issueAfterFailedRenew) {
                    // Perhaps the STS does not support renewing, so try to issue a new token
                    return issueToken(message, params);
                } else {
                    throw new Fault(ex);
                }
            } finally {
                client.setTrust((Trust10)null);
                client.setTrust((Trust13)null);
                client.setTemplate(null);
                client.setAddressingNamespace(null);
            }
        }
    }

    // Check to see if the received token is a SAML2 Token with "OneTimeUse" set. If so,
    // it should not be cached on the endpoint, but only on the message.
    private static boolean isOneTimeUse(SecurityToken issuedToken) {
        Element token = issuedToken.getToken();
        if (token != null && "Assertion".equals(token.getLocalName())
            && WSConstants.SAML2_NS.equals(token.getNamespaceURI())) {
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

    private static void mapSecurityProps(Message message, Map<String, Object> ctx) {
        for (String s : SecurityConstants.ALL_PROPERTIES) {
            Object v = message.getContextualProperty(s + ".it");
            if (v == null) {
                v = message.getContextualProperty(s);
            }
            if (!ctx.containsKey(s) && v != null) {
                ctx.put(s, v);
            }
        }
    }

    /**
     * Parse ActAs/OnBehalfOf appropriately. See if the required token is stored in the cache.
     */
    private static SecurityToken handleDelegation(
                                           Message message,
                                           Element onBehalfOfToken,
                                           Element actAsToken,
                                           String appliesTo,
                                           boolean enableAppliesTo) throws Exception {
        TokenStore tokenStore = WSS4JUtils.getTokenStore(message);
        String key = appliesTo;
        if (!enableAppliesTo || key == null || "".equals(key)) {
            key = ASSOCIATED_TOKEN;
        }
        // See if the token corresponding to the OnBehalfOf Token is stored in the cache
        // and if it points to an issued token
        if (onBehalfOfToken != null) {
            String id = getIdFromToken(onBehalfOfToken);
            SecurityToken cachedToken = tokenStore.getToken(id);
            if (cachedToken != null) {
                Properties properties = cachedToken.getProperties();
                if (properties != null && properties.containsKey(key)) {
                    String associatedToken = properties.getProperty(key);
                    SecurityToken issuedToken = tokenStore.getToken(associatedToken);
                    if (issuedToken != null) {
                        return issuedToken;
                    }
                }
            }
        }

        // See if the token corresponding to the ActAs Token is stored in the cache
        // and if it points to an issued token
        if (actAsToken != null) {
            String id = getIdFromToken(actAsToken);
            SecurityToken cachedToken = tokenStore.getToken(id);
            if (cachedToken != null) {
                Properties properties = cachedToken.getProperties();
                if (properties != null && properties.containsKey(key)) {
                    String associatedToken = properties.getProperty(key);
                    SecurityToken issuedToken = tokenStore.getToken(associatedToken);
                    if (issuedToken != null) {
                        return issuedToken;
                    }
                }
            }
        }
        return null;
    }

    private static String getIdFromToken(Element token) {
        if (token != null) {
            // Try to find the "Id" on the token.
            if (token.hasAttributeNS(WSConstants.WSU_NS, "Id")) {
                return token.getAttributeNS(WSConstants.WSU_NS, "Id");
            } else if (token.hasAttributeNS(null, "ID")) {
                return token.getAttributeNS(null, "ID");
            } else if (token.hasAttributeNS(null, "AssertionID")) {
                return token.getAttributeNS(null, "AssertionID");
            }
        }
        return "";
    }

    private static void storeDelegationTokens(
                                       Message message,
                                       SecurityToken issuedToken,
                                       Element onBehalfOfToken,
                                       Element actAsToken,
                                       String appliesTo,
                                       boolean enableAppliesTo) throws Exception {
        if (issuedToken == null) {
            return;
        }
        TokenStore tokenStore = WSS4JUtils.getTokenStore(message);
        String key = appliesTo;
        if (!enableAppliesTo || key == null || "".equals(key)) {
            key = ASSOCIATED_TOKEN;
        }
        if (onBehalfOfToken != null) {
            String id = getIdFromToken(onBehalfOfToken);
            SecurityToken cachedToken = tokenStore.getToken(id);
            if (cachedToken == null) {
                cachedToken = new SecurityToken(id);
                cachedToken.setToken(onBehalfOfToken);
            }
            Properties properties = cachedToken.getProperties();
            if (properties == null) {
                properties = new Properties();
                cachedToken.setProperties(properties);
            }
            properties.put(key, issuedToken.getId());
            tokenStore.add(cachedToken);
        }
        if (actAsToken != null) {
            String id = getIdFromToken(actAsToken);
            SecurityToken cachedToken = tokenStore.getToken(id);
            if (cachedToken == null) {
                cachedToken = new SecurityToken(id);
                cachedToken.setToken(actAsToken);
            }
            Properties properties = cachedToken.getProperties();
            if (properties == null) {
                properties = new Properties();
                cachedToken.setProperties(properties);
            }
            properties.put(key, issuedToken.getId());
            tokenStore.add(cachedToken);
        }
    }

    private static SecurityToken getTokenFromSTS(Message message, STSClient client,
                                          AddressingProperties maps, String appliesTo,
                                          TokenRequestParams params) throws Exception {
        client.setTrust(params.getTrust10());
        client.setTrust(params.getTrust13());
        client.setTemplate(params.getTokenTemplate());
        if (params.getWspNamespace() != null) {
            client.setWspNamespace(params.getWspNamespace());
        }
        if (maps != null && maps.getNamespaceURI() != null) {
            client.setAddressingNamespace(maps.getNamespaceURI());
        }
        if (params.getClaims() != null) {
            client.setClaims(params.getClaims());
        }
        return client.requestSecurityToken(appliesTo);
    }

    private static Trust10 getTrust10(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.TRUST_10);
        if (ais.isEmpty()) {
            return null;
        }
        return (Trust10)ais.iterator().next().getAssertion();
    }

    private static Trust13 getTrust13(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais =
            PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.TRUST_13);
        if (ais.isEmpty()) {
            return null;
        }
        return (Trust13)ais.iterator().next().getAssertion();
    }

    public static class TokenRequestParams {
        private Element issuer;
        private Trust10 trust10;
        private Trust13 trust13;
        private Element tokenTemplate;
        private String wspNamespace;
        private Element claims;

        public Element getIssuer() {
            return issuer;
        }

        public void setIssuer(Element issuer) {
            this.issuer = issuer;
        }

        public Trust10 getTrust10() {
            return trust10;
        }

        public void setTrust10(Trust10 trust10) {
            this.trust10 = trust10;
        }

        public Trust13 getTrust13() {
            return trust13;
        }

        public void setTrust13(Trust13 trust13) {
            this.trust13 = trust13;
        }

        public Element getTokenTemplate() {
            return tokenTemplate;
        }

        public void setTokenTemplate(Element tokenTemplate) {
            this.tokenTemplate = tokenTemplate;
        }

        public String getWspNamespace() {
            return wspNamespace;
        }

        public void setWspNamespace(String wspNamespace) {
            this.wspNamespace = wspNamespace;
        }

        public Element getClaims() {
            return claims;
        }

        public void setClaims(Element claims) {
            this.claims = claims;
        }
    }
}

/*
 * STSClient stsClient = new STSClient(bus); stsClient.setServiceQName(new QName(stsProps.get(STS_NAMESPACE),
 * stsProps.get(STS_SERVICE_NAME))); Map<String, Object> props = new HashMap<String, Object>(); for
 * (Map.Entry<String, String> entry : stsProps.entrySet()) { if
 * (SecurityConstants.ALL_PROPERTIES.contains(entry.getKey())) { props.put(entry.getKey(),
 * processFileURI(entry.getValue())); } } stsClient.setProperties(props);
 * stsClient.setWsdlLocation(stsProps.get(STS_WSDL_LOCATION)); stsClient.setEndpointQName(new
 * QName(stsProps.get(STS_NAMESPACE), stsProps.get(STS_ENDPOINT_NAME)));
 * stsClient.setAllowRenewingAfterExpiry(true); stsClient.setEnableLifetime(true);
 * stsClient.setTokenType(SAML2_TOKEN_TYPE); stsClient.setKeyType(BEARER_KEYTYPE); if (token != null) {
 * stsClient.setActAs(token); } token =
 * message.getContextualProperty(SecurityConstants.STS_TOKEN_ON_BEHALF_OF); if (token != null) {
 * stsClient.setOnBehalfOf(token); } Object o =
 * message.getContextualProperty(SecurityConstants.STS_APPLIES_TO); String appliesTo = null == o ? null :
 * o.toString(); appliesTo = null == appliesTo ?
 * message.getContextualProperty(Message.ENDPOINT_ADDRESS).toString() : appliesTo;
 * stsClient.setMessage(message);
 */

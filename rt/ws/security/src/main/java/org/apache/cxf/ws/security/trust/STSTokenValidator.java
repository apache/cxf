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

import java.io.IOException;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.trust.delegation.DelegationCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;

/**
 * A WSS4J-based Validator to validate a received WS-Security credential by dispatching
 * it to a STS via WS-Trust. The default binding is "validate", but "issue" is also possible
 * by setting the "useIssueBinding" property. In this case, the credentials are sent via
 * "OnBehalfOf" unless the "useOnBehalfOf" property is set to "false", in which case the
 * credentials are used depending on the security policy of the STS endpoint (e.g. in a
 * UsernameToken if this is what the policy requires). Setting "useOnBehalfOf" to "false" +
 * "useIssueBinding" to "true" only works for validating UsernameTokens.
 */
public class STSTokenValidator implements Validator {
    private boolean alwaysValidateToSts;
    private boolean useIssueBinding;
    private boolean useOnBehalfOf = true;
    private STSClient stsClient;
    private TokenStore tokenStore;
    private boolean disableCaching;

    public STSTokenValidator() {
    }

    /**
     * Construct a new instance.
     * @param alwaysValidateToSts whether to always validate the token to the STS
     */
    public STSTokenValidator(boolean alwaysValidateToSts) {
        this.alwaysValidateToSts = alwaysValidateToSts;
    }

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {

        if (isValidatedLocally(credential, data)) {
            return credential;
        }

        return validateWithSTS(credential, (Message)data.getMsgContext());
    }

    public Credential validateWithSTS(Credential credential, Message message) throws WSSecurityException {

        try {
            SecurityToken token = new SecurityToken();
            Element tokenElement = null;
            String cacheKey = null;
            if (credential.getSamlAssertion() != null) {
                SamlAssertionWrapper assertion = credential.getSamlAssertion();
                cacheKey = TokenStoreUtils.getCacheKey(assertion);
                tokenElement = assertion.getElement();
            } else if (credential.getUsernametoken() != null) {
                tokenElement = credential.getUsernametoken().getElement();
                cacheKey = TokenStoreUtils.getCacheKey(credential.getUsernametoken());
            } else if (credential.getBinarySecurityToken() != null) {
                tokenElement = credential.getBinarySecurityToken().getElement();
                cacheKey = TokenStoreUtils.getCacheKey(credential.getBinarySecurityToken());
            } else if (credential.getSecurityContextToken() != null) {
                tokenElement = credential.getSecurityContextToken().getElement();
                cacheKey = TokenStoreUtils.getCacheKey(credential.getSecurityContextToken());
            }
            token.setToken(tokenElement);

            TokenStore ts = null;
            if (!disableCaching) {
                ts = getTokenStore(message);
                if (ts == null) {
                    ts = tokenStore;
                }
                if (ts != null && cacheKey != null) {
                    SecurityToken transformedToken = getTransformedToken(ts, cacheKey);
                    if (transformedToken != null && !transformedToken.isExpired()) {
                        SamlAssertionWrapper assertion = new SamlAssertionWrapper(transformedToken.getToken());
                        credential.setPrincipal(new SAMLTokenPrincipalImpl(assertion));
                        credential.setTransformedToken(assertion);
                        return credential;
                    }
                }
            }

            STSClient c = stsClient;
            if (c == null) {
                c = STSUtils.getClient(message, "sts");
            }

            synchronized (c) {
                System.setProperty("noprint", "true");

                final SecurityToken returnedToken;

                if (useIssueBinding && useOnBehalfOf) {
                    ElementCallbackHandler callbackHandler = new ElementCallbackHandler(tokenElement);
                    c.setOnBehalfOf(callbackHandler);
                    returnedToken = c.requestSecurityToken();
                    c.setOnBehalfOf(null);
                } else if (useIssueBinding && !useOnBehalfOf && credential.getUsernametoken() != null) {
                    c.getProperties().put(SecurityConstants.USERNAME,
                                          credential.getUsernametoken().getName());
                    c.getProperties().put(SecurityConstants.PASSWORD,
                                          credential.getUsernametoken().getPassword());
                    returnedToken = c.requestSecurityToken();
                    c.getProperties().remove(SecurityConstants.USERNAME);
                    c.getProperties().remove(SecurityConstants.PASSWORD);
                } else {
                    List<SecurityToken> tokens = c.validateSecurityToken(token);
                    returnedToken = tokens.get(0);
                }

                if (returnedToken != token) {
                    SamlAssertionWrapper assertion = new SamlAssertionWrapper(returnedToken.getToken());
                    credential.setTransformedToken(assertion);
                    credential.setPrincipal(new SAMLTokenPrincipalImpl(assertion));
                    if (!disableCaching && cacheKey != null && ts != null) {
                        ts.add(returnedToken);
                        token.setTransformedTokenIdentifier(returnedToken.getId());
                        ts.add(cacheKey, token);
                    }
                }
                return credential;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "invalidSAMLsecurity");
        }
    }

    static final TokenStore getTokenStore(Message message) throws TokenStoreException {
        if (message == null) {
            return null;
        }

        return TokenStoreUtils.getTokenStore(message);
    }

    protected boolean isValidatedLocally(Credential credential, RequestData data)
        throws WSSecurityException {

        if (!alwaysValidateToSts && credential.getSamlAssertion() != null) {
            try {
                // STSSamlAssertionValidator records the trust verification result in an instance field, so
                // a new instance must be used for each request to avoid sharing state between requests
                STSSamlAssertionValidator samlValidator = new STSSamlAssertionValidator();
                samlValidator.validate(credential, data);
                return samlValidator.isTrustVerificationSucceeded();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "invalidSAMLsecurity");
            }
        }
        return false;
    }

    private SecurityToken getTransformedToken(TokenStore ts, String cacheKey) {
        SecurityToken recoveredToken = ts.getToken(cacheKey);
        if (recoveredToken != null) {
            String transformedTokenId = recoveredToken.getTransformedTokenIdentifier();
            if (transformedTokenId != null) {
                return ts.getToken(transformedTokenId);
            }
        }
        return null;
    }

    public boolean isUseIssueBinding() {
        return useIssueBinding;
    }

    public void setUseIssueBinding(boolean useIssueBinding) {
        this.useIssueBinding = useIssueBinding;
    }

    public boolean isUseOnBehalfOf() {
        return useOnBehalfOf;
    }

    public void setUseOnBehalfOf(boolean useOnBehalfOf) {
        this.useOnBehalfOf = useOnBehalfOf;
    }

    public STSClient getStsClient() {
        return stsClient;
    }

    public void setStsClient(STSClient stsClient) {
        this.stsClient = stsClient;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public boolean isDisableCaching() {
        return disableCaching;
    }

    public void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }

    private static class ElementCallbackHandler implements CallbackHandler {

        private final Element tokenElement;

        ElementCallbackHandler(Element tokenElement) {
            this.tokenElement = tokenElement;
        }

        public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof DelegationCallback) {
                    DelegationCallback callback = (DelegationCallback) callbacks[i];

                    callback.setToken(tokenElement);
                } else {
                    throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
                }
            }
        }
    }

}

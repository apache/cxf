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
package org.apache.cxf.rs.security.oauth2.provider;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.ModelEncryptionSupport;

public class JoseSessionTokenProvider implements SessionAuthenticityTokenProvider {
    private JwsSignatureProvider jwsProvider;
    private JwsSignatureVerifier jwsVerifier;
    private JweEncryptionProvider jweEncryptor;
    private JweDecryptionProvider jweDecryptor;
    private boolean jwsRequired;
    private boolean jweRequired;
    private int maxDefaultSessionInterval;
    @Override
    public String createSessionToken(MessageContext mc, MultivaluedMap<String, String> params,
                                     UserSubject subject, OAuthRedirectionState secData) {
        String stateString = convertStateToString(secData);
        String sessionToken = protectStateString(stateString);
        return OAuthUtils.setSessionToken(mc, sessionToken, maxDefaultSessionInterval);
    }

    @Override
    public String getSessionToken(MessageContext mc, MultivaluedMap<String, String> params,
                                  UserSubject subject) {
        return OAuthUtils.getSessionToken(mc);
    }

    @Override
    public String removeSessionToken(MessageContext mc, MultivaluedMap<String, String> params,
                                     UserSubject subject) {
        return getSessionToken(mc, params, subject);
    }

    @Override
    public OAuthRedirectionState getSessionState(MessageContext messageContext, String sessionToken,
                                                 UserSubject subject) {

        String stateString = decryptStateString(sessionToken);
        return convertStateStringToState(stateString);

    }

    public void setJwsProvider(JwsSignatureProvider jwsProvider) {
        this.jwsProvider = jwsProvider;
    }

    public void setJwsVerifier(JwsSignatureVerifier jwsVerifier) {
        this.jwsVerifier = jwsVerifier;
    }

    public void setJweEncryptor(JweEncryptionProvider jweEncryptor) {
        this.jweEncryptor = jweEncryptor;
    }

    public void setJweDecryptor(JweDecryptionProvider jweDecryptor) {
        this.jweDecryptor = jweDecryptor;
    }

    protected JwsSignatureProvider getInitializedSigProvider() {
        if (jwsProvider != null) {
            return jwsProvider;
        }
        return JwsUtils.loadSignatureProvider(jwsRequired);
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider() {
        if (jweEncryptor != null) {
            return jweEncryptor;
        }
        return JweUtils.loadEncryptionProvider(jweRequired);
    }

    public void setJwsRequired(boolean jwsRequired) {
        this.jwsRequired = jwsRequired;
    }
    public void setJweRequired(boolean jweRequired) {
        this.jweRequired = jweRequired;
    }

    protected JweDecryptionProvider getInitializedDecryptionProvider() {
        if (jweDecryptor != null) {
            return jweDecryptor;
        }
        return JweUtils.loadDecryptionProvider(jweRequired);
    }
    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (jwsVerifier != null) {
            return jwsVerifier;
        }
        return JwsUtils.loadSignatureVerifier(jwsRequired);
    }

    private String decryptStateString(String sessionToken) {
        JweDecryptionProvider jwe = getInitializedDecryptionProvider();
        String stateString = jwe.decrypt(sessionToken).getContentText();
        JwsSignatureVerifier jws = getInitializedSigVerifier();
        if (jws != null) {
            stateString = JwsUtils.verify(jws, stateString).getDecodedJwsPayload();
        }
        return stateString;
    }

    private String protectStateString(String stateString) {
        JwsSignatureProvider jws = getInitializedSigProvider();
        JweEncryptionProvider jwe = getInitializedEncryptionProvider();
        if (jws == null && jwe == null) {
            throw new OAuthServiceException("Session token can not be created");
        }
        if (jws != null) {
            stateString = JwsUtils.sign(jws, stateString, null);
        }
        if (jwe != null) {
            stateString = jwe.encrypt(StringUtils.toBytesUTF8(stateString), null);
        }
        return stateString;
    }

    private OAuthRedirectionState convertStateStringToState(String stateString) {
        String[] parts = ModelEncryptionSupport.getParts(stateString);
        OAuthRedirectionState state = new OAuthRedirectionState();
        if (!StringUtils.isEmpty(parts[0])) {
            state.setClientId(parts[0]);
        }
        if (!StringUtils.isEmpty(parts[1])) {
            state.setAudience(parts[1]);
        }
        if (!StringUtils.isEmpty(parts[2])) {
            state.setClientCodeChallenge(parts[2]);
        }
        if (!StringUtils.isEmpty(parts[3])) {
            state.setState(parts[3]);
        }
        if (!StringUtils.isEmpty(parts[4])) {
            state.setProposedScope(parts[4]);
        }
        if (!StringUtils.isEmpty(parts[5])) {
            state.setRedirectUri(parts[5]);
        }
        if (!StringUtils.isEmpty(parts[6])) {
            state.setNonce(parts[6]);
        }
        if (!StringUtils.isEmpty(parts[7])) {
            state.setResponseType(parts[7]);
        }
        if (!StringUtils.isEmpty(parts[8])) {
            state.setExtraProperties(ModelEncryptionSupport.parseSimpleMap(parts[8]));
        }
        if (!StringUtils.isEmpty(parts[9])) {
            state.setClientCodeChallengeMethod(parts[9]);
        }
        return state;
    }
    protected String convertStateToString(OAuthRedirectionState secData) {
        //TODO: make it simpler, convert it to JwtClaims -> JSON
        StringBuilder state = new StringBuilder();
        // 0: client id
        state.append(ModelEncryptionSupport.tokenizeString(secData.getClientId()));
        state.append(ModelEncryptionSupport.SEP);
        // 1: client audience
        state.append(ModelEncryptionSupport.tokenizeString(secData.getAudience()));
        state.append(ModelEncryptionSupport.SEP);
        // 2: client code challenge
        state.append(ModelEncryptionSupport.tokenizeString(secData.getClientCodeChallenge()));
        state.append(ModelEncryptionSupport.SEP);
        // 3: state
        state.append(ModelEncryptionSupport.tokenizeString(secData.getState()));
        state.append(ModelEncryptionSupport.SEP);
        // 4: scope
        state.append(ModelEncryptionSupport.tokenizeString(secData.getProposedScope()));
        state.append(ModelEncryptionSupport.SEP);
        // 5: redirect uri
        state.append(ModelEncryptionSupport.tokenizeString(secData.getRedirectUri()));
        state.append(ModelEncryptionSupport.SEP);
        // 6: nonce
        state.append(ModelEncryptionSupport.tokenizeString(secData.getNonce()));
        state.append(ModelEncryptionSupport.SEP);
        // 7: response_type
        state.append(ModelEncryptionSupport.tokenizeString(secData.getResponseType()));
        state.append(ModelEncryptionSupport.SEP);
        // 8: extra props
        state.append(secData.getExtraProperties().toString());
        state.append(ModelEncryptionSupport.SEP);
        // 9: client code challenge method
        state.append(ModelEncryptionSupport.tokenizeString(secData.getClientCodeChallengeMethod()));

        return state.toString();
    }

    public void setMaxDefaultSessionInterval(int maxDefaultSessionInterval) {
        this.maxDefaultSessionInterval = maxDefaultSessionInterval;
    }
}

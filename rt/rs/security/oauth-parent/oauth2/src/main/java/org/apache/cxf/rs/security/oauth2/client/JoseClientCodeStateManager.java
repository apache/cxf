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
package org.apache.cxf.rs.security.oauth2.client;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jws.NoneJwsSignatureProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class JoseClientCodeStateManager implements ClientCodeStateManager {
    private JwsSignatureProvider sigProvider;
    private JweEncryptionProvider encryptionProvider;
    private JweDecryptionProvider decryptionProvider;
    private JwsSignatureVerifier signatureVerifier;
    private JsonMapObjectReaderWriter jsonp = new JsonMapObjectReaderWriter();
    private boolean generateNonce;
    private boolean storeInSession;
    @Override
    public MultivaluedMap<String, String> toRedirectState(MessageContext mc,
                                                          MultivaluedMap<String, String> requestState) {
        JweEncryptionProvider theEncryptionProvider = getInitializedEncryptionProvider();
        JwsSignatureProvider theSigProvider = getInitializedSigProvider(theEncryptionProvider);
        if (theEncryptionProvider == null && theSigProvider == null) {
            throw new OAuthServiceException("The state can not be protected");
        }
        MultivaluedMap<String, String> redirectMap = new MetadataMap<>();

        if (generateNonce && theSigProvider != null) {
            JwsCompactProducer nonceProducer = new JwsCompactProducer(OAuthUtils.generateRandomTokenKey());
            String nonceParam = nonceProducer.signWith(theSigProvider);
            requestState.putSingle(OAuthConstants.NONCE, nonceParam);
            redirectMap.putSingle(OAuthConstants.NONCE, nonceParam);
        }
        Map<String, Object> stateMap = CastUtils.cast((Map<?, ?>)requestState);
        String json = jsonp.toJson(stateMap);

        String stateParam = null;
        if (theSigProvider != null) {
            JwsCompactProducer stateProducer = new JwsCompactProducer(json);
            stateParam = stateProducer.signWith(theSigProvider);
        }

        if (theEncryptionProvider != null) {
            stateParam = theEncryptionProvider.encrypt(StringUtils.toBytesUTF8(stateParam), null);
        }
        if (storeInSession) {
            String sessionStateAttribute = OAuthUtils.generateRandomTokenKey();
            OAuthUtils.setSessionToken(mc, stateParam, sessionStateAttribute, 0);
            stateParam = sessionStateAttribute;
        }
        redirectMap.putSingle(OAuthConstants.STATE, stateParam);

        return redirectMap;
    }

    @Override
    public MultivaluedMap<String, String> fromRedirectState(MessageContext mc,
                                                            MultivaluedMap<String, String> redirectState) {

        String stateParam = redirectState.getFirst(OAuthConstants.STATE);

        if (storeInSession) {
            stateParam = OAuthUtils.getSessionToken(mc, stateParam);
        }

        JweDecryptionProvider jwe = getInitializedDecryptionProvider();
        if (jwe != null) {
            stateParam = jwe.decrypt(stateParam).getContentText();
        }
        JwsCompactConsumer jws = new JwsCompactConsumer(stateParam);
        JwsSignatureVerifier theSigVerifier = getInitializedSigVerifier();
        if (!jws.verifySignatureWith(theSigVerifier)) {
            throw new SecurityException();
        }
        String json = jws.getUnsignedEncodedSequence();

        Map<String, List<String>> map = CastUtils.cast((Map<?, ?>)jsonp.fromJson(json));
        return (MultivaluedMap<String, String>)map;
    }

    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }

    protected JwsSignatureProvider getInitializedSigProvider(JweEncryptionProvider theEncryptionProvider) {
        if (sigProvider != null) {
            return sigProvider;
        }
        JwsSignatureProvider theSigProvider = JwsUtils.loadSignatureProvider(false);
        if (theSigProvider == null && theEncryptionProvider != null) {
            theSigProvider = new NoneJwsSignatureProvider();
        }
        return theSigProvider;
    }
    public void setDecryptionProvider(JweDecryptionProvider decProvider) {
        this.decryptionProvider = decProvider;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider() {
        if (decryptionProvider != null) {
            return decryptionProvider;
        }
        return JweUtils.loadDecryptionProvider(false);
    }
    public void setSignatureVerifier(JwsSignatureVerifier signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }

    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (signatureVerifier != null) {
            return signatureVerifier;
        }
        return JwsUtils.loadSignatureVerifier(false);
    }
    public void setEncryptionProvider(JweEncryptionProvider encProvider) {
        this.encryptionProvider = encProvider;
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider() {
        if (encryptionProvider != null) {
            return encryptionProvider;
        }
        return JweUtils.loadEncryptionProvider(false);
    }

    public void setGenerateNonce(boolean generateNonce) {
        this.generateNonce = generateNonce;
    }

    public void setStoreInSession(boolean storeInSession) {
        this.storeInSession = storeInSession;
    }

}

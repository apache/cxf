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
package org.apache.cxf.rs.security.jose.jws;

import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsJsonConsumer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsJsonConsumer.class);
    private String jwsSignedDocument;
    private String jwsPayload;
    private List<JwsJsonSignatureEntry> signatures = new LinkedList<>();
    /**
     * @param jwsSignedDocument
     *            signed JWS Document
     */
    public JwsJsonConsumer(String jwsSignedDocument) {
        this(jwsSignedDocument, null);
    }
    public JwsJsonConsumer(String jwsSignedDocument, String detachedPayload) {
        this.jwsSignedDocument = jwsSignedDocument;
        prepare(detachedPayload);
    }

    private void prepare(String detachedPayload) {
        JsonMapObject jsonObject = new JsonMapObject();
        new JsonMapObjectReaderWriter().fromJson(jsonObject, jwsSignedDocument);

        Map<String, Object> jsonObjectMap = jsonObject.asMap();
        jwsPayload = (String)jsonObjectMap.get("payload");
        if (jwsPayload == null) {
            jwsPayload = detachedPayload;
        } else if (detachedPayload != null) {
            LOG.warning("JSON JWS includes a payload expected to be detached");
            throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
        }
        if (jwsPayload == null) {
            LOG.warning("JSON JWS has no payload");
            throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
        }
        List<Map<String, Object>> signatureArray = CastUtils.cast((List<?>)jsonObjectMap.get("signatures"));
        if (signatureArray != null) {
            if (jsonObjectMap.containsKey("signature")) {
                LOG.warning("JSON JWS has a flattened 'signature' element and a 'signatures' object");
                throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
            }
            for (Map<String, Object> signatureEntry : signatureArray) {
                this.signatures.add(getSignatureObject(signatureEntry));
            }
        } else {
            this.signatures.add(getSignatureObject(jsonObjectMap));
        }
        if (signatures.isEmpty()) {
            LOG.warning("JSON JWS has no signatures");
            throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
        }
        validateB64Status();


    }
    private Boolean validateB64Status() {
        return JwsJsonProducer.validateB64Status(signatures);
    }
    protected final JwsJsonSignatureEntry getSignatureObject(Map<String, Object> signatureEntry) {
        String protectedHeader = (String)signatureEntry.get("protected");
        Map<String, Object> header = CastUtils.cast((Map<?, ?>)signatureEntry.get("header"));
        String signature = (String)signatureEntry.get("signature");
        return
            new JwsJsonSignatureEntry(jwsPayload,
                                      protectedHeader,
                                      signature,
                                      header != null ? new JwsHeaders(header) : null);
    }
    public String getSignedDocument() {
        return this.jwsSignedDocument;
    }
    public String getJwsPayload() {
        return this.jwsPayload;
    }
    public String getDecodedJwsPayload() {
        if (validateB64Status()) {
            return JoseUtils.decodeToString(jwsPayload);
        }
        return jwsPayload;
    }
    public byte[] getDecodedJwsPayloadBytes() {
        return StringUtils.toBytesUTF8(getDecodedJwsPayload());
    }
    public List<JwsJsonSignatureEntry> getSignatureEntries() {
        return Collections.unmodifiableList(signatures);
    }
    public Map<SignatureAlgorithm, List<JwsJsonSignatureEntry>> getSignatureEntryMap() {
        return JwsUtils.getJwsJsonSignatureMap(signatures);
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        return verifySignatureWith(validator, null);
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator, Map<String, Object> entryProps) {
        List<JwsJsonSignatureEntry> theSignatureEntries =
            getSignatureEntryMap().get(validator.getAlgorithm());
        if (theSignatureEntries != null) {
            for (JwsJsonSignatureEntry signatureEntry : theSignatureEntries) {
                if (entryProps != null
                    && !signatureEntry.getUnionHeader().asMap().entrySet().containsAll(entryProps.entrySet())) {
                    continue;
                }
                if (signatureEntry.verifySignatureWith(validator)) {
                    return true;
                }
            }
        }
        return false;
    }
    public boolean verifySignatureWith(PublicKey key, SignatureAlgorithm algo) {
        return verifySignatureWith(key, algo, null);
    }
    public boolean verifySignatureWith(PublicKey key, SignatureAlgorithm algo, Map<String, Object> entryProps) {
        return verifySignatureWith(JwsUtils.getPublicKeySignatureVerifier(key, algo), entryProps);
    }
    public boolean verifySignatureWith(byte[] key, SignatureAlgorithm algo) {
        return verifySignatureWith(key, algo, null);
    }
    public boolean verifySignatureWith(byte[] key, SignatureAlgorithm algo, Map<String, Object> entryProps) {
        return verifySignatureWith(JwsUtils.getHmacSignatureVerifier(key, algo), entryProps);
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    public boolean verifySignatureWith(JsonWebKey key, SignatureAlgorithm algo) {
        return verifySignatureWith(key, algo, null);
    }
    public boolean verifySignatureWith(JsonWebKey key, SignatureAlgorithm algo, Map<String, Object> entryProps) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key, algo), entryProps);
    }
    public boolean verifySignatureWith(List<JwsSignatureVerifier> validators) {
        return verifySignatureWith(validators, null);
    }

    public boolean verifySignatureWith(List<JwsSignatureVerifier> validators, Map<String, Object> entryProps) {
        try {
            verifyAndGetNonValidated(validators, entryProps);
        } catch (JwsException ex) {
            LOG.warning("One of JSON JWS signatures is invalid");
            return false;
        }
        return true;
    }

    public List<JwsJsonSignatureEntry> verifyAndGetNonValidated(List<JwsSignatureVerifier> validators) {
        return verifyAndGetNonValidated(validators, null);
    }

    public List<JwsJsonSignatureEntry> verifyAndGetNonValidated(List<JwsSignatureVerifier> validators,
                                                                Map<String, Object> entryProps) {
        List<JwsJsonSignatureEntry> validatedSignatures = new LinkedList<>();
        for (JwsSignatureVerifier validator : validators) {
            List<JwsJsonSignatureEntry> theSignatureEntries =
                getSignatureEntryMap().get(validator.getAlgorithm());
            if (theSignatureEntries != null) {
                for (JwsJsonSignatureEntry sigEntry : theSignatureEntries) {
                    if (entryProps != null
                        && !sigEntry.getUnionHeader().asMap().entrySet().containsAll(entryProps.entrySet())) {
                        continue;
                    }
                    if (sigEntry.verifySignatureWith(validator)) {
                        validatedSignatures.add(sigEntry);
                        break;
                    }
                }
            }
        }
        if (validatedSignatures.isEmpty()) {
            throw new JwsException(JwsException.Error.INVALID_SIGNATURE);
        }
        List<JwsJsonSignatureEntry> nonValidatedSignatures = new LinkedList<>();
        for (JwsJsonSignatureEntry sigEntry : signatures) {
            if (!validatedSignatures.contains(sigEntry)) {
                nonValidatedSignatures.add(sigEntry);
            }
        }
        return nonValidatedSignatures;
    }
    public String verifyAndProduce(List<JwsSignatureVerifier> validators) {
        return verifyAndProduce(validators, null);

    }
    public String verifyAndProduce(List<JwsSignatureVerifier> validators, Map<String, Object> entryProps) {
        List<JwsJsonSignatureEntry> nonValidated = verifyAndGetNonValidated(validators, entryProps);
        if (!nonValidated.isEmpty()) {
            JwsJsonProducer producer = new JwsJsonProducer(getDecodedJwsPayload());
            producer.getSignatureEntries().addAll(nonValidated);
            return producer.getJwsJsonSignedDocument();
        }
        return null;
    }


}

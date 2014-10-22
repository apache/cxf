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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.provider.json.JsonMapObject;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsJsonConsumer {

    private String jwsSignedDocument;
    private String encodedJwsPayload;
    private List<JwsJsonSignatureEntry> signatureEntries;
    
    /**
     * @param jwsSignedDocument
     *            signed JWS Document
     */
    public JwsJsonConsumer(String jwsSignedDocument) {
        this.jwsSignedDocument = jwsSignedDocument;
        prepare();
    }

    private void prepare() {
        JsonMapObject jsonObject = new JsonMapObject();
        new JsonMapObjectReaderWriter().fromJson(jsonObject, jwsSignedDocument);
        encodedJwsPayload = (String)jsonObject.asMap().get("payload");
        if (encodedJwsPayload == null) {
            throw new SecurityException("Invalid JWS JSON sequence: no payload is available");
        }
        
        List<Map<String, Object>> signatureArray = CastUtils.cast((List<?>)jsonObject.asMap().get("signatures"));
        
        this.signatureEntries = new ArrayList<JwsJsonSignatureEntry>(signatureArray.size());
        
        for (Map<String, Object> signatureEntry : signatureArray) {
            String protectedHeader = (String)signatureEntry.get("protected");
            Map<String, Object> header = CastUtils.cast((Map<?, ?>)signatureEntry.get("header"));
            String signature = (String)signatureEntry.get("signature");
            JwsJsonSignatureEntry signatureObject = 
                new JwsJsonSignatureEntry(encodedJwsPayload, 
                                          protectedHeader, 
                                          signature, 
                                          header != null ? new JwsJsonUnprotectedHeader(header) : null);
            this.signatureEntries.add(signatureObject);
        }
        if (signatureEntries.isEmpty()) {
            throw new SecurityException("Invalid JWS JSON sequence: no signatures are available");
        }
    }
    public String getSignedDocument() {
        return this.jwsSignedDocument;
    }
    public String getEncodedJwsPayload() {
        return this.encodedJwsPayload;
    }
    public String getDecodedJwsPayload() {
        return JoseUtils.decodeToString(encodedJwsPayload);
    }
    public byte[] getDecodedJwsPayloadBytes() {
        return StringUtils.toBytesUTF8(getDecodedJwsPayload());
    }
    public List<JwsJsonSignatureEntry> getSignatureEntries() {
        return Collections.unmodifiableList(signatureEntries);
    }
    public MultivaluedMap<String, JwsJsonSignatureEntry> getSignatureEntryMap() {
        return JwsUtils.getJwsJsonSignatureMap(signatureEntries);
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        List<JwsJsonSignatureEntry> theSignatureEntries = 
            getSignatureEntryMap().get(validator.getAlgorithm());
        if (theSignatureEntries != null) {
            for (JwsJsonSignatureEntry signatureEntry : theSignatureEntries) {
                if (signatureEntry.verifySignatureWith(validator)) {
                    return true;
                }
            }
        }
        return false;
    }
    public boolean verifySignatureWith(List<JwsSignatureVerifier> validators) {
        try {
            verifyAndGetNonValidated(validators);
            return true;
        } catch (SecurityException ex) {
            return false;
        }
    }
    public List<JwsJsonSignatureEntry> verifyAndGetNonValidated(List<JwsSignatureVerifier> validators) {
        if (validators.size() > signatureEntries.size()) {
            throw new SecurityException("Too many signature validators");
        }
        // TODO: more effective approach is needed
        List<JwsJsonSignatureEntry> validatedSignatures = new LinkedList<JwsJsonSignatureEntry>();
        for (JwsSignatureVerifier validator : validators) {
            boolean validated = false;
            List<JwsJsonSignatureEntry> theSignatureEntries = 
                getSignatureEntryMap().get(validator.getAlgorithm());
            if (theSignatureEntries != null) {
                for (JwsJsonSignatureEntry sigEntry : theSignatureEntries) {
                    if (sigEntry.verifySignatureWith(validator)) {     
                        validatedSignatures.add(sigEntry);
                        validated = true;
                        break;
                    }
                }
            }
            if (!validated) {
                throw new SecurityException();
            }
        }
        if (validatedSignatures.isEmpty()) {
            throw new SecurityException();
        }
        List<JwsJsonSignatureEntry> nonValidatedSignatures = new LinkedList<JwsJsonSignatureEntry>();
        for (JwsJsonSignatureEntry sigEntry : signatureEntries) {
            if (!validatedSignatures.contains(sigEntry)) {        
                nonValidatedSignatures.add(sigEntry);
            }
        }
        return nonValidatedSignatures;
    }
    
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    public JwsJsonProducer toProducer() {
        return new JwsJsonProducer(getDecodedJwsPayload(), signatureEntries);
    }
}

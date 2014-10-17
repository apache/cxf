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
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.StringUtils;
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
        this.encodedJwsPayload = (String)jsonObject.asMap().get("payload");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> signatureArray = 
            (List<Map<String, Object>>)jsonObject.asMap().get("signatures");
        
        this.signatureEntries = new ArrayList<JwsJsonSignatureEntry>(signatureArray.size());
        
        for (Map<String, Object> signatureEntry : signatureArray) {
            String protectedHeader = (String)signatureEntry.get("protected");
            @SuppressWarnings("unchecked")
            Map<String, Object> header = (Map<String, Object>)signatureEntry.get("header");
            String signature = (String)signatureEntry.get("signature");
            if (protectedHeader == null && header == null || signature == null) {
                throw new SecurityException("Invalid security entry");
            }
            JwsJsonSignatureEntry signatureObject = 
                new JwsJsonSignatureEntry(encodedJwsPayload, 
                                          protectedHeader, 
                                          signature, 
                                          new JwsJsonUnprotectedHeader(header));
            this.signatureEntries.add(signatureObject);
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
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        for (JwsJsonSignatureEntry signatureEntry : signatureEntries) {
            if (signatureEntry.verifySignatureWith(validator)) {
                return true;
            }
        }
        return false;
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    
}

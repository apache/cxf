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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
public class JwsJsonProducer {
    private String plainPayload;
    private String encodedPayload;
    private StringBuilder jwsJsonSignedDocBuilder = new StringBuilder();
    private List<JwsJsonSignatureEntry> signatures = new LinkedList<JwsJsonSignatureEntry>();
    public JwsJsonProducer(String tbsDocument) {
        this.plainPayload = tbsDocument;
        this.encodedPayload = Base64UrlUtility.encode(tbsDocument);
    }
    public JwsJsonProducer(String tbsDocument, List<JwsJsonSignatureEntry> signatures) {
        this(tbsDocument);
        for (JwsJsonSignatureEntry entry : signatures) {
            updateJwsJsonSignedDocument(entry);
        }
    }

    public String getPlainPayload() {
        return plainPayload;
    }
    public String getUnsignedEncodedPayload() {
        return encodedPayload;
    }

    public String getJwsJsonSignedDocument() {
        if (signatures.isEmpty()) { 
            throw new SecurityException("Signature is not available");
        }
        return jwsJsonSignedDocBuilder.toString() + "]}";
    }
    public List<JwsJsonSignatureEntry> getSignatureEntries() {
        return Collections.unmodifiableList(signatures);
    }
    public MultivaluedMap<String, JwsJsonSignatureEntry> getSignatureEntryMap() {
        return JwsUtils.getJwsJsonSignatureMap(signatures);
    }
    public String signWith(List<JwsSignatureProvider> signers) {
        for (JwsSignatureProvider signer : signers) {
            signWith(signer);    
        }
        return getJwsJsonSignedDocument(); 
    }
    public String signWith(JwsSignatureProvider signer) {
        JoseHeaders headers = new JoseHeaders();
        headers.setAlgorithm(signer.getAlgorithm());
        return signWith(signer, new JwsJsonProtectedHeader(headers));
    }
    public String signWith(JwsSignatureProvider signer, 
                           JwsJsonProtectedHeader protectedHeader) {
        return signWith(signer, protectedHeader, null);
    }
    public String signWith(JsonWebKey jwk) {
        return signWith(JwsUtils.getSignatureProvider(jwk));
    }
    public String signWith(JwsSignatureProvider signer,
                           JwsJsonProtectedHeader protectedHeader,
                           JwsJsonUnprotectedHeader unprotectedHeader) {
        JoseHeaders unionHeaders = new JoseHeaders();
         
        if (protectedHeader != null) {
            unionHeaders.asMap().putAll(protectedHeader.getHeaderEntries().asMap());
        }
        if (unprotectedHeader != null) {
            if (!Collections.disjoint(unionHeaders.asMap().keySet(), 
                                     unprotectedHeader.getHeaderEntries().asMap().keySet())) {
                throw new SecurityException("Protected and unprotected headers have duplicate values");
            }
            unionHeaders.asMap().putAll(unprotectedHeader.getHeaderEntries().asMap());
        }
        if (unionHeaders.getAlgorithm() == null) {
            throw new SecurityException("Algorithm header is not set");
        }
        JwsSignature worker = signer.createJwsSignature(unionHeaders);
        String sequenceToBeSigned = protectedHeader.getEncodedHeaderEntries() 
            + "." + getUnsignedEncodedPayload();
        byte[] bytesToBeSigned = StringUtils.toBytesUTF8(sequenceToBeSigned);
        worker.update(bytesToBeSigned, 0, bytesToBeSigned.length);
        byte[] signatureBytes = worker.sign();
        String encodedSignatureBytes = Base64UrlUtility.encode(signatureBytes);
        JwsJsonSignatureEntry signature = 
            new JwsJsonSignatureEntry(encodedPayload, 
                                      protectedHeader.getEncodedHeaderEntries(),
                                      encodedSignatureBytes,
                                      unprotectedHeader); 
        return updateJwsJsonSignedDocument(signature);
    }
    
    private String updateJwsJsonSignedDocument(JwsJsonSignatureEntry signature) {
        if (signatures.isEmpty()) {
            jwsJsonSignedDocBuilder.append("{\"payload\":\"" + encodedPayload + "\"");
            jwsJsonSignedDocBuilder.append(",\"signatures\":[");
        } else {
            jwsJsonSignedDocBuilder.append(",");    
        }
        jwsJsonSignedDocBuilder.append(signature.toJson());
        signatures.add(signature);
        
        return getJwsJsonSignedDocument();
    }
}

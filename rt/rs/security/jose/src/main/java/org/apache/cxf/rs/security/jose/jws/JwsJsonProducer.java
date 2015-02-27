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

import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
public class JwsJsonProducer {
    private boolean supportFlattened;
    private String plainPayload;
    private String encodedPayload;
    private List<JwsJsonSignatureEntry> signatures = new LinkedList<JwsJsonSignatureEntry>();
    private JoseHeadersReaderWriter writer = new JoseHeadersReaderWriter();
    public JwsJsonProducer(String tbsDocument) {
        this(tbsDocument, false);
    }
    public JwsJsonProducer(String tbsDocument, boolean supportFlattened) {
        this.supportFlattened = supportFlattened;
        this.plainPayload = tbsDocument;
        this.encodedPayload = Base64UrlUtility.encode(tbsDocument);
    }
    
    public String getPlainPayload() {
        return plainPayload;
    }
    public String getUnsignedEncodedPayload() {
        return encodedPayload;
    }
    public String getJwsJsonSignedDocument() {
        return getJwsJsonSignedDocument(false);
    }
    public String getJwsJsonSignedDocument(boolean detached) {
        if (signatures.isEmpty()) { 
            throw new SecurityException("Signature is not available");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (!detached) {
            sb.append("\"payload\":\"" + encodedPayload + "\"");
            sb.append(",");
        }
        if (!supportFlattened || signatures.size() > 1) {
            sb.append("\"signatures\":[");
            for (int i = 0; i < signatures.size(); i++) {
                JwsJsonSignatureEntry signature = signatures.get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(signature.toJson());
            }
            sb.append("]");
        } else {
            sb.append(signatures.get(0).toJson(true));
        }
        sb.append("}");
        return sb.toString();
    }
    public List<JwsJsonSignatureEntry> getSignatureEntries() {
        return signatures;
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
        headers.setAlgorithm(signer.getAlgorithm().getJwaName());
        return signWith(signer, headers);
    }
    public String signWith(JwsSignatureProvider signer, 
                           JoseHeaders protectedHeader) {
        return signWith(signer, protectedHeader, null);
    }
    public String signWith(JsonWebKey jwk) {
        return signWith(JwsUtils.getSignatureProvider(jwk));
    }
    public String signWith(RSAPrivateKey key, String algo) {
        return signWith(JwsUtils.getRSAKeySignatureProvider(key, algo));
    }
    public String signWith(byte[] key, String algo) {
        return signWith(JwsUtils.getHmacSignatureProvider(key, algo));
    }
    public String signWith(JwsSignatureProvider signer,
                           JoseHeaders protectedHeader,
                           JoseHeaders unprotectedHeader) {
        JoseHeaders unionHeaders = new JoseHeaders();
         
        if (protectedHeader != null) {
            unionHeaders.asMap().putAll(protectedHeader.asMap());
        }
        if (unprotectedHeader != null) {
            checkCriticalHeaders(unprotectedHeader);
            if (!Collections.disjoint(unionHeaders.asMap().keySet(), 
                                     unprotectedHeader.asMap().keySet())) {
                throw new SecurityException("Protected and unprotected headers have duplicate values");
            }
            unionHeaders.asMap().putAll(unprotectedHeader.asMap());
        }
        if (unionHeaders.getAlgorithm() == null) {
            throw new SecurityException("Algorithm header is not set");
        }
        String sequenceToBeSigned;
        if (protectedHeader != null) {
            sequenceToBeSigned = Base64UrlUtility.encode(writer.toJson(protectedHeader))
                    + "." + getUnsignedEncodedPayload();
        } else {
            sequenceToBeSigned = "." + getUnsignedEncodedPayload();
        }
        byte[] bytesToBeSigned = StringUtils.toBytesUTF8(sequenceToBeSigned);
        
        byte[] signatureBytes = signer.sign(unionHeaders, bytesToBeSigned);
        
        String encodedSignatureBytes = Base64UrlUtility.encode(signatureBytes);
        JwsJsonSignatureEntry signature;
        if (protectedHeader != null) {
            signature = new JwsJsonSignatureEntry(encodedPayload,
                    Base64UrlUtility.encode(writer.toJson(protectedHeader)),
                    encodedSignatureBytes,
                    unprotectedHeader);
        } else {
            signature = new JwsJsonSignatureEntry(encodedPayload,
                    null,
                    encodedSignatureBytes,
                    unprotectedHeader);
        }
        return updateJwsJsonSignedDocument(signature);
    }
    private String updateJwsJsonSignedDocument(JwsJsonSignatureEntry signature) {
        signatures.add(signature);
        return getJwsJsonSignedDocument();
    }
    private static void checkCriticalHeaders(JoseHeaders unprotected) {
        if (unprotected.asMap().containsKey(JoseConstants.HEADER_CRITICAL)) {
            throw new SecurityException();
        }
    }
}

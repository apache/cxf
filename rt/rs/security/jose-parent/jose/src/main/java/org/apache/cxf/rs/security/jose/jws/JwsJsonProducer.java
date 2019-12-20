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

import java.security.PrivateKey;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;

public class JwsJsonProducer {
    protected static final Logger LOG = LogUtils.getL7dLogger(JwsJsonProducer.class);
    private final boolean supportFlattened;
    private final boolean supportDetached;
    private final String plainPayload;
    private String encodedPayload;
    private final List<JwsJsonSignatureEntry> signatures = new LinkedList<>();
    private final JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();

    public JwsJsonProducer(String tbsDocument) {
        this(tbsDocument, false);
    }
    public JwsJsonProducer(String tbsDocument, boolean supportFlattened) {
        this(tbsDocument, supportFlattened, false);
    }

    public JwsJsonProducer(String tbsDocument, boolean supportFlattened, boolean supportDetached) {
        this.plainPayload = tbsDocument;
        this.supportFlattened = supportFlattened;
        this.supportDetached = supportDetached;
    }

    public String getPlainPayload() {
        return plainPayload;
    }
    public String getUnsignedEncodedPayload() {
        if (encodedPayload == null) {
            encodedPayload = Base64UrlUtility.encode(getPlainPayload());
        }
        return encodedPayload;
    }
    public String getJwsJsonSignedDocument() {
        return doGetJwsJsonSignedDocument(supportDetached);
    }
    @Deprecated
    public String getJwsJsonSignedDocument(boolean detached) {
        return doGetJwsJsonSignedDocument(detached);
    }
    private String doGetJwsJsonSignedDocument(boolean detached) {
        if (signatures.isEmpty()) {
            return null;
        }

        Boolean b64Status = validateB64Status(signatures);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (!detached) {
            sb.append("\"payload\":\"").append(getActualPayload(b64Status)).append('"');
            sb.append(',');
        }
        if (!supportFlattened || signatures.size() > 1) {
            sb.append("\"signatures\":[");
            for (int i = 0; i < signatures.size(); i++) {
                JwsJsonSignatureEntry signature = signatures.get(i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(signature.toJson());
            }
            sb.append(']');
        } else {
            sb.append(signatures.get(0).toJson(true));
        }
        sb.append('}');
        return sb.toString();
    }
    public List<JwsJsonSignatureEntry> getSignatureEntries() {
        return signatures;
    }

    public String signWith(List<JwsSignatureProvider> signers) {
        for (JwsSignatureProvider signer : signers) {
            signWith(signer);
        }
        return getJwsJsonSignedDocument();
    }
    public String signWith(JwsSignatureProvider signer) {
        JwsHeaders headers = new JwsHeaders();
        headers.setSignatureAlgorithm(signer.getAlgorithm());
        return signWith(signer, headers);
    }
    public String signWith(JwsSignatureProvider signer,
                           JwsHeaders protectedHeader) {
        return signWith(signer, protectedHeader, null);
    }
    public String signWith(JsonWebKey jwk) {
        return signWith(JwsUtils.getSignatureProvider(jwk));
    }
    public String signWith(PrivateKey key, SignatureAlgorithm algo) {
        return signWith(JwsUtils.getPrivateKeySignatureProvider(key, algo));
    }
    public String signWith(byte[] key, SignatureAlgorithm algo) {
        return signWith(JwsUtils.getHmacSignatureProvider(key, algo));
    }
    public String signWith(JwsSignatureProvider signer,
                           JwsHeaders protectedHeader,
                           JwsHeaders unprotectedHeader) {
        JwsHeaders unionHeaders = new JwsHeaders();

        if (protectedHeader != null) {
            unionHeaders.asMap().putAll(protectedHeader.asMap());
        }
        if (unprotectedHeader != null) {
            checkUnprotectedHeaders(unprotectedHeader,
                                    JoseConstants.HEADER_CRITICAL,
                                    JoseConstants.JWS_HEADER_B64_STATUS_HEADER);
            if (!Collections.disjoint(unionHeaders.asMap().keySet(),
                                     unprotectedHeader.asMap().keySet())) {
                LOG.warning("Protected and unprotected headers have duplicate values");
                throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
            }
            unionHeaders.asMap().putAll(unprotectedHeader.asMap());
        }
        if (unionHeaders.getSignatureAlgorithm() == null) {
            LOG.warning("Algorithm header is not set");
            throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
        }
        String sequenceToBeSigned;
        String actualPayload = protectedHeader != null
            ? getActualPayload(protectedHeader.getPayloadEncodingStatus())
            : getUnsignedEncodedPayload();
        if (protectedHeader != null) {
            sequenceToBeSigned = Base64UrlUtility.encode(writer.toJson(protectedHeader))
                    + "." + actualPayload;
        } else {
            sequenceToBeSigned = "." + getUnsignedEncodedPayload();
        }
        byte[] bytesToBeSigned = StringUtils.toBytesUTF8(sequenceToBeSigned);

        byte[] signatureBytes = signer.sign(unionHeaders, bytesToBeSigned);

        String encodedSignatureBytes = Base64UrlUtility.encode(signatureBytes);
        JwsJsonSignatureEntry signature;
        if (protectedHeader != null) {
            signature = new JwsJsonSignatureEntry(actualPayload,
                    Base64UrlUtility.encode(writer.toJson(protectedHeader)),
                    encodedSignatureBytes,
                    unprotectedHeader);
        } else {
            signature = new JwsJsonSignatureEntry(getUnsignedEncodedPayload(),
                    null,
                    encodedSignatureBytes,
                    unprotectedHeader);
        }
        return updateJwsJsonSignedDocument(signature);
    }
    private String getActualPayload(Boolean payloadEncodingStatus) {
        return Boolean.FALSE == payloadEncodingStatus
            ? getPlainPayload() : this.getUnsignedEncodedPayload();
    }
    private String updateJwsJsonSignedDocument(JwsJsonSignatureEntry signature) {
        signatures.add(signature);
        return getJwsJsonSignedDocument();
    }
    private static void checkUnprotectedHeaders(JoseHeaders unprotected, String... headerNames) {
        for (String headerName : headerNames) {
            if (unprotected.containsHeader(headerName)) {
                LOG.warning("Unprotected headers contain a header \""
                    + headerName + "\" which must be protected");
                throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
            }
        }
    }
    static Boolean validateB64Status(List<JwsJsonSignatureEntry> signatures) {
        Set<Boolean> b64Set = new LinkedHashSet<>();
        for (JwsJsonSignatureEntry entry : signatures) {
            JwsHeaders headers = entry.getProtectedHeader();
            Boolean status = headers != null ? headers.getPayloadEncodingStatus() : null;
            if (status == null) {
                status = Boolean.TRUE;
            }
            b64Set.add(status);
        }
        if (b64Set.size() > 1) {
            LOG.warning("Each signature entry can sign only encoded or only unencoded payload");
            throw new JwsException(JwsException.Error.INVALID_JSON_JWS);
        }
        return b64Set.iterator().next();
    }
}

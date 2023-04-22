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
package org.apache.cxf.rs.security.jose.jaxrs.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartInputFilter;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.common.KeyManagementUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsInputStream;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jws.JwsVerificationSignature;

public class JwsMultipartSignatureInFilter implements MultipartInputFilter {
    private JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
    private JwsSignatureVerifier verifier;
    private boolean bufferPayload;
    private Message message;
    private boolean useJwsJsonSignatureFormat;
    public JwsMultipartSignatureInFilter(Message message, 
                                         JwsSignatureVerifier verifier, 
                                         boolean bufferPayload,
                                         boolean useJwsJsonSignatureFormat) {
        this.message = message;
        this.verifier = verifier;
        this.bufferPayload = bufferPayload;
        this.useJwsJsonSignatureFormat = useJwsJsonSignatureFormat;
    }
    
    @Override
    public void filter(List<Attachment> atts) {
        if (atts.size() < 2) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        Attachment sigPart = atts.remove(atts.size() - 1);
        
        final String jwsSequence;
        try {
            jwsSequence = IOUtils.readStringFromStream(sigPart.getDataHandler().getInputStream());
        } catch (IOException ex) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        
        final String base64UrlEncodedHeaders;
        final String base64UrlEncodedSignature;
        
        if (!useJwsJsonSignatureFormat) {
            String[] parts = JoseUtils.getCompactParts(jwsSequence);
            if (parts.length != 3 || !parts[1].isEmpty()) {
                throw ExceptionUtils.toBadRequestException(null, null);
            }
            base64UrlEncodedHeaders = parts[0];
            base64UrlEncodedSignature = parts[2];
        } else {
            Map<String, Object> parts = reader.fromJson(jwsSequence);
            if (parts.size() != 2 || !parts.containsKey("protected") || !parts.containsKey("signature")) {
                throw ExceptionUtils.toBadRequestException(null, null);
            }
            base64UrlEncodedHeaders = (String)parts.get("protected");
            base64UrlEncodedSignature = (String)parts.get("signature");
        }
        
        JwsHeaders headers = new JwsHeaders(
                                 new JsonMapObjectReaderWriter().fromJson(
                                     JoseUtils.decodeToString(base64UrlEncodedHeaders)));
        JoseUtils.traceHeaders(headers);
        if (Boolean.FALSE != headers.getPayloadEncodingStatus()) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        final JwsSignatureVerifier theVerifier;
        if (verifier == null) {
            Properties props = KeyManagementUtils.loadStoreProperties(message, true,
                                                   JoseConstants.RSSEC_SIGNATURE_IN_PROPS,
                                                   JoseConstants.RSSEC_SIGNATURE_PROPS);
            
            theVerifier = JwsUtils.loadSignatureVerifier(message, props, headers);
        } else {
            theVerifier = verifier;
        }
        
        
        JwsVerificationSignature sig = theVerifier.createJwsVerificationSignature(headers);
        if (sig == null) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        byte[] signatureBytes = JoseUtils.decode(base64UrlEncodedSignature);
        
        byte[] headerBytesWithDot = StringUtils.toBytesASCII(base64UrlEncodedHeaders + '.');
        sig.update(headerBytesWithDot, 0, headerBytesWithDot.length);
        
        int attSize = atts.size();
        for (int i = 0; i < attSize; i++) {
            Attachment dataPart = atts.get(i);
            final InputStream dataPartStream;
            try {
                dataPartStream = dataPart.getDataHandler().getDataSource().getInputStream();
            } catch (IOException ex) {
                throw ExceptionUtils.toBadRequestException(ex, null);
            }
            boolean verifyOnLastRead = i == attSize - 1;
            JwsInputStream jwsStream = new JwsInputStream(dataPartStream, sig, signatureBytes, verifyOnLastRead);
            
            final InputStream newStream;
            if (bufferPayload) {
                CachedOutputStream cos = new CachedOutputStream();
                try {
                    IOUtils.copy(jwsStream, cos);
                    newStream = cos.getInputStream();
                } catch (Exception ex) {
                    throw ExceptionUtils.toBadRequestException(ex, null);
                }
            } else {
                newStream = jwsStream;
            }
            Attachment newDataPart = new Attachment(newStream, dataPart.getHeaders());
            atts.set(i, newDataPart);
        }
    }
}

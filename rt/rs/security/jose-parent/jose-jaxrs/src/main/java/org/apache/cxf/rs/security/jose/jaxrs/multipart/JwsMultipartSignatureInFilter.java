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

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartInputFilter;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsInputStream;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jws.JwsVerificationSignature;

public class JwsMultipartSignatureInFilter implements MultipartInputFilter {

    private JwsSignatureVerifier verifier;
    private boolean supportSinglePartOnly;
    
    public JwsMultipartSignatureInFilter(boolean supportSinglePartOnly) {
        this(null, supportSinglePartOnly);
    }
    
    public JwsMultipartSignatureInFilter(JwsSignatureVerifier verifier, boolean supportSinglePartOnly) {
        this.verifier = verifier;
        this.supportSinglePartOnly = supportSinglePartOnly;
    }
    
    @Override
    public void filter(List<Attachment> atts) {
        if (atts.size() < 2 || supportSinglePartOnly && atts.size() > 2) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        Attachment sigPart = atts.remove(atts.size() - 1);
        
        String encodedJws = null;
        try {
            encodedJws = IOUtils.readStringFromStream(sigPart.getDataHandler().getInputStream());
        } catch (IOException ex) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        String[] parts = JoseUtils.getCompactParts(encodedJws);
        // Detached signature
        if (parts.length != 3 || parts[1].length() > 0) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        JwsHeaders headers = new JwsHeaders(
                                 new JsonMapObjectReaderWriter().fromJson(
                                     JoseUtils.decodeToString(parts[0])));
        JwsSignatureVerifier theVerifier = 
            verifier == null ? JwsUtils.loadSignatureVerifier(headers, true) : verifier;
        
        JwsVerificationSignature sig = theVerifier.createJwsVerificationSignature(headers);
        if (sig == null) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        byte[] signatureBytes = JoseUtils.decode(parts[2]);
        
        int attSize = atts.size();
        for (int i = 0; i < attSize; i++) {
            Attachment dataPart = atts.remove(i);
            InputStream dataPartStream = null;
            try {
                dataPartStream = dataPart.getDataHandler().getDataSource().getInputStream();
            } catch (IOException ex) {
                throw ExceptionUtils.toBadRequestException(ex, null);
            }
            boolean verifyOnLastRead = i == attSize - 1 ? true : false;
            JwsInputStream jwsStream = new JwsInputStream(dataPartStream, sig, signatureBytes, verifyOnLastRead);
            Attachment newDataPart = new Attachment(jwsStream, dataPart.getHeaders());
            atts.add(i, newDataPart);
        }
    }
}

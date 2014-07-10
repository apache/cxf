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
package org.apache.cxf.rs.security.oauth2.jws;

import java.io.OutputStream;
import java.util.Set;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;

public abstract class AbstractJwsSignatureProvider implements JwsSignatureProvider {
    private Set<String> supportedAlgorithms;
    private String defaultJwtAlgorithm;
    
    protected AbstractJwsSignatureProvider(Set<String> supportedAlgorithms) {
        this.supportedAlgorithms = supportedAlgorithms;
    }
    @Override
    public JwtHeaders prepareHeaders(JwtHeaders headers) {
        if (headers == null) {
            headers = new JwtHeaders();
        }
        String algo = headers.getAlgorithm();
        if (algo != null) {
            checkAlgorithm(algo);
        } else {
            headers.setAlgorithm(defaultJwtAlgorithm);
        }
        return headers;
    }
    
    @Override
    public JwsOutputStream createJwsStream(OutputStream os, String contentType) {
        JwtHeaders headers = new JwtHeaders();
        if (contentType != null) {
            headers.setContentType(contentType);
        }
        headers = prepareHeaders(headers);
        JwsSignatureProviderWorker worker = createJwsSignatureWorker(headers);
        try {
            byte[] headerBytes = new JwtTokenReaderWriter().headersToJson(headers).getBytes("UTF-8");
            Base64Utility.encodeAndStream(headerBytes, 0, headerBytes.length, os);
            os.write(new byte[]{'.'});
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
        return new JwsOutputStream(os, worker);
    }
    
    protected abstract JwsSignatureProviderWorker createJwsSignatureWorker(JwtHeaders headers);
    
    public void setDefaultJwtAlgorithm(String algo) {
        this.defaultJwtAlgorithm = algo;
    }
    protected void checkAlgorithm(String algo) {
        if (algo == null || !supportedAlgorithms.contains(algo)) {
            throw new SecurityException();
        }
    }

}

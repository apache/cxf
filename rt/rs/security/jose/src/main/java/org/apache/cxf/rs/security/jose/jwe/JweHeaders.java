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

package org.apache.cxf.rs.security.jose.jwe;

import java.util.Map;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;




public class JweHeaders extends JoseHeaders {
    private JweHeaders protectedHeaders;
    public JweHeaders() {
    }
    
    public JweHeaders(JoseHeaders headers) {
        super(headers.asMap());
    }
    
    public JweHeaders(Map<String, Object> values) {
        super(values);
    }
    public JweHeaders(String keyEncAlgo, String ctEncAlgo) {
        this(keyEncAlgo, ctEncAlgo, false);
    }
    public JweHeaders(String ctEncAlgo) {
        this(null, ctEncAlgo, false);
    }
    public JweHeaders(String ctEncAlgo, boolean deflate) {
        this(null, ctEncAlgo, deflate);
    }
    public JweHeaders(String keyEncAlgo, String ctEncAlgo, boolean deflate) {
        init(keyEncAlgo, ctEncAlgo, deflate);
    }
    private void init(String keyEncAlgo, String ctEncAlgo, boolean deflate) {
        if (keyEncAlgo != null) {
            setKeyEncryptionAlgorithm(keyEncAlgo);    
        }
        setContentEncryptionAlgorithm(ctEncAlgo);
        if (deflate) {
            setZipAlgorithm(JoseConstants.DEFLATE_ZIP_ALGORITHM);
        }
    }
    
    public void setKeyEncryptionAlgorithm(String type) {
        super.setAlgorithm(type);
    }
    
    public String getKeyEncryptionAlgorithm() {
        return super.getAlgorithm();
    }
    
    public void setContentEncryptionAlgorithm(String type) {
        setHeader(JoseConstants.JWE_HEADER_CONTENT_ENC_ALGORITHM, type);
    }
    
    public String getContentEncryptionAlgorithm() {
        return (String)getHeader(JoseConstants.JWE_HEADER_CONTENT_ENC_ALGORITHM);
    }
    
    public void setZipAlgorithm(String type) {
        setHeader(JoseConstants.JWE_HEADER_ZIP_ALGORITHM, type);
    }
    
    public String getZipAlgorithm() {
        return (String)getHeader(JoseConstants.JWE_HEADER_ZIP_ALGORITHM);
    }
    
    @Override
    public JoseHeaders setHeader(String name, Object value) {
        return (JoseHeaders)super.setHeader(name, value);
    }
    public byte[] toCipherAdditionalAuthData() { 
        return toCipherAdditionalAuthData(new JoseHeadersReaderWriter().headersToJson(this));
    }
    public static byte[] toCipherAdditionalAuthData(String headersJson) { 
        byte[] headerBytes = StringUtils.toBytesUTF8(headersJson);
        String base64UrlHeadersInJson = Base64UrlUtility.encode(headerBytes);
        return StringUtils.toBytesASCII(base64UrlHeadersInJson);
    }

    public JweHeaders getProtectedHeaders() {
        return protectedHeaders;
    }

    public void setProtectedHeaders(JweHeaders protectedHeaders) {
        this.protectedHeaders = protectedHeaders;
    }
}

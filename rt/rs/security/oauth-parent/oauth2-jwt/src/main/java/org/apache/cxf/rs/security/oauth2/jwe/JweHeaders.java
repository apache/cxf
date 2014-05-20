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

package org.apache.cxf.rs.security.oauth2.jwe;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeaders;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;




public class JweHeaders extends JwtHeaders {
    
    public JweHeaders() {
    }
    
    public JweHeaders(Map<String, Object> values) {
        super(values);
    }
    public JweHeaders(String keyEncAlgo, String ctEncAlgo) {
        this(keyEncAlgo, ctEncAlgo, false);
    }
    public JweHeaders(String keyEncAlgo, String ctEncAlgo, boolean deflate) {
        init(keyEncAlgo, ctEncAlgo, deflate);
    }
    
    private void init(String keyEncAlgo, String ctEncAlgo, boolean deflate) {
        setKeyEncryptionAlgorithm(keyEncAlgo);
        setContentEncryptionAlgorithm(ctEncAlgo);
        if (deflate) {
            setZipAlgorithm(JwtConstants.DEFLATE_ZIP_ALGORITHM);
        }
    }
    
    public void setKeyEncryptionAlgorithm(String type) {
        super.setAlgorithm(type);
    }
    
    public String getKeyEncryptionAlgorithm() {
        return super.getAlgorithm();
    }
    
    public void setContentEncryptionAlgorithm(String type) {
        setHeader(JwtConstants.JWE_HEADER_CONTENT_ENC_ALGORITHM, type);
    }
    
    public String getContentEncryptionAlgorithm() {
        return (String)getHeader(JwtConstants.JWE_HEADER_CONTENT_ENC_ALGORITHM);
    }
    
    public void setZipAlgorithm(String type) {
        setHeader(JwtConstants.JWE_HEADER_ZIP_ALGORITHM, type);
    }
    
    public String getZipAlgorithm() {
        return (String)getHeader(JwtConstants.JWE_HEADER_ZIP_ALGORITHM);
    }
    
    @Override
    public JwtHeaders setHeader(String name, Object value) {
        return (JwtHeaders)super.setHeader(name, value);
    }
    
    public byte[] toCipherAdditionalAuthData(JwtHeadersWriter writer) { 
        return toCipherAdditionalAuthData(writer.headersToJson(this));
    }
    public static byte[] toCipherAdditionalAuthData(String headersJson) { 
        try {
            String base64UrlHeadersInJson = Base64UrlUtility.encode(headersJson.getBytes("UTF-8"));
            return base64UrlHeadersInJson.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
}

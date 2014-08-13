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

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.jwt.Algorithm;
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersWriter;
import org.apache.cxf.rs.security.oauth2.jwt.JwtTokenReaderWriter;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.KeyProperties;

public abstract class AbstractJweEncryption implements JweEncryptionProvider {
    protected static final int DEFAULT_AUTH_TAG_LENGTH = 128;
    private JweHeaders headers;
    private JwtHeadersWriter writer;
    private ContentEncryptionAlgorithm contentEncryptionAlgo;
    private KeyEncryptionAlgorithm keyEncryptionAlgo;
    
    protected AbstractJweEncryption(JweHeaders headers, 
                                    ContentEncryptionAlgorithm contentEncryptionAlgo,
                                    KeyEncryptionAlgorithm keyEncryptionAlgo) {
        this(headers, contentEncryptionAlgo, keyEncryptionAlgo, null);
    }
    protected AbstractJweEncryption(JweHeaders headers, 
                                    ContentEncryptionAlgorithm contentEncryptionAlgo, 
                                    KeyEncryptionAlgorithm keyEncryptionAlgo,
                                    JwtHeadersWriter writer) {
        this.headers = headers;
        this.writer = writer;
        if (this.writer == null) {
            this.writer = new JwtTokenReaderWriter();
        }
        this.keyEncryptionAlgo = keyEncryptionAlgo;
        this.contentEncryptionAlgo = contentEncryptionAlgo;
    }
    
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
        return contentEncryptionAlgo.getAlgorithmParameterSpec(theIv);
    }
    
    protected byte[] getContentEncryptionKey() {
        byte[] cek = getProvidedContentEncryptionKey();
        if (cek == null) {
            String algoJava = getContentEncryptionAlgoJava();
            String algoJwt = getContentEncryptionAlgoJwt();
            cek = CryptoUtils.getSecretKey(Algorithm.stripAlgoProperties(algoJava), 
                                           getCekSize(algoJwt)).getEncoded();
        }
        return cek;
    }
   
    protected int getCekSize(String algoJwt) {
        return Algorithm.valueOf(algoJwt.replace('-', '_')).getKeySizeBits();
    }
    
    protected byte[] getProvidedContentEncryptionKey() {
        return contentEncryptionAlgo.getContentEncryptionKey(headers);
    }
    
    protected byte[] getEncryptedContentEncryptionKey(byte[] theCek) {
        return keyEncryptionAlgo.getEncryptedContentEncryptionKey(headers, theCek);
    }
    
    protected String getContentEncryptionAlgoJwt() {
        return headers.getContentEncryptionAlgorithm();
    }
    protected String getContentEncryptionAlgoJava() {
        return Algorithm.toJavaName(getContentEncryptionAlgoJwt());
    }
    protected byte[] getAAD(JweHeaders theHeaders) {
        return contentEncryptionAlgo.getAdditionalAuthenticationData(writer.headersToJson(theHeaders));
    }
    public String encrypt(byte[] content, String contentType) {
        JweEncryptionInternal state = getInternalState(contentType);
        
        byte[] cipher = CryptoUtils.encryptBytes(content, createCekSecretKey(state), state.keyProps);
        
        
        JweCompactProducer producer = getJweCompactProducer(state, cipher);
        return producer.getJweContent();
    }
    
    protected JweCompactProducer getJweCompactProducer(JweEncryptionInternal state, byte[] cipher) {
        return new JweCompactProducer(state.theHeaders, 
                                      getJwtHeadersWriter(),                
                                      state.jweContentEncryptionKey,
                                      state.theIv,
                                      cipher,
                                      DEFAULT_AUTH_TAG_LENGTH);
    }
    
    protected JwtHeadersWriter getJwtHeadersWriter() {
        return writer;
    }
    protected JweHeaders getJweHeaders() {
        return headers;
    }
    @Override
    public JweEncryptionState createJweEncryptionState(String contentType) {
        JweEncryptionInternal state = getInternalState(contentType);
        Cipher c = CryptoUtils.initCipher(createCekSecretKey(state), state.keyProps, 
                                          Cipher.ENCRYPT_MODE);
        return new JweEncryptionState(c, 
                                      state.theHeaders, 
                                      state.jweContentEncryptionKey, 
                                      state.theIv,
                                      getAuthenticationTagProducer(state),
                                      state.keyProps.isCompressionSupported());
    }
    protected AuthenticationTagProducer getAuthenticationTagProducer(JweEncryptionInternal state) {
        return null;
    }
    protected SecretKey createCekSecretKey(JweEncryptionInternal state) {
        return CryptoUtils.createSecretKeySpec(getActualCek(state.secretKey, this.getContentEncryptionAlgoJwt()), 
                                               state.keyProps.getKeyAlgo());
    }
    
    protected byte[] getActualCek(byte[] theCek, String algoJwt) {
        return theCek;
    }
    
    private JweEncryptionInternal getInternalState(String contentType) {
        JweHeaders theHeaders = headers;
        if (contentType != null) {
            theHeaders = new JweHeaders(theHeaders.asMap());
            theHeaders.setContentType(contentType);
        }
        
        byte[] theCek = getContentEncryptionKey();
        String contentEncryptionAlgoJavaName = Algorithm.toJavaName(theHeaders.getContentEncryptionAlgorithm());
        KeyProperties keyProps = new KeyProperties(contentEncryptionAlgoJavaName);
        keyProps.setCompressionSupported(compressionRequired(theHeaders));
        byte[] additionalEncryptionParam = getAAD(theHeaders);
        keyProps.setAdditionalData(additionalEncryptionParam);
        
        byte[] theIv = contentEncryptionAlgo.getInitVector();
        AlgorithmParameterSpec specParams = getAlgorithmParameterSpec(theIv);
        keyProps.setAlgoSpec(specParams);
        byte[] jweContentEncryptionKey = getEncryptedContentEncryptionKey(theCek);
        JweEncryptionInternal state = new JweEncryptionInternal();
        state.theHeaders = theHeaders;
        state.jweContentEncryptionKey = jweContentEncryptionKey;
        state.keyProps = keyProps;
        state.secretKey = theCek; 
        state.theIv = theIv;
        return state;
    }
    private boolean compressionRequired(JweHeaders theHeaders) {
        return JwtConstants.DEFLATE_ZIP_ALGORITHM.equals(theHeaders.getZipAlgorithm());
    }
    protected KeyEncryptionAlgorithm getKeyEncryptionAlgo() {
        return keyEncryptionAlgo;
    }
    protected static class JweEncryptionInternal {
        JweHeaders theHeaders;
        byte[] jweContentEncryptionKey;
        byte[] theIv;
        KeyProperties keyProps;
        byte[] secretKey;
    }
}

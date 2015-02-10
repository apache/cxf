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

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.apache.cxf.common.util.crypto.CryptoUtils;
import org.apache.cxf.common.util.crypto.KeyProperties;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;

public abstract class AbstractJweEncryption implements JweEncryptionProvider {
    protected static final int DEFAULT_AUTH_TAG_LENGTH = 128;
    private ContentEncryptionAlgorithm contentEncryptionAlgo;
    private KeyEncryptionAlgorithm keyEncryptionAlgo;
    private JoseHeadersReaderWriter writer = new JoseHeadersReaderWriter();
    protected AbstractJweEncryption(ContentEncryptionAlgorithm contentEncryptionAlgo,
                                    KeyEncryptionAlgorithm keyEncryptionAlgo) {
        this.keyEncryptionAlgo = keyEncryptionAlgo;
        this.contentEncryptionAlgo = contentEncryptionAlgo;
    }
    protected ContentEncryptionAlgorithm getContentEncryptionAlgorithm() {
        return contentEncryptionAlgo;
    }
    protected AlgorithmParameterSpec getAlgorithmParameterSpec(byte[] theIv) {
        return getContentEncryptionAlgorithm().getAlgorithmParameterSpec(theIv);
    }
    
    protected byte[] getContentEncryptionKey(JweHeaders headers) {
        byte[] cek = getProvidedContentEncryptionKey(headers);
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
    
    protected byte[] getProvidedContentEncryptionKey(JweHeaders headers) {
        return getContentEncryptionAlgorithm().getContentEncryptionKey(headers);
    }
    
    protected byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] theCek) {
        return getKeyEncryptionAlgo().getEncryptedContentEncryptionKey(headers, theCek);
    }
    
    protected String getContentEncryptionAlgoJwt() {
        return getContentEncryptionAlgorithm().getAlgorithm();
    }
    protected String getContentEncryptionAlgoJava() {
        return Algorithm.toJavaName(getContentEncryptionAlgoJwt());
    }
    protected byte[] getAAD(String protectedHeaders, byte[] aad) {
        return getContentEncryptionAlgorithm().getAdditionalAuthenticationData(protectedHeaders, aad);
    }
    public String encrypt(byte[] content, JweHeaders jweHeaders) {
        JweEncryptionInternal state = getInternalState(jweHeaders, null);
        
        byte[] cipher = CryptoUtils.encryptBytes(content, createCekSecretKey(state), state.keyProps);
        
        
        JweCompactProducer producer = getJweCompactProducer(state, cipher);
        return producer.getJweContent();
    }
    
    protected JweCompactProducer getJweCompactProducer(JweEncryptionInternal state, byte[] cipher) {
        return new JweCompactProducer(state.theHeaders, 
                                      state.jweContentEncryptionKey,
                                      state.theIv,
                                      cipher,
                                      DEFAULT_AUTH_TAG_LENGTH);
    }
    @Override
    public String getKeyAlgorithm() {
        return getKeyEncryptionAlgo().getAlgorithm();
    }
    @Override
    public String getContentAlgorithm() {
        return getContentEncryptionAlgorithm().getAlgorithm();
    }
    protected JoseHeadersReaderWriter getJwtHeadersWriter() {
        return writer;
    }
    @Override
    public JweEncryptionState createJweEncryptionState(JweEncryptionInput jweInput) {
        JweEncryptionInternal state = getInternalState(jweInput.getJweHeaders(), jweInput);
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
    
    private JweEncryptionInternal getInternalState(JweHeaders jweInHeaders, JweEncryptionInput jweInput) {
        JweHeaders theHeaders = new JweHeaders();
        if (getKeyAlgorithm() != null) {
            theHeaders.setKeyEncryptionAlgorithm(getKeyAlgorithm());
        }
        theHeaders.setContentEncryptionAlgorithm(getContentAlgorithm());
        
        JweHeaders protectedHeaders = null;
        if (jweInHeaders != null) {
            if (jweInHeaders.getKeyEncryptionAlgorithm() != null 
                && (getKeyAlgorithm() == null 
                    || !getKeyAlgorithm().equals(jweInHeaders.getKeyEncryptionAlgorithm()))
                || jweInHeaders.getAlgorithm() != null 
                    && !getContentAlgorithm().equals(jweInHeaders.getAlgorithm())) {
                throw new SecurityException();
            }
            theHeaders.asMap().putAll(jweInHeaders.asMap());
            if (jweInHeaders.getProtectedHeaders() != null 
                && !jweInHeaders.asMap().entrySet().containsAll(theHeaders.asMap().entrySet())) {
                jweInHeaders.getProtectedHeaders().asMap().putAll(theHeaders.asMap());
            }
            protectedHeaders = jweInHeaders.getProtectedHeaders() != null 
                ? jweInHeaders.getProtectedHeaders() : theHeaders;
        } else {
            protectedHeaders = theHeaders;
        }
        
        
        
        byte[] theCek = jweInput != null && jweInput.getCek() != null 
            ? jweInput.getCek() : getContentEncryptionKey(theHeaders);
        String contentEncryptionAlgoJavaName = Algorithm.toJavaName(getContentEncryptionAlgoJwt());
        KeyProperties keyProps = new KeyProperties(contentEncryptionAlgoJavaName);
        keyProps.setCompressionSupported(compressionRequired(theHeaders));
        
        byte[] theIv = jweInput != null && jweInput.getIv() != null  
            ? jweInput.getIv() : getContentEncryptionAlgorithm().getInitVector();
        AlgorithmParameterSpec specParams = getAlgorithmParameterSpec(theIv);
        keyProps.setAlgoSpec(specParams);
        byte[] jweContentEncryptionKey = 
            getEncryptedContentEncryptionKey(theHeaders, theCek);
        
        
        String protectedHeadersJson = writer.headersToJson(protectedHeaders);
        
        byte[] additionalEncryptionParam = getAAD(protectedHeadersJson, 
                                                  jweInput == null ? null : jweInput.getAad());
        keyProps.setAdditionalData(additionalEncryptionParam);
        
        JweEncryptionInternal state = new JweEncryptionInternal();
        state.theHeaders = theHeaders;
        state.jweContentEncryptionKey = jweContentEncryptionKey;
        state.keyProps = keyProps;
        state.secretKey = theCek; 
        state.theIv = theIv;
        state.protectedHeadersJson = protectedHeadersJson;
        state.aad = jweInput != null ? jweInput.getAad() : null;
        return state;
    }
    private boolean compressionRequired(JweHeaders theHeaders) {
        return JoseConstants.DEFLATE_ZIP_ALGORITHM.equals(theHeaders.getZipAlgorithm());
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
        String protectedHeadersJson;
        byte[] aad;
    }
}

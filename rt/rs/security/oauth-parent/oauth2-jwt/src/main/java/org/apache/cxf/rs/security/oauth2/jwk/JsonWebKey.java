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
package org.apache.cxf.rs.security.oauth2.jwk;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.oauth2.jwt.AbstractJwtObject;
import org.apache.cxf.rs.security.oauth2.jwt.JwtConstants;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;


public class JsonWebKey extends AbstractJwtObject {
    
    public static final String KEY_TYPE = "kty";
    public static final String PUBLIC_KEY_USE = "use";
    public static final String KEY_OPERATIONS = "key_ops";
    public static final String KEY_ALGO = JwtConstants.HEADER_ALGORITHM;
    public static final String KEY_ID = JwtConstants.HEADER_KEY_ID;
    public static final String X509_URL = JwtConstants.HEADER_X509_URL;
    public static final String X509_CHAIN = JwtConstants.HEADER_X509_CHAIN;
    public static final String X509_THUMBPRINT = JwtConstants.HEADER_X509_THUMBPRINT;
    public static final String X509_THUMBPRINT_SHA256 = JwtConstants.HEADER_X509_THUMBPRINT_SHA256;
    
    public static final String KEY_TYPE_RSA = "RSA";
    public static final String RSA_MODULUS = "n";
    public static final String RSA_PUBLIC_EXP = "e";
    public static final String RSA_PRIVATE_EXP = "d";
    public static final String RSA_FIRST_PRIME_FACTOR = "p";
    public static final String RSA_SECOND_PRIME_FACTOR = "q";
    public static final String RSA_FIRST_PRIME_CRT = "dp";
    public static final String RSA_SECOND_PRIME_CRT = "dq";
    public static final String RSA_FIRST_CRT_COEFFICIENT = "qi";
        
    public static final String KEY_TYPE_OCTET = "oct";
    public static final String OCTET_KEY_VALUE = "k";
    
    public static final String KEY_TYPE_ELLIPTIC = "EC";
    public static final String EC_CURVE = "crv";
    public static final String EC_CURVE_P256 = "P-256";
    public static final String EC_CURVE_P384 = "P-384";
    public static final String EC_CURVE_P512 = "P-512";
    public static final String EC_X_COORDINATE = "x";
    public static final String EC_Y_COORDINATE = "y";
    public static final String EC_PRIVATE_KEY = "d";
    
    public static final String PUBLIC_KEY_USE_SIGN = "sig";
    public static final String PUBLIC_KEY_USE_ENCRYPT = "enc";
    
    public static final String KEY_OPER_SIGN = "sign";
    public static final String KEY_OPER_VERIFY = "verify";
    public static final String KEY_OPER_ENCRYPT = "encrypt";
    public static final String KEY_OPER_DECRYPT = "decrypt";
    
    public JsonWebKey() {
        
    }
    
    public JsonWebKey(Map<String, Object> values) {
        super(values);
    }
    
    public void setKeyType(String keyType) {
        super.setValue(KEY_TYPE, keyType);
    }

    public String getKeyType() {
        return (String)super.getValue(KEY_TYPE);
    }

    public void setPublicKeyUse(String use) {
        super.setValue(PUBLIC_KEY_USE, use);
    }
    
    public String getPublicKeyUse() {
        return (String)super.getValue(PUBLIC_KEY_USE);
    }

    public void setKeyOperation(List<String> keyOperation) {
        super.setValue(KEY_OPERATIONS, keyOperation);
    }

    public List<String> getKeyOperation() {
        return CastUtils.cast((List<?>)super.getValue(KEY_OPERATIONS));
    }
    
    public void setAlgorithm(String algorithm) {
        super.setValue(KEY_ALGO, algorithm);
    }

    public String getAlgorithm() {
        return (String)super.getValue(KEY_ALGO);
    }
    
    public void setKid(String kid) {
        super.setValue(KEY_ID, kid);
    }

    public String getKid() {
        return (String)super.getValue(KEY_ID);
    }
    
    public void setX509Url(String x509Url) {
        super.setValue(X509_URL, x509Url);
    }
    
    public String getX509Url() {
        return (String)super.getValue(X509_URL);
    }

    public void setX509Chain(String x509Chain) {
        super.setValue(X509_CHAIN, x509Chain);
    }

    public String getX509Chain() {
        return (String)super.getValue(X509_CHAIN);
    }
    
    public void setX509Thumbprint(String x509Thumbprint) {
        super.setValue(X509_THUMBPRINT, x509Thumbprint);
    }
    
    public String getX509Thumbprint() {
        return (String)super.getValue(X509_THUMBPRINT);
    }
    
    public void setX509ThumbprintSHA256(String x509Thumbprint) {
        super.setValue(X509_THUMBPRINT_SHA256, x509Thumbprint);
    }
    
    public String getX509ThumbprintSHA256() {
        return (String)super.getValue(X509_THUMBPRINT_SHA256);
    }
    
    public JsonWebKey setProperty(String name, Object value) {
        super.setValue(name, value);
        return this;
    }
    
    public Object getProperty(String name) {
        return super.getValue(name);
    }
    
    public RSAPublicKey toRSAPublicKey() {
        String encodedModulus = (String)super.getValue(RSA_MODULUS);
        String encodedPublicExponent = (String)super.getValue(RSA_PUBLIC_EXP);
        return CryptoUtils.getRSAPublicKey(encodedModulus, encodedPublicExponent);
    }
    public RSAPrivateKey toRSAPrivateKey() {
        String encodedModulus = (String)super.getValue(RSA_MODULUS);
        String encodedPrivateExponent = (String)super.getValue(RSA_PRIVATE_EXP);
        String encodedPrimeP = (String)super.getValue(RSA_FIRST_PRIME_FACTOR);
        if (encodedPrimeP == null) {
            return CryptoUtils.getRSAPrivateKey(encodedModulus, encodedPrivateExponent);
        } else {
            String encodedPublicExponent = (String)super.getValue(RSA_PUBLIC_EXP);
            String encodedPrimeQ = (String)super.getValue(RSA_SECOND_PRIME_FACTOR);
            String encodedPrimeExpP = (String)super.getValue(RSA_FIRST_PRIME_CRT);
            String encodedPrimeExpQ = (String)super.getValue(RSA_SECOND_PRIME_CRT);
            String encodedCrtCoefficient = (String)super.getValue(RSA_FIRST_CRT_COEFFICIENT);
            return CryptoUtils.getRSAPrivateKey(encodedModulus, 
                                                encodedPublicExponent,
                                                encodedPrivateExponent,
                                                encodedPrimeP,
                                                encodedPrimeQ,
                                                encodedPrimeExpP,
                                                encodedPrimeExpQ,
                                                encodedCrtCoefficient);
        }
    }
    
}

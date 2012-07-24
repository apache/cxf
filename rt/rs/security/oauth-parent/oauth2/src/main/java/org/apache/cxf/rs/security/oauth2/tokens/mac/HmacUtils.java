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
package org.apache.cxf.rs.security.oauth2.tokens.mac;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public final class HmacUtils {
    private HmacUtils() {
        
    }
    
    public static String computeSignature(String macAlgoOAuthName, String macSecret, String data) {
        HmacAlgorithm theAlgo = HmacAlgorithm.toHmacAlgorithm(macAlgoOAuthName);
        return HmacUtils.computeHmacString(macSecret, 
                                           theAlgo.getJavaName(), 
                                           data);
    }
    
    /**
      * Computes HMAC value using the given parameters.
      * 
      * @param macSecret
      * @param macAlgorithm Should be one of HmacSHA1 or HmacSHA256
      * @param data
      * @return Base64 encoded string representation of the computed hmac value
      */
    public static String computeHmacString(String macSecret, String macAlgoJavaName, String data) {
        return new String(Base64Utility.encode(computeHmac(macSecret, macAlgoJavaName, data)));
    }
    
    /**
     * Computes HMAC value using the given parameters.
     * 
     * @param macKey
     * @param macAlgorithm
     * @param data
     * @return Computed HMAC value.
     */
    public static byte[] computeHmac(String key, HmacAlgorithm algo, String data) {
        return computeHmac(key, algo.getJavaName(), data);
    }
    
    /**
      * Computes HMAC value using the given parameters.
      * 
      * @param macKey
      * @param macAlgorithm
      * @param data
      * @return Computed HMAC value.
      */
    public static byte[] computeHmac(String key, String macAlgoJavaName, String data) {
        try {
            Mac hmac = Mac.getInstance(macAlgoJavaName);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), macAlgoJavaName);
            hmac.init(secretKey);
            return hmac.doFinal(data.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST, e);
        } catch (InvalidKeyException e) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST, e);
        } catch (UnsupportedEncodingException e) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST, e);
        }
    }
    
    public static String generateSecret(HmacAlgorithm algo) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algo.name());
            return Base64Utility.encode(keyGen.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR, e);
        }
    }
    
       
       
}

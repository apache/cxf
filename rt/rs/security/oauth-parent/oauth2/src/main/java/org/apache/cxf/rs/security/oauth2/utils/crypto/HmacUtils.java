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
package org.apache.cxf.rs.security.oauth2.utils.crypto;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;

public final class HmacUtils {
    
    private HmacUtils() {
        
    }
    
    public static String encodeHmacString(String macSecret, String macAlgoJavaName, String data) {
        return Base64Utility.encode(computeHmac(macSecret, macAlgoJavaName, data));
    }
    
    public static String encodeHmacString(String macSecret, String macAlgoJavaName, String data, boolean urlSafe) {
        byte[] bytes = computeHmac(macSecret, macAlgoJavaName, data);
        return urlSafe ? Base64UrlUtility.encode(bytes) : Base64Utility.encode(bytes);
    }
    
    public static Mac getMac(String macAlgoJavaName) {
        return getMac(macAlgoJavaName, (String)null);
    }
    
    public static Mac getMac(String macAlgoJavaName, String provider) {
        try {
            return provider == null ? Mac.getInstance(macAlgoJavaName) : Mac.getInstance(macAlgoJavaName, provider);
        } catch (NoSuchAlgorithmException e) {
            throw new OAuthServiceException(e);
        } catch (NoSuchProviderException e) {
            throw new OAuthServiceException(e);
        }
    }
    
    public static Mac getMac(String macAlgoJavaName, Provider provider) {
        try {
            return Mac.getInstance(macAlgoJavaName, provider);
        } catch (NoSuchAlgorithmException e) {
            throw new OAuthServiceException(e);
        }
    }
    
    public static byte[] computeHmac(String key, String macAlgoJavaName, String data) {
        Mac mac = getMac(macAlgoJavaName);
        return computeHmac(key, mac, data);
    }
    
    public static byte[] computeHmac(byte[] key, String macAlgoJavaName, String data) {
        Mac mac = getMac(macAlgoJavaName);
        return computeHmac(key, mac, data);
    }
    
    public static byte[] computeHmac(String key, Mac hmac, String data) {
        try {
            return computeHmac(key.getBytes("UTF-8"), hmac, data);
        } catch (UnsupportedEncodingException e) {
            throw new OAuthServiceException(e);
        }
    }
    
    public static byte[] computeHmac(byte[] key, Mac hmac, String data) {
        SecretKeySpec secretKey = new SecretKeySpec(key, hmac.getAlgorithm());
        return computeHmac(secretKey, hmac, data);
    }
    
    public static byte[] computeHmac(Key secretKey, Mac hmac, String data) {
        return computeHmac(secretKey, hmac, null, data);
    }
    
    public static byte[] computeHmac(Key secretKey, Mac hmac, AlgorithmParameterSpec spec, String data) {
        try {
            if (spec == null) {
                hmac.init(secretKey);
            } else {
                hmac.init(secretKey, spec);
            }
            return hmac.doFinal(data.getBytes());
        } catch (InvalidKeyException e) {
            throw new OAuthServiceException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new OAuthServiceException(e);
        }
    }
    
    public static String generateKey(String algo) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algo);
            return Base64Utility.encode(keyGen.generateKey().getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new OAuthServiceException(e);
        }
    }
    
       
       
}

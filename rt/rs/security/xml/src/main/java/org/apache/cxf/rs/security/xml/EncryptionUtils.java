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
package org.apache.cxf.rs.security.xml;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;

public final class EncryptionUtils {
    private EncryptionUtils() {
        
    }
    
    public static Cipher initCipherWithCert(String keyEncAlgo, int mode, X509Certificate cert)
        throws WSSecurityException {
        Cipher cipher = WSSecurityUtil.getCipherInstance(keyEncAlgo);
        try {
            OAEPParameterSpec oaepParameterSpec = null;
            if (XMLCipher.RSA_OAEP.equals(keyEncAlgo)) {
                oaepParameterSpec = new OAEPParameterSpec(
                    "SHA-1", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT
                );
            }
            if (oaepParameterSpec == null) {
                cipher.init(mode, cert);
            } else {
                cipher.init(mode, cert.getPublicKey(), oaepParameterSpec);
            }
        } catch (InvalidKeyException e) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, e
            );
        } catch (InvalidAlgorithmParameterException e) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, e
            );
        }
        return cipher;
    }
    
    public static Cipher initCipherWithKey(String keyEncAlgo, int mode, Key key)
        throws WSSecurityException {
        Cipher cipher = WSSecurityUtil.getCipherInstance(keyEncAlgo);
        try {
            OAEPParameterSpec oaepParameterSpec = null;
            if (XMLCipher.RSA_OAEP.equals(keyEncAlgo)) {
                oaepParameterSpec = new OAEPParameterSpec(
                    "SHA-1", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT
                );
            }
            if (oaepParameterSpec == null) {
                cipher.init(mode, key);
            } else {
                cipher.init(mode, key, oaepParameterSpec);
            }
        } catch (InvalidKeyException e) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, e
            );
        } catch (InvalidAlgorithmParameterException e) {
            throw new WSSecurityException(
                WSSecurityException.FAILED_ENCRYPTION, null, null, e
            );
        }
        return cipher;
    }
    
    public static XMLCipher initXMLCipher(String symEncAlgo, int mode, Key key) 
        throws WSSecurityException {
        try {
            XMLCipher cipher = XMLCipher.getInstance(symEncAlgo);
            cipher.setSecureValidation(true);
            cipher.init(mode, key);
            return cipher;
        } catch (XMLEncryptionException ex) {
            throw new WSSecurityException(
                WSSecurityException.UNSUPPORTED_ALGORITHM, null, null, ex
            );
        }
    }
    
}


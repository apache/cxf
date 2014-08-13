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
package org.apache.cxf.rs.security.oauth2.jwt.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.jwe.JweCryptoProperties;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;
import org.apache.cxf.rs.security.oauth2.jwe.WrappedKeyJweDecryption;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public class AbstractJweDecryptingFilter {
    private static final String RSSEC_ENCRYPTION_IN_PROPS = "rs.security.encryption.in.properties";
    private static final String RSSEC_ENCRYPTION_PROPS = "rs.security.encryption.properties";
        
    private JweDecryptionProvider decryption;
    private JweCryptoProperties cryptoProperties;
    private String defaultMediaType;
    protected JweDecryptionOutput decrypt(InputStream is) throws IOException {
        JweDecryptionProvider theDecryptor = getInitializedDecryption();
        JweDecryptionOutput out = theDecryptor.decrypt(new String(IOUtils.readBytesFromStream(is), "UTF-8"));
        validateHeaders(out.getHeaders());
        return out;
    }

    protected void validateHeaders(JweHeaders headers) {
        // complete
    }
    public void setDecryptionProvider(JweDecryptionProvider decryptor) {
        this.decryption = decryptor;
    }
    protected JweDecryptionProvider getInitializedDecryption() {
        if (decryption != null) {
            return decryption;    
        } 
        try {
            PrivateKey pk = CryptoUtils.loadPrivateKey(JAXRSUtils.getCurrentMessage(), 
                                                       RSSEC_ENCRYPTION_IN_PROPS, 
                                                       RSSEC_ENCRYPTION_PROPS,
                                                       CryptoUtils.RSSEC_DECRYPT_KEY_PSWD_PROVIDER);
            return new WrappedKeyJweDecryption(pk, cryptoProperties);
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }

    public void setCryptoProperties(JweCryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

}

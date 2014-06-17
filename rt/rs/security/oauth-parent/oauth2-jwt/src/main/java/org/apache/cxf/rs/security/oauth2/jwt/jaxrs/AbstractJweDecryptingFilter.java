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
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jwe.JweCryptoProperties;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptor;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;
import org.apache.cxf.rs.security.oauth2.jwe.WrappedKeyJweDecryptor;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.PrivateKeyPasswordProvider;

public class AbstractJweDecryptingFilter {
    private static final String RSSEC_ENCRYPTION_IN_PROPS = "rs.security.encryption.in.properties";
    
    private JweDecryptor decryptor;
    private JweCryptoProperties cryptoProperties;
    private String defaultMediaType;
    protected JweDecryptionOutput decrypt(InputStream is) throws IOException {
        JweDecryptor theDecryptor = getInitializedDecryptor();
        JweDecryptionOutput out = theDecryptor.decrypt(new String(IOUtils.readBytesFromStream(is), "UTF-8"));
        validateHeaders(out.getHeaders());
        return out;
    }

    protected void validateHeaders(JweHeaders headers) {
        // complete
    }
    public void setDecryptor(JweDecryptor decryptor) {
        this.decryptor = decryptor;
    }
    protected JweDecryptor getInitializedDecryptor() {
        if (decryptor != null) {
            return decryptor;    
        } 
        Message m = JAXRSUtils.getCurrentMessage();
        if (m == null) {
            throw new SecurityException();
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_ENCRYPTION_IN_PROPS);
        if (propLoc == null) {
            throw new SecurityException();
        }
        try {
            Bus bus = m.getExchange().getBus();
            Properties props = ResourceUtils.loadProperties(propLoc, bus);
            PrivateKey pk = null;
            KeyStore keyStore = (KeyStore)m.getExchange().get(props.get(CryptoUtils.RSSEC_KEY_STORE_FILE));
            if (keyStore == null) {
                keyStore = CryptoUtils.loadKeyStore(props, bus);
                m.getExchange().put((String)props.get(CryptoUtils.RSSEC_KEY_STORE_FILE), keyStore);
            }
            PrivateKeyPasswordProvider cb = 
                (PrivateKeyPasswordProvider)m.getContextualProperty(CryptoUtils.RSSEC_KEY_PSWD_PROVIDER);
            pk = CryptoUtils.loadPrivateKey(keyStore, props, bus, cb);
            
            return new WrappedKeyJweDecryptor(pk, cryptoProperties);
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

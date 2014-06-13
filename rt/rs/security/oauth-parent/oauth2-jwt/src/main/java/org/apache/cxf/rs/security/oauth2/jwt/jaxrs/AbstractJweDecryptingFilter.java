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

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.jwe.JweCryptoProperties;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.oauth2.jwe.JweDecryptor;
import org.apache.cxf.rs.security.oauth2.jwe.JweHeaders;
import org.apache.cxf.rs.security.oauth2.jwe.WrappedKeyJweDecryptor;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.PrivateKeyPasswordProvider;

public class AbstractJweDecryptingFilter {
    private static final String RSSEC_ENCRYPTION_PROPS = "rs-security.encryption.properties";
    private static final String RSSEC_KEY_PSWD_PROVIDER = "org.apache.rs.security.crypto.private.provider";
    
    private JweDecryptor decryptor;
    private JweCryptoProperties cryptoProperties;
    protected byte[] decrypt(InputStream is) throws IOException {
        JweDecryptor theDecryptor = getInitializedDecryptor();
        if (theDecryptor == null) {
            throw new SecurityException();
        }
        JweDecryptionOutput out = theDecryptor.decrypt(new String(IOUtils.readBytesFromStream(is), "UTF-8"));
        validateHeaders(out.getHeaders());
        return out.getContent();
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
            return null;
        }
        String propLoc = (String)m.getContextualProperty(RSSEC_ENCRYPTION_PROPS);
        if (propLoc == null) {
            return null;
        }
        PrivateKeyPasswordProvider cb = (PrivateKeyPasswordProvider)m.getContextualProperty(RSSEC_KEY_PSWD_PROVIDER);
        Bus bus = (Bus)m.getExchange().get(Endpoint.class).get(Bus.class.getName());
        PrivateKey pk = CryptoUtils.loadPrivateKey(propLoc, bus, cb);
        return new WrappedKeyJweDecryptor(pk, cryptoProperties);
    }

    public void setCryptoProperties(JweCryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
    }

}

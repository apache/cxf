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
package org.apache.cxf.rs.security.oauth2.provider;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class OAuthJoseJwtConsumer extends JoseJwtConsumer {

    private boolean decryptWithClientSecret;
    private boolean verifyWithClientSecret;

    public JwtToken getJwtToken(String wrappedJwtToken, String clientSecret) {
        return getJwtToken(wrappedJwtToken,
                           getInitializedDecryptionProvider(clientSecret),
                           getInitializedSignatureVerifier(clientSecret));
    }

    protected JwsSignatureVerifier getInitializedSignatureVerifier(String clientSecret) {
        if (verifyWithClientSecret && !StringUtils.isEmpty(clientSecret)) {
            return OAuthUtils.getClientSecretSignatureVerifier(clientSecret);
        }
        return null;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider(String clientSecret) {
        if (decryptWithClientSecret && !StringUtils.isEmpty(clientSecret)) {
            return OAuthUtils.getClientSecretDecryptionProvider(clientSecret);
        }
        return null;
    }

    public boolean isDecryptWithClientSecret() {
        return decryptWithClientSecret;
    }

    public void setDecryptWithClientSecret(boolean decryptWithClientSecret) {
        this.decryptWithClientSecret = decryptWithClientSecret;
    }

    public boolean isVerifyWithClientSecret() {
        return verifyWithClientSecret;
    }

    public void setVerifyWithClientSecret(boolean verifyWithClientSecret) {
        this.verifyWithClientSecret = verifyWithClientSecret;
    }


}

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
package org.apache.cxf.rs.security.jose.common;

import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public abstract class AbstractJoseConsumer {
    private boolean jwsRequired = true;
    private boolean jweRequired;
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    

    public void setJweDecryptor(JweDecryptionProvider jweDecryptor) {
        this.jweDecryptor = jweDecryptor;
    }

    public JweDecryptionProvider getJweDecryptor() {
        return jweDecryptor;
    }

    public void setJwsVerifier(JwsSignatureVerifier theJwsVerifier) {
        this.jwsVerifier = theJwsVerifier;
    }

    public JwsSignatureVerifier getJwsVerifier() {
        return jwsVerifier;
    }

    protected JweDecryptionProvider getInitializedDecryptionProvider(JweHeaders jweHeaders) {
        if (jweDecryptor != null) {
            return jweDecryptor;
        }
        return JweUtils.loadDecryptionProvider(jweHeaders, false);
    }
    protected JwsSignatureVerifier getInitializedSignatureVerifier(JwsHeaders jwsHeaders) {
        if (jwsVerifier != null) {
            return jwsVerifier;
        }

        return JwsUtils.loadSignatureVerifier(jwsHeaders, false);
    }

    public boolean isJwsRequired() {
        return jwsRequired;
    }

    public void setJwsRequired(boolean jwsRequired) {
        this.jwsRequired = jwsRequired;
    }

    public boolean isJweRequired() {
        return jweRequired;
    }

    public void setJweRequired(boolean jweRequired) {
        this.jweRequired = jweRequired;
    }

    protected void checkProcessRequirements() {
        if (!isJwsRequired() && !isJweRequired()) {
            throw new JoseException("Unable to process the data");
        }
    }
}
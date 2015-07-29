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
package org.apache.cxf.rs.security.jose;

import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public abstract class AbstractJoseConsumer {
    private JweDecryptionProvider jweDecryptor;
    private JwsSignatureVerifier jwsVerifier;
    
    public void setJweDecryptor(JweDecryptionProvider jweDecryptor) {
        this.jweDecryptor = jweDecryptor;
    }

    public void setJwsVerifier(JwsSignatureVerifier theJwsVerifier) {
        this.jwsVerifier = theJwsVerifier;
    }

    protected JweDecryptionProvider getInitializedDecryptionProvider(boolean required) {
        if (jweDecryptor != null) {
            return jweDecryptor;    
        } 
        return JweUtils.loadDecryptionProvider(required);
    }
    protected JwsSignatureVerifier getInitializedSignatureVerifier(boolean required) {
        if (jwsVerifier != null) {
            return jwsVerifier;    
        } 
        return JwsUtils.loadSignatureVerifier(required);
    }

}

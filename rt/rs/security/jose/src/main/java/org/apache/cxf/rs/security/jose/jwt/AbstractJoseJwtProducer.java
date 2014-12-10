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
package org.apache.cxf.rs.security.jose.jwt;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jws.NoneJwsSignatureProvider;

public abstract class AbstractJoseJwtProducer {
    private JwsSignatureProvider sigProvider;
    private JweEncryptionProvider encryptionProvider;
    protected String processJwt(JwtToken jwt, boolean jwsRequired, boolean jweRequired) {
        JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwt); 
        JwsSignatureProvider theSigProvider = getInitializedSigProvider(jweRequired, jwsRequired);
        String data = jws.signWith(theSigProvider);
        JweEncryptionProvider theEncProvider = getInitializedEncryptionProvider(jweRequired);
        if (theEncProvider != null) {
            data = theEncProvider.encrypt(StringUtils.toBytesUTF8(data), null);
        }
        return data;
    }
    protected JwsSignatureProvider getInitializedSigProvider(boolean jwsRequired, boolean jweRequired) {
        if (sigProvider != null) {
            return sigProvider;    
        } 
        JwsSignatureProvider theSigProvider = JwsUtils.loadSignatureProvider(jwsRequired);
        if (theSigProvider == null && jweRequired) {
            return new NoneJwsSignatureProvider();
        }
        throw new SecurityException();
    }
    protected JweEncryptionProvider getInitializedEncryptionProvider(boolean required) {
        if (encryptionProvider != null) {
            return encryptionProvider;    
        }
        return JweUtils.loadEncryptionProvider(required);
    }
}

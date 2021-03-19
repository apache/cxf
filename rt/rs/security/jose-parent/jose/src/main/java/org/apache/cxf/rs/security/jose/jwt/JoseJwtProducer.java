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
import org.apache.cxf.rs.security.jose.common.AbstractJoseProducer;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

public class JoseJwtProducer extends AbstractJoseProducer {
    
    public String processJwt(JwtToken jwt) {
        return processJwt(jwt, null, null);
    }
    public String processJwt(JwtToken jwt,
                                JweEncryptionProvider theEncProvider,
                                JwsSignatureProvider theSigProvider) {
        super.checkProcessRequirements();
        String data;

        if (isJweRequired() && theEncProvider == null) {
            theEncProvider = getInitializedEncryptionProvider(jwt.getJweHeaders());
            if (theEncProvider == null) {
                throw new JwtException("Unable to encrypt JWT");
            }
        }

        if (isJwsRequired()) {
            
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwt);
            if (jws.isPlainText()) {
                data = jws.getSignedEncodedJws();
            } else {
                if (theSigProvider == null) {
                    theSigProvider = getInitializedSignatureProvider(jws.getJwsHeaders());
                }

                if (theSigProvider == null) {
                    throw new JwtException("Unable to sign JWT");
                }

                data = jws.signWith(theSigProvider);
            }
            if (theEncProvider != null) {
                data = theEncProvider.encrypt(StringUtils.toBytesUTF8(data), jwt.getJweHeaders());
            }
        } else {
            JweJwtCompactProducer jwe = new JweJwtCompactProducer(jwt.getJweHeaders(), jwt.getClaims());
            data = jwe.encryptWith(theEncProvider);
        }
        return data;
    }

}

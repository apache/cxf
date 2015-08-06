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
import org.apache.cxf.rs.security.jose.AbstractJoseProducer;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

public abstract class AbstractJoseJwtProducer extends AbstractJoseProducer {
    private boolean jwsRequired = true;
    private boolean jweRequired;
    
    protected String processJwt(JwtToken jwt) {
        if (!isJwsRequired() && !isJweRequired()) {
            throw new JwtException("Unable to secure JWT");
        }
        String data = null;
        JweEncryptionProvider theEncProvider = getInitializedEncryptionProvider(isJweRequired());
        if (isJwsRequired()) {
            JwsJwtCompactProducer jws = new JwsJwtCompactProducer(jwt); 
            JwsSignatureProvider theSigProvider = getInitializedSignatureProvider(isJwsRequired());
            data = jws.signWith(theSigProvider);
            if (theEncProvider != null) {
                data = theEncProvider.encrypt(StringUtils.toBytesUTF8(data), null);
            }
        } else {
            JweJwtCompactProducer jwe = new JweJwtCompactProducer(jwt);
            data = jwe.encryptWith(theEncProvider);
        }
        return data;
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
}

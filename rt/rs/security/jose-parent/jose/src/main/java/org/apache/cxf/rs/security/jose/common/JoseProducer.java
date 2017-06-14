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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;

public class JoseProducer extends AbstractJoseProducer {
    
    public String processData(String data) {
        super.checkProcessRequirements();
        
        JweEncryptionProvider theEncProvider = null;
        JweHeaders jweHeaders = new JweHeaders();
        if (isJweRequired()) {
            theEncProvider = getInitializedEncryptionProvider(jweHeaders);
            if (theEncProvider == null) {
                throw new JoseException("Unable to encrypt the data");
            }
        }

        if (isJwsRequired()) {
            JwsHeaders jwsHeaders = new JwsHeaders();
            JwsCompactProducer jws = new JwsCompactProducer(jwsHeaders, data);
            
            JwsSignatureProvider theSigProvider = getInitializedSignatureProvider(jwsHeaders);
            
            if (theSigProvider == null) {
                throw new JoseException("Unable to sign the data");
            }

            data = jws.signWith(theSigProvider);
            
        }
        if (theEncProvider != null) {
            data = theEncProvider.encrypt(StringUtils.toBytesUTF8(data), jweHeaders);
        }
        return data;
    }

}

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

import org.apache.cxf.rs.security.jose.jwe.JweCompactConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jwt.JwtException;

public class JoseConsumer extends AbstractJoseConsumer {
    
    public String getData(String data) {
        super.checkProcessRequirements();

        if (isJweRequired()) {
            JweCompactConsumer jweConsumer = new JweCompactConsumer(data);

            JweDecryptionProvider theDecryptor = getInitializedDecryptionProvider(jweConsumer.getJweHeaders());
            if (theDecryptor == null) {
                throw new JwtException("Unable to decrypt JWT");
            }

            if (!isJwsRequired()) {
                return jweConsumer.getDecryptedContentText(theDecryptor);
            }

            JweDecryptionOutput decOutput = theDecryptor.decrypt(data);
            data = decOutput.getContentText();
        }

        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(data);
        if (isJwsRequired()) {
            JwsSignatureVerifier theSigVerifier = getInitializedSignatureVerifier(jwsConsumer.getJwsHeaders());
            if (theSigVerifier == null) {
                throw new JwtException("Unable to validate JWT");
            }

            if (!jwsConsumer.verifySignatureWith(theSigVerifier)) {
                throw new JwtException("Invalid Signature");
            }
        }
        return jwsConsumer.getDecodedJwsPayload();
    }
}

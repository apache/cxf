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
package org.apache.cxf.rs.security.jose.jws;

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

public class NoneJwsSignatureVerifier implements JwsSignatureVerifier {

    @Override
    public boolean verify(JwsHeaders headers, String unsignedText, byte[] signature) {
        return headers.getSignatureAlgorithm() == getAlgorithm()
            && signature.length == 0;
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        return SignatureAlgorithm.NONE;
    }

    @Override
    public JwsVerificationSignature createJwsVerificationSignature(JwsHeaders headers) {
        return new NoneJwsVerificationSignature();
    }

    private static final class NoneJwsVerificationSignature implements JwsVerificationSignature {

        @Override
        public void update(byte[] src, int off, int len) {
            // complete
        }

        @Override
        public boolean verify(byte[] signature) {
            return signature.length == 0;
        }

    }
}

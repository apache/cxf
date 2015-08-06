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
package org.apache.cxf.rs.security.jose.jwe;

import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;



public class AesGcmContentDecryptionAlgorithm extends AbstractContentEncryptionCipherProperties
    implements ContentDecryptionProvider {
    public AesGcmContentDecryptionAlgorithm(ContentAlgorithm supportedAlgo) {
        super(supportedAlgo);
    }

    @Override
    public byte[] getEncryptedSequence(JweHeaders headers, byte[] cipher, byte[] authTag) {
        String algo = headers.getContentEncryptionAlgorithm().getJwaName();
        if (!AlgorithmUtils.isAesGcm(algo) || !getAlgorithm().getJwaName().equals(algo)) {
            LOG.warning("Invalid content encryption algorithm");
            throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
        }
        return JweCompactConsumer.getCipherWithAuthTag(cipher, authTag);
    }
}

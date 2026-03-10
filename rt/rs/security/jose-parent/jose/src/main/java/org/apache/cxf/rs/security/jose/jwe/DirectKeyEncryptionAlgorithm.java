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

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;

public class DirectKeyEncryptionAlgorithm implements KeyEncryptionProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(DirectKeyEncryptionAlgorithm.class);
    public byte[] getEncryptedContentEncryptionKey(JweHeaders headers, byte[] theCek) {
        checkKeyEncryptionAlgorithm(headers);
        return new byte[0];
    }
    protected void checkKeyEncryptionAlgorithm(JweHeaders headers) {
        KeyAlgorithm keyAlgo = headers.getKeyEncryptionAlgorithm();
        if (keyAlgo != null && KeyAlgorithm.DIRECT != keyAlgo) {
            LOG.warning("Key encryption algorithm header is set");
            throw new JweException(JweException.Error.INVALID_KEY_ALGORITHM);
        }
    }
    @Override
    public KeyAlgorithm getAlgorithm() {
        return KeyAlgorithm.DIRECT;
    }
}

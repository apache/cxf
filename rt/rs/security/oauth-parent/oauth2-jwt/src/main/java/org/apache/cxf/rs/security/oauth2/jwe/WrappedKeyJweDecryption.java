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
package org.apache.cxf.rs.security.oauth2.jwe;

import java.security.Key;

import org.apache.cxf.rs.security.oauth2.jwt.JwtHeadersReader;

public class WrappedKeyJweDecryption extends AbstractJweDecryption {
    public WrappedKeyJweDecryption(Key cekDecryptionKey) {    
        this(cekDecryptionKey, true);
    }
    public WrappedKeyJweDecryption(Key cekDecryptionKey, boolean unwrap) {    
        this(cekDecryptionKey, unwrap, null);
    }
    public WrappedKeyJweDecryption(Key cekDecryptionKey, JweCryptoProperties props) {
        this(cekDecryptionKey, true, props);
    }
    public WrappedKeyJweDecryption(Key cekDecryptionKey, boolean unwrap,
                                  JweCryptoProperties props) {    
        this(cekDecryptionKey, unwrap, props, null);
    }
    public WrappedKeyJweDecryption(Key cekDecryptionKey, boolean unwrap,
                                   JweCryptoProperties props, JwtHeadersReader reader) {    
        this(new WrappedKeyDecryptionAlgorithm(cekDecryptionKey, unwrap),
             props, reader);
    }
    public WrappedKeyJweDecryption(WrappedKeyDecryptionAlgorithm keyDecryptionAlgo,
                                   JweCryptoProperties props, JwtHeadersReader reader) {    
        this(keyDecryptionAlgo, props, reader, new AesGcmContentDecryptionAlgorithm());
    }
    public WrappedKeyJweDecryption(WrappedKeyDecryptionAlgorithm keyDecryptionAlgo,
                                   JweCryptoProperties props, JwtHeadersReader reader,
                                   ContentDecryptionAlgorithm cipherProps) {    
        super(props, reader, keyDecryptionAlgo, cipherProps);
    }
}

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

import org.apache.cxf.rs.security.jose.common.JoseException;

public class JweException extends JoseException {

    private static final long serialVersionUID = 4118589816228511524L;
    private final Error status;

    public JweException(Error status) {
        this(status, null);
    }
    public JweException(Error status, Throwable cause) {
        super(status != null ? status.toString() : null, cause);
        this.status = status;
    }
    public Error getError() {
        return status;
    }
    public enum Error {
        NO_ENCRYPTOR,
        NO_DECRYPTOR,
        NO_INIT_PROPERTIES,
        KEY_ALGORITHM_NOT_SET,
        CUSTOM_IV_REUSED,
        INVALID_KEY_ALGORITHM,
        INVALID_CONTENT_ALGORITHM,
        INVALID_CONTENT_KEY,
        KEY_ENCRYPTION_FAILURE,
        CONTENT_ENCRYPTION_FAILURE,
        KEY_DECRYPTION_FAILURE,
        CONTENT_DECRYPTION_FAILURE,
        INVALID_COMPACT_JWE,
        INVALID_JSON_JWE
    }
}

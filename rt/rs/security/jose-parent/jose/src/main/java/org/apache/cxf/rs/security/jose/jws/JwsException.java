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

import org.apache.cxf.rs.security.jose.common.JoseException;

public class JwsException extends JoseException {

    private static final long serialVersionUID = 4118589816228511524L;
    private final Error status;
    public JwsException(Error status) {
        this(status, null);
    }
    public JwsException(Error status, Throwable cause) {
        super(status != null ? status.toString() : null, cause);
        this.status = status;
    }
    public Error getError() {
        return status;
    }
    public enum Error {
        NO_PROVIDER,
        NO_VERIFIER,
        NO_INIT_PROPERTIES,
        ALGORITHM_NOT_SET,
        INVALID_ALGORITHM,
        INVALID_KEY,
        SIGNATURE_FAILURE,
        INVALID_SIGNATURE,
        INVALID_COMPACT_JWS,
        INVALID_JSON_JWS
    }
}

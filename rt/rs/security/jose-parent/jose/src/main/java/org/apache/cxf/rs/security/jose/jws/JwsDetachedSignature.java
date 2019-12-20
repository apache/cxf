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

public class JwsDetachedSignature {
    private final JwsHeaders headers;
    private final String base64UrlEncodedHeaders;
    private final JwsSignature signature;
    private final boolean useJwsJsonSignatureFormat;

    public JwsDetachedSignature(JwsHeaders headers,
                                String base64UrlEncodedHeaders,
                                JwsSignature signature,
                                boolean useJwsJsonSignatureFormat) {
        this.headers = headers;
        this.base64UrlEncodedHeaders = base64UrlEncodedHeaders;
        this.signature = signature;
        this.useJwsJsonSignatureFormat = useJwsJsonSignatureFormat;
    }
    public JwsHeaders getHeaders() {
        return headers;
    }
    public String getEncodedHeaders() {
        return base64UrlEncodedHeaders;
    }
    public JwsSignature getSignature() {
        return signature;
    }
    public boolean isUseJwsJsonSignatureFormat() {
        return useJwsJsonSignatureFormat;
    }

}

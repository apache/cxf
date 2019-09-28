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

package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.rt.security.rs.RSSecurityConstants;

/**
 * Some security constants to be used with HTTP Signature.
 */
public final class HTTPSignatureConstants extends RSSecurityConstants {

    public static final String REQUEST_TARGET = "(request-target)";

    /**
     * The signature key id. This is a required configuration option on the outbound side.
     */
    public static final String RSSEC_HTTP_SIGNATURE_KEY_ID = "rs.security.http.signature.key.id";

    /**
     * This is a list of String values which correspond to the list of HTTP headers that will be signed
     * in the outbound request. The default is to sign all message headers. In addition, by default a client
     * will include "(request-target)" in the signed headers list.
     */
    public static final String RSSEC_HTTP_SIGNATURE_OUT_HEADERS = "rs.security.http.signature.out.headers";

    /**
     * This is a list of String values which correspond to the list of HTTP headers that must be signed
     * in the inbound request. By default, a client request must sign "(request-target)". In addition,
     * both a client request and service response must sign "digest", unless it is a GET request.
     */
    public static final String RSSEC_HTTP_SIGNATURE_IN_HEADERS = "rs.security.http.signature.in.headers";

    /**
     * The digest algorithm to use when digesting the payload. The default algorithm if not specified is "SHA-256".
     */
    public static final String RSSEC_HTTP_SIGNATURE_DIGEST_ALGORITHM = "rs.security.http.signature.digest.algorithm";


    private HTTPSignatureConstants() {
        // complete
    }
}

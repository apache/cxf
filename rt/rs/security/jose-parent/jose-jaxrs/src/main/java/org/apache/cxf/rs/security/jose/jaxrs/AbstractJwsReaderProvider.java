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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public class AbstractJwsReaderProvider {
    private Set<String> protectedHttpHeaders;
    private boolean validateHttpHeaders;
    
    private JwsSignatureVerifier sigVerifier;
    private String defaultMediaType;
    private boolean checkEmptyStream;
    
    public void setSignatureVerifier(JwsSignatureVerifier signatureVerifier) {
        this.sigVerifier = signatureVerifier;
    }

    protected JwsSignatureVerifier getInitializedSigVerifier(JwsHeaders headers) {
        JoseUtils.traceHeaders(headers);
        if (sigVerifier != null) {
            return sigVerifier;
        }
        return JwsUtils.loadSignatureVerifier(headers, true);
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    public void setValidateHttpHeaders(boolean validateHttpHeaders) {
        this.validateHttpHeaders = validateHttpHeaders;
    }
    public boolean isValidateHttpHeaders() {
        return validateHttpHeaders;
    }
    
    protected void validateHttpHeadersIfNeeded(MultivaluedMap<String, String> httpHeaders, JwsHeaders jwsHeaders) {
        JoseJaxrsUtils.validateHttpHeaders(httpHeaders, 
                                           jwsHeaders, 
                                           protectedHttpHeaders);
    }
    public void setProtectedHttpHeaders(Set<String> protectedHttpHeaders) {
        this.protectedHttpHeaders = protectedHttpHeaders;
    }

    /**
     * @deprecated has no effect, see {@link #setCheckEmptyStream(boolean)}.
     */
    @Deprecated
    public boolean isCheckEmptyStream() {
        return checkEmptyStream;
    }

    /**
     * No longer has any effect: the filters always read the entity stream and skip
     * a missing or empty one. A Content-Length: 0 header alone is not trusted, as the
     * stream may still carry a body.
     *
     * @deprecated has no effect.
     */
    @Deprecated
    public void setCheckEmptyStream(boolean checkEmptyStream) {
        this.checkEmptyStream = checkEmptyStream;
    }

    /**
     * Returns true if the entity must not be read for the given HTTP method.
     * By default no method is skipped: an entity is always verified when one is
     * present, whatever the method, as JAX-RS binds an entity parameter for
     * DELETE, GET and the other methods too. Subclasses may override this
     * to skip specific methods entirely.
     * <p>
     * This used to return true for DELETE, GET, HEAD, OPTIONS and TRACE requests and
     * for responses to DELETE, HEAD and OPTIONS. An override that extends
     * super.isMethodWithNoContent and still needs those methods skipped entirely
     * has to list them itself.
     */
    protected boolean isMethodWithNoContent(String method) {
        return false;
    }
    
}

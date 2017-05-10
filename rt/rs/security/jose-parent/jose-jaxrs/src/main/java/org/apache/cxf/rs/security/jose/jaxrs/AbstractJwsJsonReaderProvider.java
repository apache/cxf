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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsJsonConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonSignatureEntry;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public class AbstractJwsJsonReaderProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJwsJsonReaderProvider.class);
    private static final Set<String> DEFAULT_PROTECTED_HTTP_HEADERS = 
        new HashSet<String>(Arrays.asList(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
    private Set<String> protectedHttpHeaders = DEFAULT_PROTECTED_HTTP_HEADERS;
    private boolean validateHttpHeaders;
    private JwsSignatureVerifier sigVerifier;
    private String defaultMediaType;
    private Map<String, Object> entryProps;
    
    public void setSignatureVerifier(JwsSignatureVerifier signatureVerifier) {
        this.sigVerifier = signatureVerifier;
    }

    protected JwsSignatureVerifier getInitializedSigVerifier() {
        if (sigVerifier != null) {
            return sigVerifier;
        }
        return JwsUtils.loadSignatureVerifier(null, true);
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }


    protected void validate(JwsJsonConsumer c, JwsSignatureVerifier theSigVerifier) throws JwsException {

        List<JwsJsonSignatureEntry> remaining =
            c.verifyAndGetNonValidated(Collections.singletonList(theSigVerifier), entryProps);
        if (!remaining.isEmpty()) {
            JAXRSUtils.getCurrentMessage().put("jws.json.remaining.entries", remaining);
        }
        JAXRSUtils.getCurrentMessage().put(JwsJsonConsumer.class, c);
    }

    public Map<String, Object> getEntryProps() {
        return entryProps;
    }

    public void setEntryProps(Map<String, Object> entryProps) {
        this.entryProps = entryProps;
    }

    public void setValidateHttpHeaders(boolean validateHttpHeaders) {
        this.validateHttpHeaders = validateHttpHeaders;
    }
    public boolean isValidateHttpHeaders() {
        return validateHttpHeaders;
    }
    
    protected void validateHttpHeadersIfNeeded(MultivaluedMap<String, String> httpHeaders, JwsHeaders jwsHeaders) {
        Map<String, String> jwsHttpHeaders = new HashMap<String, String>();
        Map<String, String> updatedHttpHeaders = new HashMap<String, String>();
        final String prefix = "http.";
        for (String headerName : protectedHttpHeaders) {
            List<String> headerValues = httpHeaders.get(headerName);
            if (headerValues != null) {
                String headerValue = headerValues.size() > 1 ? headerValues.toString()
                    : headerValues.get(0).toString();
                String prefixedHeaderName = prefix + headerName;
                updatedHttpHeaders.put(prefixedHeaderName, headerValue);
                String jwsHeaderValue = jwsHeaders.getStringProperty(prefixedHeaderName);
                if (jwsHeaderValue != null) {
                    jwsHttpHeaders.put(prefixedHeaderName, jwsHeaderValue);
                }    
            }
            
        }
        if (jwsHttpHeaders.size() != updatedHttpHeaders.size() 
            || !jwsHttpHeaders.entrySet().containsAll(updatedHttpHeaders.entrySet())) { 
            throw new JwsException(JwsException.Error.INVALID_SIGNATURE);
        }
    }
    public void setProtectedHttpHeaders(Set<String> protectedHttpHeaders) {
        this.protectedHttpHeaders = protectedHttpHeaders;
    }
}

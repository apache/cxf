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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweJsonEncryptionEntry;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;

public class AbstractJweJsonDecryptingFilter {
    private Set<String> protectedHttpHeaders;
    private boolean validateHttpHeaders;
    private JweDecryptionProvider decryption;
    private String defaultMediaType;
    private Map<String, Object> recipientProperties;
    private boolean checkEmptyStream;
    protected JweDecryptionOutput decrypt(final byte[] content) throws IOException {
        JweJsonConsumer c = new JweJsonConsumer(new String(content, StandardCharsets.UTF_8));
        JweDecryptionProvider theProvider = getInitializedDecryptionProvider(c.getProtectedHeader());
        JweJsonEncryptionEntry entry = c.getJweDecryptionEntry(theProvider, recipientProperties);
        if (entry == null) {
            throw new JweException(JweException.Error.INVALID_JSON_JWE);
        }
        JweDecryptionOutput out = c.decryptWith(theProvider, entry);

        JAXRSUtils.getCurrentMessage().put(JweJsonConsumer.class, c);
        JAXRSUtils.getCurrentMessage().put(JweJsonEncryptionEntry.class, entry);
        return out;
    }

    protected void validateHeaders(JweHeaders headers) {
        // complete
    }
    public void setDecryptionProvider(JweDecryptionProvider decryptor) {
        this.decryption = decryptor;
    }
    protected JweDecryptionProvider getInitializedDecryptionProvider(JweHeaders headers) {
        if (decryption != null) {
            return decryption;
        }
        return JweUtils.loadDecryptionProvider(headers, true);
    }
    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    public void setRecipientProperties(Map<String, Object> recipientProperties) {
        this.recipientProperties = recipientProperties;
    }

    public void setValidateHttpHeaders(boolean validateHttpHeaders) {
        this.validateHttpHeaders = validateHttpHeaders;
    }
    public boolean isValidateHttpHeaders() {
        return validateHttpHeaders;
    }
    
    protected void validateHttpHeadersIfNeeded(MultivaluedMap<String, String> httpHeaders, JweHeaders jweHeaders) {
        JoseJaxrsUtils.validateHttpHeaders(httpHeaders, 
                                           jweHeaders, 
                                           protectedHttpHeaders);
    }
    public void setProtectedHttpHeaders(Set<String> protectedHttpHeaders) {
        this.protectedHttpHeaders = protectedHttpHeaders;
    }

    public boolean isCheckEmptyStream() {
        return checkEmptyStream;
    }

    public void setCheckEmptyStream(boolean checkEmptyStream) {
        this.checkEmptyStream = checkEmptyStream;
    }
}

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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.io.StringWriter;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.utils.MessageDigestGenerator;

public class DigestCodeVerifier implements CodeVerifierTransformer {

    public String transformCodeVerifier(String codeVerifier) {
        MessageDigestGenerator mdg = new MessageDigestGenerator();
        byte[] digest = mdg.createDigest(codeVerifier, "SHA-256");
        int length = digest.length > 128 / 8 ? 128 / 8 : digest.length;
        
        StringWriter stringWriter = new StringWriter();
        try {
            Base64UrlUtility.encode(digest, 0, length, stringWriter);
        } catch (Base64Exception e) {
            throw new OAuthServiceException("server_error", e);
        }
        return stringWriter.toString();
    }

    

}

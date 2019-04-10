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
package org.apache.cxf.transport.http.auth;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;

public final class DefaultBasicAuthSupplier implements HttpAuthSupplier {
    private static final String ENCODE_BASIC_AUTH_WITH_ISO8859 = "encode.basicauth.with.iso8859";
    public DefaultBasicAuthSupplier() {
        super();
    }

    public boolean requiresRequestCaching() {
        return false;
    }

    public static String getBasicAuthHeader(String userName, String passwd) {
        return getBasicAuthHeader(userName, passwd, false);
    }

    public static String getBasicAuthHeader(String userName, String passwd, boolean useIso8859) {
        final String userAndPass = userName + ':' + passwd;
        byte[] authBytes = useIso8859 ? userAndPass.getBytes(StandardCharsets.ISO_8859_1) : userAndPass.getBytes();
        return "Basic " + Base64Utility.encode(authBytes);
    }

    public String getAuthorization(AuthorizationPolicy  authPolicy,
                                   URI currentURI,
                                   Message message,
                                   String fullHeader) {
        if (authPolicy.getUserName() != null && authPolicy.getPassword() != null) {
            boolean encodeBasicAuthWithIso8859 = PropertyUtils.isTrue(
                message.getContextualProperty(ENCODE_BASIC_AUTH_WITH_ISO8859));
            return getBasicAuthHeader(authPolicy.getUserName(),
                                      authPolicy.getPassword(),
                                      encodeBasicAuthWithIso8859);
        }
        return null;
    }

}
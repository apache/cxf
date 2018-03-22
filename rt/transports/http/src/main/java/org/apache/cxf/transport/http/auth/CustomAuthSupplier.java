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

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;

/**
 * Use the AuthorizationPolicy type + value to create the authorization header.
 */
public final class CustomAuthSupplier implements HttpAuthSupplier {

    public boolean requiresRequestCaching() {
        return false;
    }

    public String getAuthorization(AuthorizationPolicy  authPolicy,
                                   URI currentURI,
                                   Message message,
                                   String fullHeader) {
        if (authPolicy.getAuthorizationType() != null && authPolicy.getAuthorization() != null) {
            return authPolicy.getAuthorizationType() + " " + authPolicy.getAuthorization();
        }
        return null;
    }

}
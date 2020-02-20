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
 * Supplies Authorization information to an HTTPConduit.
 * <p>
 * A HTTPConduit keeps a reference to this HttpAuthSupplier for the life
 * of the HTTPConduit, unless changed out by dynamic configuration.
 * Therefore, an implementation of this HttpAuthSupplier may maintain
 * state for subsequent calls.
 * <p>
 * For instance, an implementation may not provide a Authorization preemptively for
 * a particular URL and decide to get the realm information from
 * a 401 response in which the HTTPConduit will call getAuthorization for
 * that URL. Then this implementation may provide the Authorization for this
 * particular URL preemptively for subsequent calls to getAuthorization.
 */
public interface HttpAuthSupplier {

    /**
     * If the supplier requires the request to be cached to be resent, return true
     */
    boolean requiresRequestCaching();

    /**
     * The HTTPConduit makes a call to this method to obtain
     * an Authentication token for http authentication.
     *
     * @param authPolicy credentials for the authentication
     * @param uri  The URI we want to connect to
     * @param message     The CXF Message
     * @param fullHeader  The full WWW-Authenticate header or null if preemptive auth
     * @return token for Authenticate string or null if authentication is not possible
     */
    String getAuthorization(
            AuthorizationPolicy  authPolicy,
            URI     uri,
            Message message,
            String  fullHeader);
}

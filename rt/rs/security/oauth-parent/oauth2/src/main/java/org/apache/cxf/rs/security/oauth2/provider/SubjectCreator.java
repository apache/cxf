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

package org.apache.cxf.rs.security.oauth2.provider;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

/**
 * Optional provider responsible for creating
 * resource owner subject representations
 */
public interface SubjectCreator {


    /**
     * Create a {@link UserSubject}
     * @param mc the {@link MessageContext} of this request
     * @param params the request parameters
     * @return {@link UserSubject}
     * @throws OAuthServiceException
     */
    UserSubject createUserSubject(MessageContext mc,
                                  MultivaluedMap<String, String> params) throws OAuthServiceException;
}

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
package org.apache.cxf.systest.jaxrs.security.oauth2.common;

import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.saml.Saml2BearerGrantHandler;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

/**
 * Extend Saml2BearerGrantHandler not to use SamlUserSubject, which is not an entity and hence causes problems with JPA.
 */
public class JPASaml2BearerGrantHandler extends Saml2BearerGrantHandler {

    @Override
    protected UserSubject getGrantSubject(Message message, SamlAssertionWrapper wrapper) {
        UserSubject userSubject = super.getGrantSubject(message, wrapper);
        return new UserSubject(userSubject.getLogin(), userSubject.getRoles());
    }

}

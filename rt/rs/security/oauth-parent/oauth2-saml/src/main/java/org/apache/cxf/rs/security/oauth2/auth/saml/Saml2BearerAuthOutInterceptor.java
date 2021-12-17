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
package org.apache.cxf.rs.security.oauth2.auth.saml;

import jakarta.ws.rs.core.Form;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.saml.SamlFormOutInterceptor;

public class Saml2BearerAuthOutInterceptor extends SamlFormOutInterceptor {

    @Override
    protected void updateForm(Form form, String encodedToken) {
        form.param(Constants.CLIENT_AUTH_ASSERTION_TYPE, Constants.CLIENT_AUTH_SAML2_BEARER);
        form.param(Constants.CLIENT_AUTH_ASSERTION_PARAM, encodedToken);
    }

    @Override
    protected String encodeToken(String token) throws Base64Exception {
        return Base64UrlUtility.encode(token);
    }
}

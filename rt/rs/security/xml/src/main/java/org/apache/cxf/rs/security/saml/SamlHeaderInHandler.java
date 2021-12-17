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

package org.apache.cxf.rs.security.saml;

import java.util.List;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class SamlHeaderInHandler extends AbstractSamlBase64InHandler {

    private static final String SAML_AUTH = "SAML";

    @Context
    private HttpHeaders headers;

    @Override
    public void filter(ContainerRequestContext context) {
        Message message = JAXRSUtils.getCurrentMessage();

        List<String> values = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (values == null || values.size() != 1 || !values.get(0).startsWith(SAML_AUTH)) {
            throwFault("Authorization header must be available and use SAML profile", null);
        }

        String[] parts = values.get(0).split(" ");
        if (parts.length != 2) {
            throwFault("Authorization header is malformed", null);
        }

        handleToken(message, parts[1]);
    }



}

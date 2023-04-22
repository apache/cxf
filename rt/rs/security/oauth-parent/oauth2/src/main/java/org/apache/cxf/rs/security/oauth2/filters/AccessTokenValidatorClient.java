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
package org.apache.cxf.rs.security.oauth2.filters;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class AccessTokenValidatorClient implements AccessTokenValidator {

    private WebClient tokenValidatorClient;
    private List<String> supportedSchemes = new LinkedList<>();
    public List<String> getSupportedAuthorizationSchemes() {
        return supportedSchemes.isEmpty()
            ? Collections.singletonList(OAuthConstants.ALL_AUTH_SCHEMES)
            : Collections.unmodifiableList(supportedSchemes);
    }

    public AccessTokenValidation validateAccessToken(MessageContext mc,
                                                     String authScheme,
                                                     String authSchemeData,
                                                     MultivaluedMap<String, String> extraProps)
        throws OAuthServiceException {
        WebClient client = WebClient.fromClient(tokenValidatorClient, true);
        MultivaluedMap<String, String> props = new MetadataMap<>();
        props.putSingle(OAuthConstants.AUTHORIZATION_SCHEME_TYPE, authScheme);
        props.putSingle(OAuthConstants.AUTHORIZATION_SCHEME_DATA, authSchemeData);
        if (extraProps != null) {
            props.putAll(extraProps);
        }
        try {
            return client.post(props, AccessTokenValidation.class);
        } catch (WebApplicationException ex) {
            throw new OAuthServiceException(ex);
        }
    }

    public void setTokenValidatorClient(WebClient tokenValidatorClient) {
        this.tokenValidatorClient = tokenValidatorClient;
    }
    public void setSupportedSchemes(List<String> schemes) {
        this.supportedSchemes.addAll(schemes);
    }
    public void setSupportedScheme(String scheme) {
        this.supportedSchemes.add(scheme);
    }

}

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
package org.apache.cxf.rs.security.oidc.idp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.services.AuthorizationMetadataService;

public class OidcConfigurationService extends AuthorizationMetadataService {
    // Response types supported with the combination of 
    // AuthorizationCode, Implicit and Hybrid services
    private static final List<String> DEFAULT_RESPONSE_TYPES = 
        Arrays.asList("code", "code id_token", "id_token", "token id_token");
    // Required:
    private List<String> responseTypes;
    
    // Recommended - but optional
    private boolean userInfoEndpointNotAvailable;
    private String userInfoEndpointAddress;
    
    // Optional RP initiated logout
    private boolean endSessionEndpointNotAvailable;
    private String endSessionEndpointAddress;
    private boolean backChannelLogoutSupported;

    @GET
    @Path("openid-configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public String getConfiguration(@Context UriInfo ui) {
        return super.getConfiguration(ui);
    }

    @Override
    protected void prepareConfigurationData(Map<String, Object> cfg, String baseUri) {
        super.prepareConfigurationData(cfg, baseUri);
        // UriInfo Endpoint
        if (!isUserInfoEndpointNotAvailable()) {
            String theUserInfoEndpointAddress =
                calculateEndpointAddress(userInfoEndpointAddress, baseUri, "/users/userinfo");
            cfg.put("userinfo_endpoint", theUserInfoEndpointAddress);
        }

        Properties sigProps = JwsUtils.loadSignatureOutProperties(false);
        if (sigProps != null && sigProps.containsKey(JoseConstants.RSSEC_SIGNATURE_ALGORITHM)) {
            cfg.put("id_token_signing_alg_values_supported",
                    Collections.singletonList(sigProps.get(JoseConstants.RSSEC_SIGNATURE_ALGORITHM)));
        }
        
        // RP Initiated Logout Endpoint
        if (!isEndSessionEndpointNotAvailable()) {
            String theEndSessionEndpointAddress =
                calculateEndpointAddress(endSessionEndpointAddress, baseUri, "/idp/logout");
            cfg.put("end_session_endpoint", theEndSessionEndpointAddress);
        }
        
        if (isBackChannelLogoutSupported()) {
            cfg.put("backchannel_logout_supported", Boolean.TRUE);
        }
        
        //Subject types: pairwise is not supported yet
        cfg.put("subject_types_supported", Collections.singletonList("public"));
        
        List<String> theResponseTypes = responseTypes == null ? DEFAULT_RESPONSE_TYPES : responseTypes;
        cfg.put("response_types_supported", theResponseTypes);
    }

    public boolean isUserInfoEndpointNotAvailable() {
        return userInfoEndpointNotAvailable;
    }

    public void setUserInfoEndpointNotAvailable(boolean userInfoEndpointNotAvailable) {
        this.userInfoEndpointNotAvailable = userInfoEndpointNotAvailable;
    }

    public String getUserInfoEndpointAddress() {
        return userInfoEndpointAddress;
    }

    public void setUserInfoEndpointAddress(String userInfoEndpointAddress) {
        this.userInfoEndpointAddress = userInfoEndpointAddress;
    }

    public boolean isEndSessionEndpointNotAvailable() {
        return endSessionEndpointNotAvailable;
    }

    public void setEndSessionEndpointNotAvailable(boolean endSessionEndpointNotAvailable) {
        this.endSessionEndpointNotAvailable = endSessionEndpointNotAvailable;
    }

    public String getEndSessionEndpointAddress() {
        return endSessionEndpointAddress;
    }

    public void setEndSessionEndpointAddress(String endSessionEndpointAddress) {
        this.endSessionEndpointAddress = endSessionEndpointAddress;
    }

    public boolean isBackChannelLogoutSupported() {
        return backChannelLogoutSupported;
    }

    public void setBackChannelLogoutSupported(boolean backChannelLogoutSupported) {
        this.backChannelLogoutSupported = backChannelLogoutSupported;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

}

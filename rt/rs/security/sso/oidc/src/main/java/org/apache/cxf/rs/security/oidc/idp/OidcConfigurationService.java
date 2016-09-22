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

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Path;

import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.services.AuthorizationMetadataService;

@Path("openid-configuration")
public class OidcConfigurationService extends AuthorizationMetadataService {
    // Recommended - but optional
    private boolean userInfoEndpointNotAvailable;
    private String userInfoEndpointAddress;
        
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
    }

    public boolean isUserInfoEndpointNotAvailable() {
        return userInfoEndpointNotAvailable;
    }

    public void setUserInfoEndpointNotAvailable(boolean userInfoEndpointNotAvailable) {
        this.userInfoEndpointNotAvailable = userInfoEndpointNotAvailable;
    }
    
}

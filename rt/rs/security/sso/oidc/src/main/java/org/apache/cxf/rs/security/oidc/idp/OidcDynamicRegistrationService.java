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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistration;
import org.apache.cxf.rs.security.oauth2.services.ClientRegistrationResponse;
import org.apache.cxf.rs.security.oauth2.services.DynamicRegistrationService;

public class OidcDynamicRegistrationService extends DynamicRegistrationService {
    private boolean protectIdTokenWithClientSecret;
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public ClientRegistrationResponse register(OidcClientRegistration request) {
        return super.register(request);
    }
    
    @Override
    protected Client createNewClient(ClientRegistration request) {
        // TODO: cast to OidcClientRegistrationRequest, 
        // set OIDC specific properties as Client extra properties 
        return super.createNewClient(request);
    }
    
    protected int getClientSecretSizeInBytes(ClientRegistration request) {
           
        // TODO: may need to be 384/8 or 512/8 if not a default HS256 but HS384 or HS512
        int keySizeOctets = protectIdTokenWithClientSecret
            ? 32
            : super.getClientSecretSizeInBytes(request);
       
        return keySizeOctets;
    }
    public void setProtectIdTokenWithClientSecret(boolean protectIdTokenWithClientSecret) {
        this.protectIdTokenWithClientSecret = protectIdTokenWithClientSecret;
    }
}

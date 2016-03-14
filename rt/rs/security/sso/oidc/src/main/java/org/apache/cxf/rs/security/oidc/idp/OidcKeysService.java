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

import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

@Path("keys")
public class OidcKeysService {

    private volatile JsonWebKeys keySet;
    private WebClient keyServiceClient;
    
    @GET
    @Produces("application/json")
    public JsonWebKeys getPublicVerificationKeys() {
        if (keySet == null) {
            if (keyServiceClient == null) {
                keySet = getFromLocalStore();
            } else {
                keySet = keyServiceClient.get(JsonWebKeys.class);
            }
            
        }
        return keySet;
    }

    private static JsonWebKeys getFromLocalStore() {
        Properties props = JwsUtils.loadSignatureInProperties(true);
        return JwsUtils.loadPublicVerificationKeys(JAXRSUtils.getCurrentMessage(), props);
    }

    public void setKeyServiceClient(WebClient keyServiceClient) {
        this.keyServiceClient = keyServiceClient;
    }
    
}

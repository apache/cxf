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
package org.apache.cxf.rs.security.oauth2.services;

import java.util.Properties;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public class JwksService {

    private volatile JsonWebKeys keySet;
    private WebClient keyServiceClient;
    private boolean stripPrivateParameters = true;

    @Path("keys")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonWebKeys getPublicVerificationKeys() {
        if (keySet == null) {
            if (keyServiceClient == null) {
                keySet = getFromLocalStore(stripPrivateParameters);
            } else {
                keySet = keyServiceClient.get(JsonWebKeys.class);
            }

        }
        return keySet;
    }

    private static JsonWebKeys getFromLocalStore(boolean stripPrivateParameters) {
        Properties props = JwsUtils.loadSignatureInProperties(true);
        return JwsUtils.loadPublicVerificationKeys(JAXRSUtils.getCurrentMessage(), props, stripPrivateParameters);
    }

    public void setKeyServiceClient(WebClient keyServiceClient) {
        this.keyServiceClient = keyServiceClient;
    }

    public boolean isStripPrivateParameters() {
        return stripPrivateParameters;
    }

    /**
     * Whether to strip private parameters from the keys that are returned. The default is true.
     */
    public void setStripPrivateParameters(boolean stripPrivateParameters) {
        this.stripPrivateParameters = stripPrivateParameters;
    }

}

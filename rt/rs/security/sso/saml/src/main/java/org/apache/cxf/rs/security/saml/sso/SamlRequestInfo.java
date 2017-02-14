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
package org.apache.cxf.rs.security.saml.sso;

public class SamlRequestInfo {
    private String samlRequest;
    private String relayState;
    private String idpServiceAddress;
    private String webAppContext;
    private String webAppDomain;

    public void setSamlRequest(String encodedSaml) {
        this.samlRequest = encodedSaml;
    }
    public String getSamlRequest() {
        return samlRequest;
    }
    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }
    public String getRelayState() {
        return relayState;
    }
    public void setIdpServiceAddress(String idpServiceAddress) {
        this.idpServiceAddress = idpServiceAddress;
    }
    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }
    public void setWebAppContext(String webAppContext) {
        this.webAppContext = webAppContext;
    }
    public String getWebAppContext() {
        return webAppContext;
    }
    public String getWebAppDomain() {
        return webAppDomain;
    }
    public void setWebAppDomain(String webAppDomain) {
        this.webAppDomain = webAppDomain;
    }
}

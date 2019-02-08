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

import java.util.Collections;

import org.apache.cxf.message.Message;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;

/**
 * A default implementation of the AuthnRequestBuilder interface to create a SAML 2.0
 * Protocol AuthnRequest.
 */
public class DefaultAuthnRequestBuilder implements AuthnRequestBuilder {

    private boolean forceAuthn;
    private boolean isPassive;
    private String protocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    private String nameIDFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";

    /**
     * Create a SAML 2.0 Protocol AuthnRequest
     */
    public AuthnRequest createAuthnRequest(
        Message message,
        String issuerId,
        String assertionConsumerServiceAddress
    ) throws Exception {
        Issuer issuer =
            SamlpRequestComponentBuilder.createIssuer(issuerId);

        NameIDPolicy nameIDPolicy =
            SamlpRequestComponentBuilder.createNameIDPolicy(true, nameIDFormat, issuerId);

        AuthnContextClassRef authnCtxClassRef =
            SamlpRequestComponentBuilder.createAuthnCtxClassRef(
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
            );
        RequestedAuthnContext authnCtx =
            SamlpRequestComponentBuilder.createRequestedAuthnCtxPolicy(
                AuthnContextComparisonTypeEnumeration.EXACT,
                Collections.singletonList(authnCtxClassRef), null
            );

        //CHECKSTYLE:OFF
        return SamlpRequestComponentBuilder.createAuthnRequest(
                assertionConsumerServiceAddress,
                forceAuthn,
                isPassive,
                protocolBinding,
                SAMLVersion.VERSION_20,
                issuer,
                nameIDPolicy,
                authnCtx
        );

    }

    public boolean isForceAuthn() {
        return forceAuthn;
    }

    public void setForceAuthn(boolean forceAuthn) {
        this.forceAuthn = forceAuthn;
    }

    public boolean isPassive() {
        return isPassive;
    }

    public void setPassive(boolean isPassive) {
        this.isPassive = isPassive;
    }

    public String getProtocolBinding() {
        return protocolBinding;
    }

    public void setProtocolBinding(String protocolBinding) {
        this.protocolBinding = protocolBinding;
    }

    public String getNameIDFormat() {
        return nameIDFormat;
    }

    public void setNameIDFormat(String nameIDFormat) {
        this.nameIDFormat = nameIDFormat;
    }

}

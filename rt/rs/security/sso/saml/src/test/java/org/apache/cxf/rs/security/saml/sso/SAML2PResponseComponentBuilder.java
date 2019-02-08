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

import java.util.UUID;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;

/**
* A (basic) set of utility methods to construct SAML 2.0 Protocol Response statements
*/
public final class SAML2PResponseComponentBuilder {

    private static SAMLObjectBuilder<Response> responseBuilder;

    private static SAMLObjectBuilder<Issuer> issuerBuilder;

    private static SAMLObjectBuilder<Status> statusBuilder;

    private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

    private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

    private static SAMLObjectBuilder<AuthnContextClassRef> authnContextClassRefBuilder;

    private static XMLObjectBuilderFactory builderFactory =
        XMLObjectProviderRegistrySupport.getBuilderFactory();

    private SAML2PResponseComponentBuilder() {

    }

    @SuppressWarnings("unchecked")
    public static Response createSAMLResponse(
        String inResponseTo,
        String issuer,
        Status status
    ) {
        if (responseBuilder == null) {
            responseBuilder = (SAMLObjectBuilder<Response>)
                builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
        }
        Response response = responseBuilder.buildObject();

        response.setID(UUID.randomUUID().toString());
        response.setIssueInstant(new DateTime());
        response.setInResponseTo(inResponseTo);
        response.setIssuer(createIssuer(issuer));
        response.setStatus(status);
        response.setVersion(SAMLVersion.VERSION_20);

        return response;
    }

    @SuppressWarnings("unchecked")
    public static Issuer createIssuer(
        String issuerValue
    ) {
        if (issuerBuilder == null) {
            issuerBuilder = (SAMLObjectBuilder<Issuer>)
                builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
        }
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(issuerValue);

        return issuer;
    }

    @SuppressWarnings("unchecked")
    public static Status createStatus(
        String statusCodeValue,
        String statusMessage
    ) {
        if (statusBuilder == null) {
            statusBuilder = (SAMLObjectBuilder<Status>)
                builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME);
        }
        if (statusCodeBuilder == null) {
            statusCodeBuilder = (SAMLObjectBuilder<StatusCode>)
                builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
        }
        if (statusMessageBuilder == null) {
            statusMessageBuilder = (SAMLObjectBuilder<StatusMessage>)
                builderFactory.getBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);
        }

        Status status = statusBuilder.buildObject();

        StatusCode statusCode = statusCodeBuilder.buildObject();
        statusCode.setValue(statusCodeValue);
        status.setStatusCode(statusCode);

        if (statusMessage != null) {
            StatusMessage statusMessageObject = statusMessageBuilder.buildObject();
            statusMessageObject.setMessage(statusMessage);
            status.setStatusMessage(statusMessageObject);
        }

        return status;
    }

    @SuppressWarnings("unchecked")
    public static AuthnContextClassRef createAuthnContextClassRef(String newAuthnContextClassRef) {
        if (authnContextClassRefBuilder == null) {
            authnContextClassRefBuilder = (SAMLObjectBuilder<AuthnContextClassRef>)
                builderFactory.getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        }

        AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject();
        authnContextClassRef.setAuthnContextClassRef(newAuthnContextClassRef);

        return authnContextClassRef;
    }

}
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

package org.apache.cxf.rs.security.oauth2.saml;

import java.time.Instant;
import java.util.List;

import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;

public class SamlOAuthValidator {
    private String accessTokenServiceAddress;
    private String issuer;
    private String clientAddress;
    private boolean subjectConfirmationDataRequired;
    public SamlOAuthValidator() {
    }


    public void setSubjectConfirmationDataRequired(boolean required) {
        subjectConfirmationDataRequired = required;
    }

    public void setAccessTokenServiceAddress(String address) {
        accessTokenServiceAddress = address;
    }

    public void setIssuer(String value) {
        issuer = value;
    }

    public void setClientAddress(String value) {
        issuer = value;
    }

    public void validate(Message message, SamlAssertionWrapper wrapper) {
        validateSAMLVersion(wrapper);

        Conditions cs = wrapper.getSaml2().getConditions();
        validateAudience(message, cs);

        if (issuer != null) {
            String actualIssuer = getIssuer(wrapper);
            String expectedIssuer = OAuthConstants.CLIENT_ID.equals(issuer)
                ? wrapper.getSaml2().getSubject().getNameID().getValue() : issuer;
            if (actualIssuer == null || !actualIssuer.equals(expectedIssuer)) {
                throw ExceptionUtils.toNotAuthorizedException(null, null);
            }
        }
        if (!validateAuthenticationSubject(message, cs, wrapper.getSaml2().getSubject())) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    private void validateSAMLVersion(SamlAssertionWrapper assertionW) {
        if (assertionW.getSaml2() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    private String getIssuer(SamlAssertionWrapper assertionW) {
        Issuer samlIssuer = assertionW.getSaml2().getIssuer();
        return samlIssuer == null ? null : samlIssuer.getValue();
    }

    private void validateAudience(Message message, Conditions cs) {
        String absoluteAddress = getAbsoluteTargetAddress(message);

        List<AudienceRestriction> restrictions = cs.getAudienceRestrictions();
        for (AudienceRestriction ar : restrictions) {
            List<Audience> audiences = ar.getAudiences();
            for (Audience a : audiences) {
                if (absoluteAddress.equals(a.getAudienceURI())) {
                    return;
                }
            }
        }
        throw ExceptionUtils.toNotAuthorizedException(null, null);
    }

    private String getAbsoluteTargetAddress(Message m) {
        if (accessTokenServiceAddress == null) {
            return new UriInfoImpl(m).getAbsolutePath().toString();
        }
        if (!accessTokenServiceAddress.startsWith("http")) {
            String httpBasePath = (String)m.get("http.base.path");
            return UriBuilder.fromUri(httpBasePath)
                             .path(accessTokenServiceAddress)
                             .build()
                             .toString();
        }
        return accessTokenServiceAddress;
    }

    private boolean validateAuthenticationSubject(Message m,
                                                  Conditions cs,
                                                  org.opensaml.saml.saml2.core.Subject subject) {
        // We need to find a Bearer Subject Confirmation method
        boolean bearerSubjectConfFound = false;
        if (subject.getSubjectConfirmations() != null) {
            for (SubjectConfirmation subjectConf : subject.getSubjectConfirmations()) {
                if (SAML2Constants.CONF_BEARER.equals(subjectConf.getMethod())) {
                    validateSubjectConfirmation(m, cs, subjectConf.getSubjectConfirmationData());
                    bearerSubjectConfFound = true;
                }
            }
        }

        return bearerSubjectConfFound;
    }

      /**
       * Validate a (Bearer) Subject Confirmation
       */
    private void validateSubjectConfirmation(Message m,
                                             Conditions cs,
                                             SubjectConfirmationData subjectConfData) {
        if (subjectConfData == null) {
            if (!subjectConfirmationDataRequired
                && cs.getNotOnOrAfter() != null && !cs.getNotOnOrAfter().isBefore(Instant.now())) {
                return;
            }
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }

        // Recipient must match assertion consumer URL
        String recipient = subjectConfData.getRecipient();
        if (recipient == null || !recipient.equals(getAbsoluteTargetAddress(m))) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }

        // We must have a NotOnOrAfter timestamp
        if (subjectConfData.getNotOnOrAfter() == null
            || subjectConfData.getNotOnOrAfter().isBefore(Instant.now())) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }

        //TODO: replay cache, same as with SAML SSO case

        // Check address
        if (subjectConfData.getAddress() != null
            && (clientAddress == null || !subjectConfData.getAddress().equals(clientAddress))) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }


    }

}

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

package org.apache.cxf.rs.security.oauth2.auth.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.w3c.dom.Element;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.saml.SamlOAuthValidator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.saml.AbstractSamlInHandler;
import org.apache.cxf.rs.security.saml.SAMLUtils;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

public class Saml2BearerAuthHandler extends AbstractSamlInHandler {
    private FormEncodingProvider<Form> provider = new FormEncodingProvider<>(true);
    private SamlOAuthValidator samlOAuthValidator = new SamlOAuthValidator();

    public Saml2BearerAuthHandler() {
    }

    public void setSamlOAuthValidator(SamlOAuthValidator validator) {
        samlOAuthValidator = validator;
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message message = JAXRSUtils.getCurrentMessage();
        Form form = readFormData(message);
        MultivaluedMap<String, String> formData = form.asMap();
        String assertionType = formData.getFirst(Constants.CLIENT_AUTH_ASSERTION_TYPE);
        String decodedAssertionType = assertionType != null ? HttpUtils.urlDecode(assertionType) : null;
        if (decodedAssertionType == null || !Constants.CLIENT_AUTH_SAML2_BEARER.equals(decodedAssertionType)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        String assertion = formData.getFirst(Constants.CLIENT_AUTH_ASSERTION_PARAM);

        Element token = readToken(message, assertion);
        String clientId = formData.getFirst(OAuthConstants.CLIENT_ID);
        validateToken(message, token, clientId);


        formData.remove(OAuthConstants.CLIENT_ID);
        formData.remove(Constants.CLIENT_AUTH_ASSERTION_PARAM);
        formData.remove(Constants.CLIENT_AUTH_ASSERTION_TYPE);

        // restore input stream
        try {
            FormUtils.restoreForm(provider, form, message);
        } catch (Exception ex) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    private Form readFormData(Message message) {
        try {
            return FormUtils.readForm(provider, message);
        } catch (Exception ex) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    protected Element readToken(Message message, String assertion) {
        if (assertion == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        try {
            byte[] deflatedToken = Base64UrlUtility.decode(assertion);
            InputStream is = new ByteArrayInputStream(deflatedToken);
            return readToken(message, is);
        } catch (Base64Exception ex) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    protected void validateToken(Message message, Element element, String clientId) {

        SamlAssertionWrapper wrapper = toWrapper(element);
        // The common SAML assertion validation:
        // signature, subject confirmation, etc
        super.validateToken(message, wrapper);

        // This is specific to OAuth2 path
        // Introduce SAMLOAuth2Validator to be reused between auth and grant handlers
        Subject subject = SAMLUtils.getSubject(message, wrapper);
        if (subject.getName() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }

        if (clientId != null && !clientId.equals(subject.getName())) {
            //TODO:  Attempt to map client_id to subject.getName()
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        samlOAuthValidator.validate(message, wrapper);
        message.put(OAuthConstants.CLIENT_ID, subject.getName());
    }

}

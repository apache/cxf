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

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;

import org.w3c.dom.Element;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.saml.SamlOAuthValidator;
import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.saml.AbstractSamlInHandler;
import org.apache.cxf.rs.security.saml.SAMLUtils;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.ws.security.saml.ext.AssertionWrapper;

public class Saml2BearerAuthHandler extends AbstractSamlInHandler {
    private FormEncodingProvider<Form> provider = new FormEncodingProvider<Form>(true);
    private SamlOAuthValidator samlOAuthValidator = new SamlOAuthValidator();
    
    public Saml2BearerAuthHandler() {
    }
    
    public void setSamlOAuthValidator(SamlOAuthValidator validator) {
        samlOAuthValidator = validator;
    }
    
    public Response handleRequest(Message message, ClassResourceInfo resourceClass) {
        
        Form form = readFormData(message);    
        String assertionType = form.getData().getFirst(Constants.CLIENT_AUTH_ASSERTION_TYPE);
        String decodedAssertionType = assertionType != null ? HttpUtils.urlDecode(assertionType) : null;
        if (decodedAssertionType == null || !Constants.CLIENT_AUTH_SAML2_BEARER.equals(decodedAssertionType)) {
            throw new NotAuthorizedException(errorResponse());
        }
        String assertion = form.getData().getFirst(Constants.CLIENT_AUTH_ASSERTION_PARAM);
        
        Element token = readToken(message, assertion);         
        String clientId = form.getData().getFirst(OAuthConstants.CLIENT_ID);
        validateToken(message, token, clientId);
        
        
        form.getData().remove(OAuthConstants.CLIENT_ID);
        form.getData().remove(Constants.CLIENT_AUTH_ASSERTION_PARAM);
        form.getData().remove(Constants.CLIENT_AUTH_ASSERTION_TYPE);
        
        // restore input stream
        try {
            FormUtils.restoreForm(provider, form, message);
        } catch (Exception ex) {
            throw new NotAuthorizedException(errorResponse());
        }
        return null;
    }
    
    private Form readFormData(Message message) {
        try {
            return FormUtils.readForm(provider, message);
        } catch (Exception ex) {
            throw new NotAuthorizedException(errorResponse());    
        }
    }
    
    protected Element readToken(Message message, String assertion) {
        if (assertion == null) {
            throw new NotAuthorizedException(errorResponse());
        }
        try {
            byte[] deflatedToken = Base64UrlUtility.decode(assertion);
            InputStream is = new ByteArrayInputStream(deflatedToken); 
            return readToken(message, is); 
        } catch (Base64Exception ex) {
            throw new NotAuthorizedException(errorResponse());
        }         
    }
    
    protected void validateToken(Message message, Element element, String clientId) {
        
        AssertionWrapper wrapper = toWrapper(element);
        // The common SAML assertion validation:
        // signature, subject confirmation, etc
        super.validateToken(message, wrapper);
        
        // This is specific to OAuth2 path
        // Introduce SAMLOAuth2Validator to be reused between auth and grant handlers
        Subject subject = SAMLUtils.getSubject(message, wrapper);
        if (subject.getName() == null) {
            throw new NotAuthorizedException(errorResponse());  
        }
        
        if (clientId != null && !clientId.equals(subject.getName())) {
            //TODO:  Attempt to map client_id to subject.getName()
            throw new NotAuthorizedException(errorResponse());
        }
        samlOAuthValidator.validate(message, wrapper);
        message.put(OAuthConstants.CLIENT_ID, subject.getName());
    }
    
    private static Response errorResponse() {
        return Response.status(401).build();
    }
}

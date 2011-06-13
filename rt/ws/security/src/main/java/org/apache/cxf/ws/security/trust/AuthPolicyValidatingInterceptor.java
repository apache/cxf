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
package org.apache.cxf.ws.security.trust;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.Credential;

public class AuthPolicyValidatingInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AuthPolicyValidatingInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(AuthPolicyValidatingInterceptor.class);
    
    private STSTokenValidator validator;
    
    public AuthPolicyValidatingInterceptor() {
        this(Phase.UNMARSHAL);
    }
    
    public AuthPolicyValidatingInterceptor(String phase) {
        super(phase);
    }
    
    public void handleMessage(Message message) throws Fault {

        AuthorizationPolicy policy = (AuthorizationPolicy)message.get(AuthorizationPolicy.class);
        if (policy == null || policy.getUserName() == null || policy.getPassword() == null) {
            String name = null;
            String password = null;
            if (policy != null) {
                name = policy.getUserName();
                password = policy.getPassword();
            }
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_USER_PASSWORD", 
                                                       BUNDLE, 
                                                       name, password);
            LOG.warning(errorMsg.toString());
            throw new SecurityException(errorMsg.toString());
        }
        
        try {
            UsernameToken token = convertPolicyToToken(policy);
            Credential credential = new Credential();
            credential.setUsernametoken(token);
            validator.validateWithSTS(credential, message);
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }

    protected UsernameToken convertPolicyToToken(AuthorizationPolicy policy) 
        throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        UsernameToken token = new UsernameToken(false, doc, 
                                                WSConstants.PASSWORD_TEXT);
        token.setName(policy.getUserName());
        token.setPassword(policy.getPassword());
        return token;
    }

    public void setValidator(STSTokenValidator validator) {
        this.validator = validator;
    }
    
}

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
package org.apache.cxf.systest.sts.sendervouches;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.ws.security.saml.ext.bean.AttributeBean;
import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;
import org.apache.ws.security.saml.ext.bean.SubjectBean;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.opensaml.common.SAMLVersion;

/**
 * Create a SAML2 Assertion via some authenticated information (Principal).
 */
public class Saml2CallbackHandler implements CallbackHandler {
    
    private Principal principal;
    
    public Saml2CallbackHandler(Principal principal) {
        this.principal = principal;
    }
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setSamlVersion(SAMLVersion.VERSION_20);
                
                callback.setIssuer("intermediary");
                String subjectName = "uid=" + principal.getName();
                String confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;

                SubjectBean subjectBean = 
                    new SubjectBean(subjectName, null, confirmationMethod);
                callback.setSubject(subjectBean);
                
                AttributeStatementBean attrBean = new AttributeStatementBean();
                if (subjectBean != null) {
                    attrBean.setSubject(subjectBean);
                }
                AttributeBean attributeBean = new AttributeBean();
                attributeBean.setQualifiedName("role");
                attributeBean.setAttributeValues(Collections.singletonList("user"));
                attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
                callback.setAttributeStatementData(Collections.singletonList(attrBean));
            }
        }
    }
    
}

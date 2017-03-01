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

package org.apache.cxf.rt.security.saml.claims;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A CallbackHandler instance to mock up a SAML Attribute Assertion.
 */
public class SamlCallbackHandler implements CallbackHandler {
    private boolean saml2 = true;
    private String confirmationMethod = SAML2Constants.CONF_BEARER;
    private List<AttributeBean> attributes;

    public SamlCallbackHandler() {
        //
    }

    public SamlCallbackHandler(boolean saml2) {
        this.saml2 = saml2;
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                if (saml2) {
                    callback.setSamlVersion(Version.SAML_20);
                } else {
                    callback.setSamlVersion(Version.SAML_11);
                }

                callback.setIssuer("sts");
                String subjectName = "uid=sts-client,o=mock-sts.com";
                String subjectQualifier = "www.mock-sts.com";
                if (!saml2 && SAML2Constants.CONF_SENDER_VOUCHES.equals(confirmationMethod)) {
                    confirmationMethod = SAML1Constants.CONF_SENDER_VOUCHES;
                }
                SubjectBean subjectBean =
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                callback.setSubject(subjectBean);

                if (attributes != null) {
                    AttributeStatementBean attrBean = new AttributeStatementBean();
                    attrBean.setSubject(subjectBean);
                    attrBean.setSamlAttributes(attributes);
                    callback.setAttributeStatementData(Collections.singletonList(attrBean));
                }

            }
        }
    }

    public List<AttributeBean> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AttributeBean> attributes) {
        this.attributes = attributes;
    }

}

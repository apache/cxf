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
package org.apache.cxf.systest.sts.bearer;

import java.io.IOException;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * Create a SAML2 Bearer Assertion.
 */
public class Saml2CallbackHandler implements CallbackHandler {

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {

                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setSamlVersion(Version.SAML_20);

                callback.setIssuer("sts");
                String subjectName = "uid=alice";
                String confirmationMethod = SAML2Constants.CONF_BEARER;

                SubjectBean subjectBean =
                    new SubjectBean(subjectName, null, confirmationMethod);
                callback.setSubject(subjectBean);

                AttributeStatementBean attrBean = new AttributeStatementBean();
                if (subjectBean != null) {
                    attrBean.setSubject(subjectBean);
                }
                AttributeBean attributeBean = new AttributeBean();
                attributeBean.setQualifiedName("role");
                attributeBean.addAttributeValue("user");
                attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
                callback.setAttributeStatementData(Collections.singletonList(attrBean));
            }
        }
    }

}

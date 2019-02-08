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
package org.apache.cxf.sts.token.provider;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.SubjectLocalityBean;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A custom AuthenticationStatementProvider implementation for use in the tests.
 */
public class CustomAuthenticationProvider implements AuthenticationStatementProvider {

    /**
     * Get an AuthenticationStatementBean using the given parameters.
     */
    public AuthenticationStatementBean getStatement(TokenProviderParameters providerParameters) {
        AuthenticationStatementBean authBean = new AuthenticationStatementBean();

        SubjectLocalityBean subjectLocality = new SubjectLocalityBean();
        subjectLocality.setIpAddress("127.0.0.1");
        authBean.setSubjectLocality(subjectLocality);

        if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(
                providerParameters.getTokenRequirements().getTokenType())) {
            authBean.setAuthenticationMethod(SAML1Constants.AUTH_METHOD_X509);
        } else {
            authBean.setAuthenticationMethod(SAML2Constants.AUTH_CONTEXT_CLASS_REF_X509);
        }
        return authBean;
    }

}

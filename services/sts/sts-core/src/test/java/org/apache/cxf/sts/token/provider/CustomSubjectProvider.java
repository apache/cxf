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

import java.security.Principal;

import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A test implementation of SubjectProvider.
 */
public class CustomSubjectProvider implements SubjectProvider {

    private String subjectNameQualifier = "http://cxf.apache.org/sts/custom";

    /**
     * Get a SubjectBean object.
     */
    public SubjectBean getSubject(SubjectProviderParameters subjectProviderParameters) {
        TokenProviderParameters providerParameters = subjectProviderParameters.getProviderParameters();
        TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();

        String tokenType = tokenRequirements.getTokenType();
        String keyType = keyRequirements.getKeyType();
        String confirmationMethod = getSubjectConfirmationMethod(tokenType, keyType);

        Principal principal = providerParameters.getPrincipal();
        return new SubjectBean(principal.getName(), subjectNameQualifier, confirmationMethod);
    }

    /**
     * Get the SubjectConfirmation method given a tokenType and keyType
     */
    private String getSubjectConfirmationMethod(String tokenType, String keyType) {
        if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML2_NS.equals(tokenType)) {
            if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)
                || STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
                return SAML2Constants.CONF_HOLDER_KEY;
            }
            return SAML2Constants.CONF_BEARER;
        }
        return extracted(keyType);
    }

    private String extracted(String keyType) {
        if (STSConstants.SYMMETRIC_KEY_KEYTYPE.equals(keyType)
            || STSConstants.PUBLIC_KEY_KEYTYPE.equals(keyType)) {
            return SAML1Constants.CONF_HOLDER_KEY;
        }
        return SAML1Constants.CONF_BEARER;
    }


}

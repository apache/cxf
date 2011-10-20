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
package org.apache.cxf.systest.sts.secure_conv;

import org.w3c.dom.Document;

import org.apache.cxf.ws.security.trust.STSTokenValidator;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.validate.Credential;

/**
 * This class validates a SecurityContextToken by dispatching it to an STS. It then
 * checks that we get back a SAML2 Assertion from the STS, and extracts the secret from it.
 */
public class SCTTokenValidator extends STSTokenValidator {
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential validatedCredential = super.validate(credential, data);
        
        AssertionWrapper transformedToken = validatedCredential.getTransformedToken();
        if (transformedToken == null || transformedToken.getSaml2() == null
            || !"DoubleItSTSIssuer".equals(transformedToken.getIssuerString())) {
            throw new WSSecurityException(WSSecurityException.FAILURE);
        }

        Document doc = transformedToken.getElement().getOwnerDocument();
        transformedToken.parseHOKSubject(data, new WSDocInfo(doc));
        SAMLKeyInfo keyInfo = transformedToken.getSubjectKeyInfo();
        byte[] secret = keyInfo.getSecret();
        validatedCredential.setSecretKey(secret);
        
        return validatedCredential;
    }

}

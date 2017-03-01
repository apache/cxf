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
package org.apache.cxf.systest.sts.custom_onbehalfof;

import org.apache.cxf.ws.security.trust.STSTokenValidator;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;

/**
 * This class validates a custom BinarySecurityToken by dispatching it to an STS. It then
 * checks that we get back a SAML2 Assertion from the STS.
 */
public class CustomBSTTokenValidator extends STSTokenValidator {

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential validatedCredential = super.validate(credential, data);

        SamlAssertionWrapper transformedToken = validatedCredential.getTransformedToken();
        if (transformedToken == null || transformedToken.getSaml2() == null
            || !"DoubleItSTSIssuer".equals(transformedToken.getIssuerString())) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
        }

        return validatedCredential;
    }

}

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
package org.apache.cxf.sts.request;

import java.util.List;

import javax.xml.ws.WebServiceContext;

import org.w3c.dom.Element;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.builder.SAML1Constants;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;

/**
 * This DelegationHandler implementation extends the Default implementation to allow UsernameTokens
 * for OnBehalfOf/ActAs
 */
public class UsernameTokenDelegationHandler extends DefaultDelegationHandler {
    
    /**
     * Is Delegation allowed for a particular token
     */
    protected boolean isDelegationAllowed(
        WebServiceContext context,
        ReceivedToken receivedToken, 
        String appliesToAddress
    ) {
        if (receivedToken.isUsernameToken()) {
            return true;
        }
        
        // It must be a SAML Token
        if (!isSAMLToken(receivedToken)) {
            return false;
        }

        Element validateTargetElement = (Element)receivedToken.getToken();
        try {
            AssertionWrapper assertion = new AssertionWrapper(validateTargetElement);

            for (String confirmationMethod : assertion.getConfirmationMethods()) {
                if (!(SAML1Constants.CONF_BEARER.equals(confirmationMethod)
                    || SAML2Constants.CONF_BEARER.equals(confirmationMethod))) {
                    return false;
                }
            }

            if (appliesToAddress != null) {
                List<String> addresses = getAudienceRestrictions(assertion);
                if (!(addresses.isEmpty() || addresses.contains(appliesToAddress))) {
                    return false;
                }
            }
        } catch (WSSecurityException ex) {
            return false;
        }

        return true;
    }
    
}
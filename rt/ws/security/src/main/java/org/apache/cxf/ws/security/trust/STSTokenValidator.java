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


import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;

/**
 * 
 */
public class STSTokenValidator implements Validator {
    private STSSamlAssertionValidator samlValidator = new STSSamlAssertionValidator();
    private boolean alwaysValidateToSts;
    
    public STSTokenValidator() {
    }
    
    /**
     * Construct a new instance.
     * @param alwaysValidateToSts whether to always validate the token to the STS
     */
    public STSTokenValidator(boolean alwaysValidateToSts) {
        this.alwaysValidateToSts = alwaysValidateToSts;
    }
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        SoapMessage m = (SoapMessage)data.getMsgContext();
        SecurityToken token = new SecurityToken();
        
        try {
            if (credential.getAssertion() != null) {
                if (!alwaysValidateToSts) {
                    //
                    // Try to validate the Assertion locally first. If trust verification fails
                    // then send it off to the STS for validation
                    //
                    samlValidator.validate(credential, data);
                    if (samlValidator.isTrustVerificationSucceeded()) {
                        return credential;
                    }
                }
                token.setToken(credential.getAssertion().getElement());
            } else if (credential.getUsernametoken() != null) {
                token.setToken(credential.getUsernametoken().getElement());
            } else if (credential.getBinarySecurityToken() != null) {
                token.setToken(credential.getBinarySecurityToken().getElement());
            }
            
            STSClient c = STSUtils.getClient(m, "sts");
            synchronized (c) {
                System.setProperty("noprint", "true");
                c.validateSecurityToken(token);
                return credential;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity", null, e);
        }
    }

}

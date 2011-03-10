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
    Validator delegate;
    
    public STSTokenValidator() {
    }
    public STSTokenValidator(Validator delegate) {
        this.delegate = delegate;
    }
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (delegate != null) {
            credential = delegate.validate(credential, data);
        }
        SoapMessage m = (SoapMessage)data.getMsgContext();
        SecurityToken token = new SecurityToken();
        
        try {
            token.setToken(credential.getAssertion().getElement());
            
            STSClient c = STSUtils.getClient(m);
            synchronized (c) {
                System.setProperty("noprint", "true");
                if (c.validateSecurityToken(token)) {
                    return credential;
                } 
                System.clearProperty("noprint");
            }
        } catch (Exception e) {
            throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity", null, e);
        }
        throw new WSSecurityException(WSSecurityException.FAILURE, "invalidSAMLsecurity");
    }

}

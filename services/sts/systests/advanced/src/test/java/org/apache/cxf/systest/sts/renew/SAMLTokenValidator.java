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
package org.apache.cxf.systest.sts.renew;

import org.apache.cxf.ws.security.trust.STSTokenValidator;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.validate.Credential;

/**
 * This class validates a SAML Token by dispatching it to an STS. It pauses first to make sure
 * that the SAML Token expires (one of the tokens it receives should be close to expiration).
 */
public class SAMLTokenValidator extends STSTokenValidator {
    
    public SAMLTokenValidator() {
        super(true);
    }
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        // Sleep to make sure token gets replicated
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return super.validate(credential, data);
    }

}

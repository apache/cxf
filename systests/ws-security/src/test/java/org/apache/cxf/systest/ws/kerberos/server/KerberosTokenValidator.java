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
package org.apache.cxf.systest.ws.kerberos.server;

import java.security.Principal;
import java.util.Arrays;

import org.apache.ws.security.CustomTokenPrincipal;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.Validator;

/**
 * This class does some trivial validation of a received (mock) Kerberos token.
 */
public class KerberosTokenValidator implements Validator {
    
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        BinarySecurity binarySecurity = credential.getBinarySecurityToken();
        
        if (binarySecurity instanceof KerberosSecurity) {
            byte[] token = binarySecurity.getToken();
            if (!Arrays.equals(token, "8721958125981".getBytes())) {
                throw new WSSecurityException(WSSecurityException.FAILURE);
            }
            
            Principal principal = new CustomTokenPrincipal("Authenticated Principal");
            credential.setPrincipal(principal);
        }
        
        return credential;
    }

}

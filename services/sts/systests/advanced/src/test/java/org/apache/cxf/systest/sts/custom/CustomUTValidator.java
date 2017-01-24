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

package org.apache.cxf.systest.sts.custom;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;
import org.apache.wss4j.dom.validate.Validator;

/**
 * A Validator that checks for a custom "realm" parameter in the RST request and only allows 
 * authentication if the value is equal to "custom-realm".
 */
public class CustomUTValidator implements Validator {

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getUsernametoken() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
        }
        
        // Find custom Element in the SOAP Body
        Document doc = credential.getUsernametoken().getElement().getOwnerDocument();
        Element soapBody = WSSecurityUtil.findBodyElement(doc);
        Element realm = XMLUtils.findElement(soapBody, "realm", "http://cxf.apache.org/custom");
        if (realm != null) {
            String realmStr = realm.getTextContent();
            if ("custom-realm".equals(realmStr)) {

                UsernameTokenValidator validator = new UsernameTokenValidator();
                return validator.validate(credential, data);
            }
        }
        
        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
    }

}

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


import org.w3c.dom.Element;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.handler.RequestData;
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

        // Need to use SAAJ to get the SOAP Body as we are just using the UsernameTokenInterceptor
        SOAPMessage soapMessage = getSOAPMessage((SoapMessage)data.getMsgContext());
        try {
            Element soapBody = SAAJUtils.getBody(soapMessage);

            if (soapBody != null) {
                // Find custom Element in the SOAP Body
                Element realm = XMLUtils.findElement(soapBody, "realm", "http://cxf.apache.org/custom");
                if (realm != null) {
                    String realmStr = realm.getTextContent();
                    if ("custom-realm".equals(realmStr)) {

                        UsernameTokenValidator validator = new UsernameTokenValidator();
                        return validator.validate(credential, data);
                    }
                }
            }
        } catch (SOAPException ex) {
            // ignore
        }

        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
    }

    private SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }
}

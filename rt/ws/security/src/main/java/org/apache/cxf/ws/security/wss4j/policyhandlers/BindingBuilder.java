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

package org.apache.cxf.ws.security.wss4j.policyhandlers;

import java.util.Collection;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecUsernameToken;

/**
 * 
 */
public class BindingBuilder {
    SOAPMessage saaj;
    WSSecHeader secHeader;
    AssertionInfoMap aim;
    Binding binding;
    SoapMessage message;
    
    public BindingBuilder(Binding binding,
                           SOAPMessage saaj,
                           WSSecHeader secHeader,
                           AssertionInfoMap aim,
                           SoapMessage message) {
        this.binding = binding;
        this.aim = aim;
        this.secHeader = secHeader;
        this.saaj = saaj;
        this.message = message;
    }

    
    private boolean isRequestor() {
        return Boolean.TRUE.equals(message.containsKey(
            org.apache.cxf.message.Message.REQUESTOR_ROLE));
    }  
    
    
    protected void handleSupportingTokens(SupportingToken suppTokens) {
        for (Token token : suppTokens.getTokens()) {
            if (token instanceof UsernameToken) {
                WSSecUsernameToken utBuilder = addUsernameToken((UsernameToken)token);
                if (utBuilder != null) {
                    utBuilder.prepare(saaj.getSOAPPart());
                    utBuilder.appendToHeader(secHeader);
                }
            }
        }
    }
    
    
    
    protected WSSecUsernameToken addUsernameToken(UsernameToken token) {
        
        AssertionInfo info = null;
        Collection<AssertionInfo> ais = aim.getAssertionInfo(token.getName());
        for (AssertionInfo ai : ais) {
            if (ai.getAssertion() == token) {
                info = ai;
                if (!isRequestor()) {
                    info.setAsserted(true);
                    return null;
                }
            }
        }
        
        String userName = (String)message.getContextualProperty(SecurityConstants.USERNAME);
        
        if (!StringUtils.isEmpty(userName)) {
            // If NoPassword property is set we don't need to set the password
            if (token.isNoPassword()) {
                WSSecUsernameToken utBuilder = new WSSecUsernameToken();
                utBuilder.setUserInfo(userName, null);
                utBuilder.setPasswordType(null);
                info.setAsserted(true);
                return utBuilder;
            }
            
            String password = (String)message.getContextualProperty(SecurityConstants.PASSWORD);
            if (StringUtils.isEmpty(password)) {
                
                //Then try to get the password from the given callback handler
                CallbackHandler handler 
                    = (CallbackHandler)message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
            
                if (handler == null) {
                    info.setNotAsserted("No callback handler and not password available");
                    return null;
                }
                
                WSPasswordCallback[] cb = {new WSPasswordCallback(userName,
                                                                  WSPasswordCallback.USERNAME_TOKEN)};
                try {
                    handler.handle(cb);
                } catch (Exception e) {
                    //REVISIT - Exception?
                }
                
                //get the password
                password = cb[0].getPassword();
            }
            
            if (!StringUtils.isEmpty(password)) {
                //If the password is available then build the token
                WSSecUsernameToken utBuilder = new WSSecUsernameToken();
                if (token.isHashPassword()) {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_DIGEST);  
                } else {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_TEXT);
                }
                
                utBuilder.setUserInfo(userName, password);
                info.setAsserted(true);
                return utBuilder;
            } else {
                info.setNotAsserted("No password available");
            }
        } else {
            info.setNotAsserted("No username available");
        }
        return null;
    }

}

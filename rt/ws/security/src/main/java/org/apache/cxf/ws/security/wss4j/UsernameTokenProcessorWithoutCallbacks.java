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

package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.processor.Processor;


/**
 * UsernameToken processor which creates Principal 
 * without delegating to CallbackHandlers
 */
public class UsernameTokenProcessorWithoutCallbacks implements Processor {
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(UsernameTokenProcessorWithoutCallbacks.class);
    
    private String utId;
    private UsernameToken ut;
    
    @SuppressWarnings("unchecked")
    public void handleToken(Element elem, Crypto crypto, Crypto decCrypto, CallbackHandler cb, 
        WSDocInfo wsDocInfo, Vector returnResults, WSSConfig wsc) throws WSSecurityException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Found UsernameToken list element");
        }
        
        Principal principal = handleUsernameToken((Element) elem, cb);
        returnResults.add(
            0, 
            new WSSecurityEngineResult(WSConstants.UT, principal, null, null, null)
        );
        utId = ut.getID();
    }
    
    private WSUsernameTokenPrincipal handleUsernameToken(
        Element token, CallbackHandler cb) throws WSSecurityException {
        //
        // Parse the UsernameToken element
        //
        ut = new UsernameToken(token, false);
        String user = ut.getName();
        String password = ut.getPassword();
        String nonce = ut.getNonce();
        String createdTime = ut.getCreated();
        String pwType = ut.getPasswordType();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("UsernameToken user " + user);
            LOG.fine("UsernameToken password " + password);
        }
        
        return createPrincipal(user, password, ut.isHashed(), nonce, createdTime, pwType);
    }

    protected WSUsernameTokenPrincipal createPrincipal(String user, 
                                                       String password,
                                                       boolean isHashed,
                                                       String nonce,
                                                       String createdTime,
                                                       String pwType) throws WSSecurityException {
        WSUsernameTokenPrincipal principal = new WSUsernameTokenPrincipal(user, isHashed);
        principal.setNonce(nonce);
        principal.setPassword(password);
        principal.setCreatedTime(createdTime);
        principal.setPasswordType(pwType);

        return principal;
    }
    
    public String getId() {
        return utId;
    }
}

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

package org.apache.cxf.systest.jaxrs.security.common;

import java.io.IOException;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;

public final class SecurityUtils {
    
    private SecurityUtils() {
        
    }
    
    public static Crypto getCrypto(Message message,
                            String cryptoKey, 
                            String propKey) 
        throws IOException, WSSecurityException {
        return new CryptoLoader().getCrypto(message, cryptoKey, propKey); 
    }
    
    public static String getUserName(Message message, Crypto crypto, String userNameKey) {
        String user = (String)message.getContextualProperty(userNameKey);
        if (crypto != null && StringUtils.isEmpty(user)) {
            try {
                user = crypto.getDefaultX509Identifier();
            } catch (WSSecurityException e1) {
                throw new Fault(e1);
            }
        }
        return user;
    }
    
    public static String getPassword(Message message, String userName, 
                                     int type, Class<?> callingClass) {
        CallbackHandler handler = getCallbackHandler(message, callingClass);
        if (handler == null) {
            return null;
        }
        
        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, type)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            return null;
        }
        
        //get the password
        String password = cb[0].getPassword();
        return password == null ? "" : password;
    }
    
    public static CallbackHandler getCallbackHandler(Message message, Class<?> callingClass) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
    
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, callingClass).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
 
}

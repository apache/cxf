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
package org.apache.cxf.interceptor.security;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;

public class NamePasswordCallbackHandler implements CallbackHandler {  
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(NamePasswordCallbackHandler.class);
    private static final Logger LOG = LogUtils.getL7dLogger(NamePasswordCallbackHandler.class);
    
    private String username;  
    private String password;  
     
    public NamePasswordCallbackHandler(String username, String password) {  
        this.username = username;  
        this.password = password;  
    }  
     
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {  
        for (int i = 0; i < callbacks.length; i++) {  
            Callback callback = callbacks[i];
            if (handleCallback(callback)) {
                continue;
            } else if (callback instanceof NameCallback) {  
                ((NameCallback) callback).setName(username);  
            } else if (callback instanceof PasswordCallback) {  
                PasswordCallback pwCallback = (PasswordCallback) callback;  
                pwCallback.setPassword(password.toCharArray());  
            } else {
                org.apache.cxf.common.i18n.Message errorMsg = 
                    new org.apache.cxf.common.i18n.Message("UNSUPPORTED_CALLBACK_TYPE", 
                                                           BUNDLE, 
                                                           callbacks[i].getClass().getName());
                LOG.severe(errorMsg.toString());
                throw new UnsupportedCallbackException(callbacks[i], errorMsg.toString());  
            }  
        }  
    }      
    
    protected boolean handleCallback(Callback callback) {
        return false;
    }
}

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
package org.apache.cxf.rs.security.oauth2.grants.owner;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.security.JAASLoginInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.security.SecurityContext;

public class JAASResourceOwnerLoginHandler implements ResourceOwnerLoginHandler {
    
    private JAASLoginInterceptor jaasInterceptor = new JAASLoginInterceptor();
    
    
    public UserSubject createSubject(String name, String password) {
        Message message = setupMessage(name, password);
        jaasInterceptor.handleMessage(message);
        
        return OAuthUtils.createSubject(message.get(SecurityContext.class));
    }

    public void setContextName(String name) {
        jaasInterceptor.setContextName(name);
    }
    
    public void setRoleClassifier(String value) {
        jaasInterceptor.setRoleClassifier(value);
    }
    
    public void setRoleClassifierType(String value) {
        jaasInterceptor.setRoleClassifierType(value);
    }
    
    private Message setupMessage(String name, String password) {
        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setUserName(name);
        policy.setPassword(password);
        Message message = new MessageImpl();
        message.put(AuthorizationPolicy.class, policy);
        return message;
    }
}

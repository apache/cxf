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
package org.apache.cxf.rs.security.oauth.services;

import javax.ws.rs.core.Context;

import net.oauth.OAuthValidator;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth.provider.DefaultOAuthValidator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthUtils;

/**
 * Abstract utility class which OAuth services extend
 */
public abstract class AbstractOAuthService {
    private MessageContext mc;
    
    private OAuthDataProvider dataProvider;
    private OAuthValidator validator = new DefaultOAuthValidator();
    
    @Context 
    public void setMessageContext(MessageContext context) {
        this.mc = context;    
    }
    
    public MessageContext getMessageContext() {
        return mc;
    }
    
    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    protected OAuthDataProvider getDataProvider() {
        return OAuthUtils.getOAuthDataProvider(dataProvider, mc.getServletContext());
    }

    public OAuthValidator getValidator() {
        return validator;
    }

    public void setValidator(OAuthValidator validator) {
        this.validator = validator;
    }
    
}

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
package org.apache.cxf.rs.security.saml.sso;

import java.util.Date;

import javax.ws.rs.Path;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.rs.security.saml.sso.state.SPStateManager;

@Path("sso")
public class AbstractSSOSpHandler {
    private SPStateManager stateProvider;
    private long stateTimeToLive = SSOConstants.DEFAULT_STATE_TIME;
    
    protected String createCookie(String name, String value, String path) { 
        
        String contextCookie = name + "=" + value;
        // Make sure all the SP application filters can get this token;
        // Path property should be enough for a single container, Domain
        // property may need to be used for more complex environments
        if (path != null) {
            contextCookie += ";Path=" + path;
        }
        // Keep the cookie across the browser restarts until it actually expires.
        // Note that the Expires property has been deprecated but apparently is 
        // supported better than 'max-age' property by different browsers 
        // (Firefox, IE, etc)
        Date expiresDate = new Date(System.currentTimeMillis() + stateTimeToLive);
        String cookieExpires = HttpUtils.getHttpDateFormat().format(expiresDate);
        contextCookie += ";Expires=" + cookieExpires;
        //TODO: Consider adding an 'HttpOnly' attribute        
        
        return contextCookie;
    }
    
    protected boolean isStateExpired(long stateCreatedAt) {
        return new Date().after(new Date(stateCreatedAt + getStateTimeToLive()));
    }
    
    public void setStateProvider(SPStateManager stateProvider) {
        this.stateProvider = stateProvider;
    }

    public SPStateManager getStateProvider() {
        return stateProvider;
    }

    public void setStateTimeToLive(long stateTimeToLive) {
        this.stateTimeToLive = stateTimeToLive;
    }

    public long getStateTimeToLive() {
        return stateTimeToLive;
    }
}

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

import java.util.ResourceBundle;
import java.util.logging.Logger;

import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.saml.sso.state.SPStateManager;
import org.apache.cxf.security.SecurityContext;

@Path("logout")
public class LogoutService {
    protected static final Logger LOG = LogUtils.getL7dLogger(LogoutService.class);
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(LogoutService.class);
    private SPStateManager stateProvider;

    private String mainApplicationAddress;

    @GET
    @Produces("text/html")
    public LogoutResponse logout(@CookieParam(SSOConstants.SECURITY_CONTEXT_TOKEN) Cookie context,
                       @Context SecurityContext sc) {
        doLogout(context, sc);
        // Use View Handler to tell the user that the logout has been successful,
        // optionally listing the user login name and/or linking to the main application address,
        // the user may click on it, will be redirected to IDP and the process will start again
        return new LogoutResponse(sc.getUserPrincipal().getName(), mainApplicationAddress);
    }

    @POST
    @Produces("text/html")
    public LogoutResponse postLogout(@CookieParam(SSOConstants.SECURITY_CONTEXT_TOKEN) Cookie context,
                                     @Context SecurityContext sc) {
        return logout(context, sc);
    }



    private void doLogout(Cookie context, SecurityContext sc) {
        if (context == null || sc.getUserPrincipal() == null || sc.getUserPrincipal().getName() == null) {
            reportError("MISSING_RESPONSE_STATE");
            throw ExceptionUtils.toBadRequestException(null, null);
        }
        stateProvider.removeResponseState(context.getValue());
    }

    protected void reportError(String code) {
        org.apache.cxf.common.i18n.Message errorMsg =
            new org.apache.cxf.common.i18n.Message(code, BUNDLE);
        LOG.warning(errorMsg.toString());
    }

    public void setStateProvider(SPStateManager stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void setMainApplicationAddress(String mainApplicationAddress) {
        this.mainApplicationAddress = mainApplicationAddress;
    }

}

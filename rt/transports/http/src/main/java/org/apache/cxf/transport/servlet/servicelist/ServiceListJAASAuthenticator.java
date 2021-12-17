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
package org.apache.cxf.transport.servlet.servicelist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.transport.http.blueprint.HttpDestinationBPBeanDefinitionParser;



public class ServiceListJAASAuthenticator {

    private static final Logger LOG = LogUtils.getL7dLogger(HttpDestinationBPBeanDefinitionParser.class);

    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    private String realm;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }


    public Object authenticate(final String username, final String password) {
        return doAuthenticate(username, password);
    }

    public Subject doAuthenticate(final String username, final String password) {
        try {
            Subject subject = new Subject();
            LoginContext loginContext = new LoginContext(realm, subject, new CallbackHandler() {
                public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            ((NameCallback)callbacks[i]).setName(username);
                        } else if (callbacks[i] instanceof PasswordCallback) {
                            ((PasswordCallback)callbacks[i]).setPassword(
                                password == null ? null : password.toCharArray());
                        } else {
                            throw new UnsupportedCallbackException(callbacks[i]);
                        }
                    }
                }
            });
            loginContext.login();
            return subject;
        } catch (FailedLoginException e) {
            LOG.log(Level.FINE, "Login failed ", e);
            return null;
        } catch (AccountException e) {
            LOG.log(Level.WARNING, "Account failure ",  e);
            return null;
        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "General Security Exception ", e);
            return null;
        }
    }

    public boolean authenticate(HttpServletRequest request, HttpServletResponse response) {
        // Return immediately if the header is missing
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.length() > 0) {

            // Get the authType (Basic, Digest) and authInfo (user/password)
            // from the header
            authHeader = authHeader.trim();
            int blank = authHeader.indexOf(' ');
            if (blank > 0) {
                String authType = authHeader.substring(0, blank);
                String authInfo = authHeader.substring(blank).trim();


                if (authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
                    try {
                        String srcString = base64Decode(authInfo);

                        int i = srcString.indexOf(':');
                        String username = srcString.substring(0, i);
                        String password = srcString.substring(i + 1);

                        // authenticate
                        Subject subject = doAuthenticate(username, password);
                        if (subject != null) {
                            return true;
                        }

                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        // request authentication
        try {
            response.setHeader(HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\""
                                                        + this.realm + "\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            // failed sending the response ... cannot do anything about it
        }

        // inform HttpService that authentication failed
        return false;
    }

    private static String base64Decode(String srcString) {
        try {
            byte[] transformed = Base64Utility.decode(srcString);
            return new String(transformed, StandardCharsets.ISO_8859_1);
        } catch (Base64Exception e) {
            return srcString;
        }
    }

}

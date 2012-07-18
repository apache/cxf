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
package org.apache.cxf.transport.http.auth;

import java.net.URL;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

public abstract class AbstractSpnegoAuthSupplier {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractSpnegoAuthSupplier.class);
    
    /**
     * Can be set on the client properties. If set to true then the kerberos oid is used
     * instead of the default spnego OID
     */
    private static final String PROPERTY_USE_KERBEROS_OID = "auth.spnego.useKerberosOid";
    private static final String PROPERTY_REQUIRE_CRED_DELEGATION = "auth.spnego.requireCredDelegation";
    
    private static final String KERBEROS_OID = "1.2.840.113554.1.2.2";
    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    private String servicePrincipalName;
    private String realm;
    private boolean credDelegation;
    
    public String getAuthorization(AuthorizationPolicy authPolicy,
                                   URL currentURL,
                                   Message message) {
        if (!HttpAuthHeader.AUTH_TYPE_NEGOTIATE.equals(authPolicy.getAuthorizationType())) {
            return null;
        }
        try {
            String spn = getCompleteServicePrincipalName(currentURL);
            
            boolean useKerberosOid = MessageUtils.isTrue(
                message.getContextualProperty(PROPERTY_USE_KERBEROS_OID));
            Oid oid = new Oid(useKerberosOid ? KERBEROS_OID : SPNEGO_OID);

            byte[] token = getToken(authPolicy, spn, oid, message);
            return HttpAuthHeader.AUTH_TYPE_NEGOTIATE + " " + Base64Utility.encode(token);
        } catch (LoginException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (GSSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Create and return service ticket token
     * 
     * @param authPolicy
     * @param context
     * @return
     * @throws GSSException
     * @throws LoginException
     */
    private byte[] getToken(AuthorizationPolicy authPolicy,
                            final GSSContext context) throws GSSException,
        LoginException {
        final byte[] token = new byte[0];

        if (authPolicy == null || StringUtils.isEmpty(authPolicy.getUserName())) {
            return context.initSecContext(token, 0, token.length);
        }

        LoginContext lc = new LoginContext(authPolicy.getAuthorization(), getUsernamePasswordHandler(
            authPolicy.getUserName(), authPolicy.getPassword()));
        lc.login();
        
        try {
            return (byte[])Subject.doAs(lc.getSubject(), new CreateServiceTicketAction(context, token));
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof GSSException) {
                throw (GSSException) e.getCause();
            }
            LOG.log(Level.SEVERE, "initSecContext", e);
            return null;
        }
    }

    /**
     * Create and return a service ticket token for a given service principal
     * name
     * 
     * @param authPolicy
     * @param spn
     * @return service ticket token
     * @throws GSSException
     * @throws LoginException
     */
    private byte[] getToken(AuthorizationPolicy authPolicy, 
                            String spn, 
                            Oid oid,
                            Message message) throws GSSException, 
        LoginException {
        GSSManager manager = GSSManager.getInstance();
        GSSName serverName = manager.createName(spn, null);

        GSSCredential delegatedCred = (GSSCredential)message.get(GSSCredential.class.getName());
        
        GSSContext context = manager
                .createContext(serverName.canonicalize(oid), oid, delegatedCred, GSSContext.DEFAULT_LIFETIME);
        
        context.requestCredDeleg(isCredDelegationRequired(message));

        // If the delegated cred is not null then we only need the context to
        // immediately return a ticket based on this credential without attempting
        // to log on again 
        return getToken(delegatedCred == null ? authPolicy : null, 
                        context);
    }
    
    protected boolean isCredDelegationRequired(Message message) { 
        Object prop = message.getContextualProperty(PROPERTY_REQUIRE_CRED_DELEGATION);
        return prop == null ? credDelegation : MessageUtils.isTrue(prop);
    }

    protected String getCompleteServicePrincipalName(URL currentURL) {
        String name = servicePrincipalName == null 
            ? "HTTP/" + currentURL.getHost() : servicePrincipalName;
        if (realm != null) {            
            name += "@" + realm;
        }
        return name;
            
            
    }
    
    public void setServicePrincipalName(String servicePrincipalName) {
        this.servicePrincipalName = servicePrincipalName;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
    
    private final class CreateServiceTicketAction implements PrivilegedExceptionAction<byte[]> {
        private final GSSContext context;
        private final byte[] token;

        private CreateServiceTicketAction(GSSContext context, byte[] token) {
            this.context = context;
            this.token = token;
        }

        public byte[] run() throws GSSException {
            return context.initSecContext(token, 0, token.length);
        }
    }
    
    public static CallbackHandler getUsernamePasswordHandler(final String username, final String password) {
        final CallbackHandler handler = new CallbackHandler() {

            public void handle(final Callback[] callback) {
                for (int i = 0; i < callback.length; i++) {
                    if (callback[i] instanceof NameCallback) {
                        final NameCallback nameCallback = (NameCallback) callback[i];
                        nameCallback.setName(username);
                    } else if (callback[i] instanceof PasswordCallback) {
                        final PasswordCallback passCallback = (PasswordCallback) callback[i];
                        passCallback.setPassword(password.toCharArray());
                    }
                }
            }
        };
        return handler;
    }

    public void setCredDelegation(boolean delegation) {
        this.credDelegation = delegation;
    }

}

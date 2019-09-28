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

import java.net.InetAddress;
import java.net.URI;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
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
    private Configuration loginConfig;
    private Oid serviceNameType;
    private boolean useCanonicalHostname;

    public String getAuthorization(AuthorizationPolicy authPolicy,
                                   URI currentURI,
                                   Message message) {
        if (!HttpAuthHeader.AUTH_TYPE_NEGOTIATE.equals(authPolicy.getAuthorizationType())) {
            return null;
        }
        try {
            String spn = getCompleteServicePrincipalName(currentURI);

            boolean useKerberosOid = MessageUtils.getContextualBoolean(message, PROPERTY_USE_KERBEROS_OID);
            Oid oid = new Oid(useKerberosOid ? KERBEROS_OID : SPNEGO_OID);

            byte[] token = getToken(authPolicy, spn, oid, message);
            return HttpAuthHeader.AUTH_TYPE_NEGOTIATE + " " + Base64Utility.encode(token);
        } catch (LoginException | GSSException e) {
            throw new RuntimeException(e.getMessage(), e);
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

        GSSCredential delegatedCred =
            (GSSCredential)message.getContextualProperty(GSSCredential.class.getName());

        Subject subject = null;
        if (authPolicy != null && delegatedCred == null) {
            String contextName = authPolicy.getAuthorization();
            if (contextName == null) {
                contextName = "";
            }

            if (!(StringUtils.isEmpty(authPolicy.getUserName())
                && StringUtils.isEmpty(contextName) && loginConfig == null)) {
                CallbackHandler callbackHandler = getUsernamePasswordHandler(
                    authPolicy.getUserName(), authPolicy.getPassword());
                LoginContext lc = new LoginContext(contextName, null, callbackHandler, loginConfig);
                lc.login();
                subject = lc.getSubject();
            }
        }

        GSSManager manager = GSSManager.getInstance();
        GSSName serverName = manager.createName(spn, serviceNameType);

        GSSContext context = manager
                .createContext(serverName.canonicalize(oid), oid, delegatedCred, GSSContext.DEFAULT_LIFETIME);

        context.requestCredDeleg(isCredDelegationRequired(message));

        // If the delegated cred is not null then we only need the context to
        // immediately return a ticket based on this credential without attempting
        // to log on again
        final byte[] token = new byte[0];
        if (delegatedCred != null) {
            return context.initSecContext(token, 0, token.length);
        }

        decorateSubject(subject);

        try {
            return Subject.doAs(subject, new CreateServiceTicketAction(context, token));
        } catch (PrivilegedActionException e) {
            if (e.getCause() instanceof GSSException) {
                throw (GSSException) e.getCause();
            }
            LOG.log(Level.SEVERE, "initSecContext", e);
            return null;
        }
    }

    // Allow subclasses to decorate the Subject if required.
    protected void decorateSubject(Subject subject) {

    }

    protected boolean isCredDelegationRequired(Message message) {
        return MessageUtils.getContextualBoolean(message, PROPERTY_REQUIRE_CRED_DELEGATION, credDelegation);
    }

    protected String getCompleteServicePrincipalName(URI currentURI) {
        String name;

        if (servicePrincipalName == null) {
            String host = currentURI.getHost();
            if (useCanonicalHostname) {
                host = getCanonicalHostname(host);
            }
            name = "HTTP/" + host;
        } else {
            name = servicePrincipalName;
        }
        if (realm != null) {
            name += "@" + realm;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service Principal Name is " + name);
        }
        return name;
    }

    private String getCanonicalHostname(String hostname) {
        String canonicalHostname = hostname;
        try {
            InetAddress in = InetAddress.getByName(hostname);
            canonicalHostname = in.getCanonicalHostName();
            LOG.fine("resolved hostname=" + hostname + " to canonicalHostname=" + canonicalHostname);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "unable to resolve canonical hostname", e);
        }
        return canonicalHostname;
    }

    public void setServicePrincipalName(String servicePrincipalName) {
        this.servicePrincipalName = servicePrincipalName;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    private static final class CreateServiceTicketAction implements PrivilegedExceptionAction<byte[]> {
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

    public CallbackHandler getUsernamePasswordHandler(final String username, final String password) {
        if (StringUtils.isEmpty(username)) {
            return null;
        }
        return new NamePasswordCallbackHandler(username, password);
    }

    public void setCredDelegation(boolean delegation) {
        this.credDelegation = delegation;
    }

    public void setLoginConfig(Configuration config) {
        this.loginConfig = config;
    }

    public Oid getServiceNameType() {
        return serviceNameType;
    }

    public void setServiceNameType(Oid serviceNameType) {
        this.serviceNameType = serviceNameType;
    }

    public boolean isUseCanonicalHostname() {
        return useCanonicalHostname;
    }

    public void setUseCanonicalHostname(boolean useCanonicalHostname) {
        this.useCanonicalHostname = useCanonicalHostname;
    }

}

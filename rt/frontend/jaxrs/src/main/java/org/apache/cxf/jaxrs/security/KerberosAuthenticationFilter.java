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
package org.apache.cxf.jaxrs.security;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.common.security.SimpleSecurityContext;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
@PreMatching
public class KerberosAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOG = LogUtils.getL7dLogger(KerberosAuthenticationFilter.class);

    private static final String NEGOTIATE_SCHEME = "Negotiate";
    private static final String PROPERTY_USE_KERBEROS_OID = "auth.spnego.useKerberosOid";
    private static final String KERBEROS_OID = "1.2.840.113554.1.2.2";
    private static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    private MessageContext messageContext;
    private CallbackHandler callbackHandler;
    private Configuration loginConfig;
    private String loginContextName = "";
    private String servicePrincipalName;
    private String realm;

    @Override
    public void filter(ContainerRequestContext context) {

        List<String> authHeaders = messageContext.getHttpHeaders()
            .getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.size() != 1) {
            LOG.fine("No Authorization header is available");
            throw ExceptionUtils.toNotAuthorizedException(null, getFaultResponse());
        }
        String[] authPair = authHeaders.get(0).split(" ");
        if (authPair.length != 2 || !NEGOTIATE_SCHEME.equalsIgnoreCase(authPair[0])) {
            LOG.fine("Negotiate Authorization scheme is expected");
            throw ExceptionUtils.toNotAuthorizedException(null, getFaultResponse());
        }

        byte[] serviceTicket = getServiceTicket(authPair[1]);

        try {
            Subject serviceSubject = loginAndGetSubject();

            GSSContext gssContext = createGSSContext();

            Subject.doAs(serviceSubject, new ValidateServiceTicketAction(gssContext, serviceTicket));

            GSSName srcName = gssContext.getSrcName();
            if (srcName == null) {
                throw ExceptionUtils.toNotAuthorizedException(null, getFaultResponse());
            }

            String complexUserName = srcName.toString();

            String simpleUserName = complexUserName;
            int index = simpleUserName.lastIndexOf('@');
            if (index > 0) {
                simpleUserName = simpleUserName.substring(0, index);
            }
            Message m = JAXRSUtils.getCurrentMessage();
            m.put(SecurityContext.class, createSecurityContext(simpleUserName, complexUserName, gssContext));

            if (!gssContext.getCredDelegState()) {
                gssContext.dispose();
            }

        } catch (LoginException e) {
            LOG.fine("Unsuccessful JAAS login for the service principal: " + e.getMessage());
            throw ExceptionUtils.toNotAuthorizedException(e, getFaultResponse());
        } catch (GSSException e) {
            LOG.fine("GSS API exception: " + e.getMessage());
            throw ExceptionUtils.toNotAuthorizedException(e, getFaultResponse());
        } catch (PrivilegedActionException e) {
            LOG.fine("PrivilegedActionException: " + e.getMessage());
            throw ExceptionUtils.toNotAuthorizedException(e, getFaultResponse());
        }
    }

    protected SecurityContext createSecurityContext(String simpleUserName, String complexUserName,
                                                    GSSContext gssContext) {
        return new KerberosSecurityContext(new KerberosPrincipal(simpleUserName, complexUserName), gssContext);
    }

    protected GSSContext createGSSContext() throws GSSException {
        boolean useKerberosOid = PropertyUtils.isTrue(
            messageContext.getContextualProperty(PROPERTY_USE_KERBEROS_OID));
        Oid oid = new Oid(useKerberosOid ? KERBEROS_OID : SPNEGO_OID);

        GSSManager gssManager = GSSManager.getInstance();

        String spn = getCompleteServicePrincipalName();
        GSSName gssService = gssManager.createName(spn, null);

        return gssManager.createContext(gssService.canonicalize(oid),
                   oid, null, GSSContext.DEFAULT_LIFETIME);
    }

    protected Subject loginAndGetSubject() throws LoginException {

        // The login without a callback can work if
        // - Kerberos keytabs are used with a principal name set in the JAAS config
        // - Kerberos is integrated into the OS logon process
        //   meaning that a process which runs this code has the
        //   user identity

        final LoginContext lc;
        if (!StringUtils.isEmpty(loginContextName) || loginConfig != null) {
            lc = new LoginContext(loginContextName, null, callbackHandler, loginConfig);
        } else {
            LOG.fine("LoginContext can not be initialized");
            throw new LoginException();
        }
        lc.login();
        return lc.getSubject();
    }

    private byte[] getServiceTicket(String encodedServiceTicket) {
        try {
            return Base64Utility.decode(encodedServiceTicket);
        } catch (Base64Exception ex) {
            throw ExceptionUtils.toNotAuthorizedException(null, getFaultResponse());
        }
    }

    private static Response getFaultResponse() {
        return JAXRSUtils.toResponseBuilder(401).header(HttpHeaders.WWW_AUTHENTICATE, NEGOTIATE_SCHEME).build();
    }

    protected String getCompleteServicePrincipalName() {
        String name = servicePrincipalName == null
            ? "HTTP/" + messageContext.getUriInfo().getBaseUri().getHost() : servicePrincipalName;
        if (realm != null) {
            name += "@" + realm;
        }
        return name;


    }

    @Context
    public void setMessageContext(MessageContext context) {
        this.messageContext = context;
    }

    public void setLoginContextName(String contextName) {
        this.loginContextName = contextName;
    }

    public void setServicePrincipalName(String servicePrincipalName) {
        this.servicePrincipalName = servicePrincipalName;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    private static final class ValidateServiceTicketAction implements PrivilegedExceptionAction<byte[]> {
        private final GSSContext context;
        private final byte[] token;

        private ValidateServiceTicketAction(GSSContext context, byte[] token) {
            this.context = context;
            this.token = token;
        }

        public byte[] run() throws GSSException {
            return context.acceptSecContext(token, 0, token.length);
        }
    }

    public static class KerberosPrincipal extends SimplePrincipal {
        private static final long serialVersionUID = 1L;
        private String complexName;
        public KerberosPrincipal(String simpleName, String complexName) {
            super(simpleName);
            this.complexName = complexName;
        }

        public String getKerberosName() {
            return complexName;
        }
    }

    public static class KerberosSecurityContext extends SimpleSecurityContext {
        private GSSContext context;
        public KerberosSecurityContext(KerberosPrincipal principal,
                                       GSSContext context) {
            super(principal);
            this.context = context;
        }

        public GSSContext getGSSContext() {
            return context;
        }
    }



}

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

import java.security.PrivilegedAction;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;

public class TLSAuthenticationInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String AUTHORIZATION_TYPE_TLS = "TLSAuthenticatedHandshake";

    private static final Logger LOG = LogUtils.getL7dLogger(TLSAuthenticationInterceptor.class);

    private String userNameKey;
    private boolean useDoAs;
    private boolean reportFault;
    private TLSSecuritySubjectProvider subjectProvider = new DefaultTLSSecuritySubjectProvider();

    public TLSAuthenticationInterceptor() {
        super(Phase.UNMARSHAL);
        addBefore(JAASLoginInterceptor.class.getName());
    }

    public TLSAuthenticationInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(final Message message) throws Fault {
        Subject subject = null;
        String userName = null;

        try {
            X509Certificate certificate = getCertificate(message);
            userName = getUserName(certificate);

            subject = subjectProvider.getSubject(userName, certificate);
            SecurityContext context = new DefaultSecurityContext(userName, subject);

            message.put(SecurityContext.class, context);

            if (useDoAs) {
                Subject.doAs(subject, new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        InterceptorChain chain = message.getInterceptorChain();
                        if (chain != null) {
                            chain.doIntercept(message);
                        }
                        return null;
                    }
                });
            }

        } catch (SecurityException ex) {
            String errorMessage = "TLS Certificate processing failed for '" + userName + "' : " + ex.getMessage();
            LOG.fine(errorMessage);
            if (isReportFault()) {
                throw new AuthenticationException(errorMessage);
            } else {
                throw new AuthenticationException("TLS Certificate processing failed "
                        + "(details can be found in server log)");
            }
        }
    }

    /**
     * Extracts certificate from message, expecting to find TLSSessionInfo
     * inside.
     * 
     * @param message
     * @return
     */
    protected X509Certificate getCertificate(Message message) {
        TLSSessionInfo tlsSessionInfo = message.get(TLSSessionInfo.class);
        if (tlsSessionInfo == null) {
            throw new SecurityException("Not TLS connection");
        }

        Certificate[] certificates = tlsSessionInfo.getPeerCertificates();
        if (certificates == null || certificates.length == 0) {
            throw new SecurityException("No certificate found");
        }

        // Due to RFC5246, senders certificates always comes 1st
        return (X509Certificate) certificates[0];
    }

    /**
     * Returns Subject DN from X509Certificate
     * 
     * @param certificate
     * @return Subject DN as a user name
     */
    protected String getUserName(X509Certificate certificate) {
        String dn = certificate.getSubjectDN().getName();

        if (getUserNameKey() == null) {
            return dn;
        }

        LdapName ldapDn;
        try {
            ldapDn = new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new SecurityException("Invalid DN");
        }

        for (Rdn rdn : ldapDn.getRdns()) {
            if (getUserNameKey().equalsIgnoreCase(rdn.getType())) {
                return (String) rdn.getValue();
            }
        }

        throw new SecurityException("No " + getUserNameKey() + " key found in certificate DN: " + dn);
    }

    public boolean getUseDoAs() {
        return useDoAs;
    }

    public void setUseDoAs(boolean useDoAs) {
        this.useDoAs = useDoAs;
    }

    public TLSSecuritySubjectProvider getSubjectProvider() {
        return subjectProvider;
    }

    public void setSubjectProvider(TLSSecuritySubjectProvider subjectProvider) {
        this.subjectProvider = subjectProvider;
    }

    public String getUserNameKey() {
        return userNameKey;
    }

    public void setUserNameKey(String userNameKey) {
        this.userNameKey = userNameKey;
    }

    public boolean isReportFault() {
        return reportFault;
    }

    public void setReportFault(boolean reportFault) {
        this.reportFault = reportFault;
    }

    private class DefaultTLSSecuritySubjectProvider extends TLSSecuritySubjectProvider {

        @Override
        public Subject getSubject(String userName, X509Certificate certificate) throws SecurityException {
            return createSubject(userName, null);
        }

    }
}

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
package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;

import jakarta.xml.soap.SOAPException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.security.SecurityContext;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;


/**
 * Base class providing an extensibility point for populating
 * javax.security.auth.Subject from a current UsernameToken.
 *
 * WSS4J requires a password for validating digests which may not be available
 * when external security systems provide for the authentication. This class
 * implements WSS4J Processor interface so that it can delegate a UsernameToken
 * validation to an external system.
 *
 * In order to handle digests, this class currently creates a new WSS4J Security Engine for
 * every request. If clear text passwords are expected then a supportDigestPasswords boolean
 * property with a false value can be used to disable creating security engines.
 *
 * Note that if a UsernameToken containing a clear text password has been encrypted then
 * an application is expected to provide a password callback handler for decrypting the token only.
 *
 */
public abstract class AbstractUsernameTokenAuthenticatingInterceptor extends WSS4JInInterceptor {

    private static final Logger LOG =
        LogUtils.getL7dLogger(AbstractUsernameTokenAuthenticatingInterceptor.class);

    private boolean supportDigestPasswords;

    public AbstractUsernameTokenAuthenticatingInterceptor() {
        this(new HashMap<String, Object>());
    }

    public AbstractUsernameTokenAuthenticatingInterceptor(Map<String, Object> properties) {
        super(properties);
        getAfter().add(PolicyBasedWSS4JInInterceptor.class.getName());
    }

    public void setSupportDigestPasswords(boolean support) {
        supportDigestPasswords = support;
    }

    public boolean getSupportDigestPasswords() {
        return supportDigestPasswords;
    }

    @Override
    public void handleMessage(SoapMessage msg) throws Fault {
        SecurityToken token = msg.get(SecurityToken.class);
        SecurityContext context = msg.get(SecurityContext.class);
        if (token == null || context == null || context.getUserPrincipal() == null) {
            super.handleMessage(msg);
            return;
        }
        UsernameToken ut = (UsernameToken)token;

        Subject subject = createSubject(ut.getName(), ut.getPassword(), ut.isHashed(),
                                        ut.getNonce(), ut.getCreatedTime());

        SecurityContext sc = doCreateSecurityContext(context.getUserPrincipal(), subject);
        msg.put(SecurityContext.class, sc);
    }

    @Override
    protected void doResults(
                             SoapMessage msg,
                             String actor,
                             Element soapHeader,
                             Element soapBody,
                             WSHandlerResult wsResult,
                             boolean utWithCallbacks
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        /*
         * All ok up to this point. Now construct and setup the security result
         * structure. The service may fetch this and check it.
         */
        List<WSHandlerResult> results = CastUtils.cast((List<?>)msg.get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            results = new LinkedList<>();
            msg.put(WSHandlerConstants.RECV_RESULTS, results);
        }
        results.add(0, wsResult);

        new UsernameTokenSecurityContextCreator().createSecurityContext(msg, wsResult);
    }

    /**
     * Creates default SecurityContext which implements isUserInRole using the
     * following approach : skip the first Subject principal, and then check optional
     * Groups the principal is a member of. Subclasses can override this method and implement
     * a custom strategy instead
     *
     * @param p principal
     * @param subject subject
     * @return security context
     */
    protected SecurityContext doCreateSecurityContext(final Principal p, final Subject subject) {
        return new DefaultSecurityContext(p, subject);
    }


    protected void setSubject(String name,
                              String password,
                              boolean isDigest,
                              String nonce,
                              String created) throws WSSecurityException {
        Message msg = PhaseInterceptorChain.getCurrentMessage();
        if (msg == null) {
            throw new IllegalStateException("Current message is not available");
        }
        final Subject subject;
        try {
            subject = createSubject(name, password, isDigest, nonce, created);
        } catch (Exception ex) {
            String errorMessage = "Failed Authentication : Subject has not been created";
            LOG.severe(errorMessage);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION,
                                          ex);
        }
        if (subject == null || subject.getPrincipals().isEmpty()
            || !subject.getPrincipals().iterator().next().getName().equals(name)) {
            String errorMessage = "Failed Authentication : Invalid Subject";
            LOG.severe(errorMessage);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION,
                                          new Exception(errorMessage));
        }
        msg.put(Subject.class, subject);
    }

    /**
     * Create a Subject representing a current user and its roles.
     * This Subject is expected to contain at least one Principal representing a user
     * and optionally followed by one or more principal Groups this user is a member of.
     * It will also be available in doCreateSecurityContext.
     * @param name username
     * @param password password
     * @param isDigest true if a password digest is used
     * @param nonce optional nonce
     * @param created optional timestamp
     * @return subject
     * @throws SecurityException
     */
    protected abstract Subject createSubject(String name,
                                    String password,
                                    boolean isDigest,
                                    String nonce,
                                    String created) throws SecurityException;

    @Override
    protected WSSecurityEngine getSecurityEngine(boolean utNoCallbacks) {
        WSSConfig config = WSSConfig.getNewInstance();
        config.setValidator(WSConstants.USERNAME_TOKEN, new CustomValidator());
        WSSecurityEngine ret = new WSSecurityEngine();
        ret.setWssConfig(config);
        return ret;
    }

    protected class CustomValidator extends UsernameTokenValidator {

        @Override
        protected void verifyCustomPassword(
            org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
            RequestData data
        ) throws WSSecurityException {
            AbstractUsernameTokenAuthenticatingInterceptor.this.setSubject(
                usernameToken.getName(), usernameToken.getPassword(), false, null, null
            );
        }

        @Override
        protected void verifyPlaintextPassword(
            org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
            RequestData data
        ) throws WSSecurityException {
            AbstractUsernameTokenAuthenticatingInterceptor.this.setSubject(
                usernameToken.getName(), usernameToken.getPassword(), false, null, null
            );
        }

        @Override
        protected void verifyDigestPassword(
            org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
            RequestData data
        ) throws WSSecurityException {
            if (!supportDigestPasswords) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
            String user = usernameToken.getName();
            String password = usernameToken.getPassword();
            boolean isHashed = usernameToken.isHashed();
            String nonce = usernameToken.getNonce();
            String createdTime = usernameToken.getCreated();
            AbstractUsernameTokenAuthenticatingInterceptor.this.setSubject(
                user, password, isHashed, nonce, createdTime
            );
        }

        @Override
        protected void verifyUnknownPassword(
            org.apache.wss4j.dom.message.token.UsernameToken usernameToken,
            RequestData data
        ) throws WSSecurityException {
            AbstractUsernameTokenAuthenticatingInterceptor.this.setSubject(
                usernameToken.getName(), null, false, null, null
            );
        }

    }

    private static final class UsernameTokenSecurityContextCreator extends DefaultWSS4JSecurityContextCreator {

        @Override
        protected SecurityContext createSecurityContext(final Principal p) {
            Message msg = PhaseInterceptorChain.getCurrentMessage();
            if (msg == null) {
                throw new IllegalStateException("Current message is not available");
            }
            return new DefaultSecurityContext(p, msg.get(Subject.class));
        }
    }
}

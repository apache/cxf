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

package org.apache.cxf.ws.security.trust;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.claims.RoleClaimsCallbackHandler;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;

/**
 * A JAAS LoginModule for authenticating a Username/Password to the STS. It can be configured
 * either by specifying the various options (documented below) in the JAAS configuration, or
 * else by picking up a CXF STSClient from the CXF bus (either the default one, or else one
 * that has the same QName as the service name).
 */
public class STSLoginModule implements LoginModule {
    /**
     * Whether we require roles or not from the STS. If this is not set then the
     * WS-Trust validate binding is used. If it is set then the issue binding is
     * used, where the Username + Password credentials are passed via "OnBehalfOf"
     * (unless the DISABLE_ON_BEHALF_OF property is set to "true", see below). In addition,
     * claims are added to the request for the standard "role" ClaimType.
     */
    public static final String REQUIRE_ROLES = "require.roles";

    /**
     * Whether to disable passing Username + Password credentials via "OnBehalfOf". If the
     * REQUIRE_ROLES property (see above) is set to "true", then the Issue Binding is used
     * and the credentials are passed via OnBehalfOf. If this (DISABLE_ON_BEHALF_OF) property
     * is set to "true", then the credentials instead are passed through to the
     * WS-SecurityPolicy layer and used depending on the security policy of the STS endpoint.
     * For example, if the STS endpoint requires a WS-Security UsernameToken, then the
     * credentials are inserted here.
     */
    public static final String DISABLE_ON_BEHALF_OF = "disable.on.behalf.of";

    /**
     * Whether to disable caching of validated credentials or not. The default is "false", meaning that
     * caching is enabled. However, caching only applies when token transformation takes place, i.e. when
     * the "require.roles" property is set to "true".
     */
    public static final String DISABLE_CACHING = "disable.caching";

    /**
     * The WSDL Location of the STS
     */
    public static final String WSDL_LOCATION = "wsdl.location";

    /**
     * The Service QName of the STS
     */
    public static final String SERVICE_NAME = "service.name";

    /**
     * The Endpoint QName of the STS
     */
    public static final String ENDPOINT_NAME = "endpoint.name";

    /**
     * The default key size to use if using the SymmetricKey KeyType. Defaults to 256.
     */
    public static final String KEY_SIZE = "key.size";

    /**
     * The key type to use. The default is the standard "Bearer" URI.
     */
    public static final String KEY_TYPE = "key.type";

    /**
     * The token type to use. The default is the standard SAML 2.0 URI.
     */
    public static final String TOKEN_TYPE = "token.type";

    /**
     * The WS-Trust namespace to use. The default is the WS-Trust 1.3 namespace.
     */
    public static final String WS_TRUST_NAMESPACE = "ws.trust.namespace";

    /**
     * The location of a Spring configuration file that can be used to configure the
     * STS client (for example, to configure the TrustStore if TLS is used). This is
     * designed to be used if the service that is being secured is not CXF-based.
     */
    public static final String CXF_SPRING_CFG = "cxf.spring.config";

    private static final Logger LOG = LogUtils.getL7dLogger(STSLoginModule.class);

    private Set<Principal> roles = new HashSet<>();
    private Principal userPrincipal;
    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean requireRoles;
    private boolean disableOnBehalfOf;
    private boolean disableCaching;
    private String wsdlLocation;
    private String serviceName;
    private String endpointName;
    private String cxfSpringCfg;
    private int keySize;
    private String keyType = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private String tokenType = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private String namespace;
    private Map<String, Object> stsClientProperties = new HashMap<>();

    /** the authentication status*/
    private boolean succeeded;
    private boolean commitSucceeded;

    @Override
    public void initialize(Subject subj, CallbackHandler cbHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {
        subject = subj;
        callbackHandler = cbHandler;
        if (options.containsKey(REQUIRE_ROLES)) {
            requireRoles = Boolean.parseBoolean((String)options.get(REQUIRE_ROLES));
        }
        if (options.containsKey(DISABLE_ON_BEHALF_OF)) {
            disableOnBehalfOf = Boolean.parseBoolean((String)options.get(DISABLE_ON_BEHALF_OF));
        }
        if (options.containsKey(DISABLE_CACHING)) {
            disableCaching = Boolean.parseBoolean((String)options.get(DISABLE_CACHING));
        }
        if (options.containsKey(WSDL_LOCATION)) {
            wsdlLocation = (String)options.get(WSDL_LOCATION);
        }
        if (options.containsKey(SERVICE_NAME)) {
            serviceName = (String)options.get(SERVICE_NAME);
        }
        if (options.containsKey(ENDPOINT_NAME)) {
            endpointName = (String)options.get(ENDPOINT_NAME);
        }
        if (options.containsKey(KEY_SIZE)) {
            keySize = Integer.parseInt((String)options.get(KEY_SIZE));
        }
        if (options.containsKey(KEY_TYPE)) {
            keyType = (String)options.get(KEY_TYPE);
        }
        if (options.containsKey(TOKEN_TYPE)) {
            tokenType = (String)options.get(TOKEN_TYPE);
        }
        if (options.containsKey(WS_TRUST_NAMESPACE)) {
            namespace = (String)options.get(WS_TRUST_NAMESPACE);
        }
        if (options.containsKey(CXF_SPRING_CFG)) {
            cxfSpringCfg = (String)options.get(CXF_SPRING_CFG);
        }

        stsClientProperties.clear();
        for (String s : SecurityConstants.ALL_PROPERTIES) {
            if (options.containsKey(s)) {
                stsClientProperties.put(s, options.get(s));
            }
        }
    }

    @Override
    public boolean login() throws LoginException {
        // Get username and password
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioException) {
            throw new LoginException(ioException.getMessage());
        } catch (UnsupportedCallbackException unsupportedCallbackException) {
            throw new LoginException(unsupportedCallbackException.getMessage()
                                     + " not available to obtain information from user.");
        }

        String user = ((NameCallback) callbacks[0]).getName();

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        String password = new String(tmpPassword);

        roles = new HashSet<>();
        userPrincipal = null;

        STSTokenValidator validator = new STSTokenValidator(true);
        validator.setUseIssueBinding(requireRoles);
        validator.setUseOnBehalfOf(!disableOnBehalfOf);
        validator.setDisableCaching(!requireRoles || disableCaching);

        // Authenticate token
        try {
            UsernameToken token = convertToToken(user, password);
            Credential credential = new Credential();
            credential.setUsernametoken(token);

            RequestData data = new RequestData();
            Message message = PhaseInterceptorChain.getCurrentMessage();

            STSClient stsClient = configureSTSClient(message);
            if (message != null) {
                message.put(SecurityConstants.STS_CLIENT, stsClient);
                data.setMsgContext(message);
            } else {
                validator.setStsClient(stsClient);
            }

            credential = validator.validate(credential, data);

            // Add user principal
            userPrincipal = new SimplePrincipal(user);

            // Add roles if a SAML Assertion was returned from the STS
            roles.addAll(getRoles(message, credential));
        } catch (Exception e) {
            LOG.log(Level.INFO, "User " + user + " authentication failed", e);
            throw new LoginException("User " + user + " authentication failed: " + e.getMessage());
        }

        succeeded = true;
        return true;
    }

    private STSClient configureSTSClient(Message msg) throws BusException, EndpointException {
        final STSClient c;
        if (cxfSpringCfg != null) {
            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = Loader.getResource(cxfSpringCfg);

            Bus bus = bf.createBus(busFile.toString());
            BusFactory.setDefaultBus(bus);
            BusFactory.setThreadDefaultBus(bus);
            c = new STSClient(bus);
        } else if (msg == null) {
            Bus bus = BusFactory.getDefaultBus(true);
            c = new STSClient(bus);
        } else {
            c = STSUtils.getClient(msg, "sts");
        }

        if (wsdlLocation != null) {
            c.setWsdlLocation(wsdlLocation);
        }
        if (serviceName != null) {
            c.setServiceName(serviceName);
        }
        if (endpointName != null) {
            c.setEndpointName(endpointName);
        }
        if (keySize > 0) {
            c.setKeySize(keySize);
        }
        if (keyType != null) {
            c.setKeyType(keyType);
        }
        if (tokenType != null) {
            c.setTokenType(tokenType);
        }
        if (namespace != null) {
            c.setNamespace(namespace);
        }

        c.setProperties(stsClientProperties);

        if (requireRoles && c.getClaimsCallbackHandler() == null) {
            c.setClaimsCallbackHandler(new RoleClaimsCallbackHandler());
        }

        return c;
    }

    private UsernameToken convertToToken(String username, String password)
        throws Exception {

        Document doc = DOMUtils.getEmptyDocument();
        UsernameToken token = new UsernameToken(false, doc,
                                                WSS4JConstants.PASSWORD_TEXT);
        token.setName(username);
        token.setPassword(password);
        return token;
    }

    private Set<Principal> getRoles(Message msg, Credential credential) {
        SamlAssertionWrapper samlAssertion = credential.getTransformedToken();
        if (samlAssertion == null) {
            samlAssertion = credential.getSamlAssertion();
        }
        if (samlAssertion != null) {
            String roleAttributeName = null;
            if (msg != null) {
                roleAttributeName =
                    (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_ROLE_ATTRIBUTENAME,
                                                                   msg);
            }
            if (roleAttributeName == null || roleAttributeName.length() == 0) {
                roleAttributeName = WSS4JInInterceptor.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
            }

            ClaimCollection claims =
                SAMLUtils.getClaims(samlAssertion);
            return SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);
        }

        return Collections.emptySet();
    }


    @Override
    public boolean commit() throws LoginException {
        if (!succeeded || userPrincipal == null) {
            roles.clear();
            succeeded = false;
            userPrincipal = null;
            return false;
        } 
        subject.getPrincipals().add(userPrincipal);
        subject.getPrincipals().addAll(roles);
        commitSucceeded = true;
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        if (!succeeded) {
            return false;
        } else if (commitSucceeded) {
            // we succeeded, but another required module failed
            logout();
        } else {
            // our commit failed
            roles.clear();
            userPrincipal = null;
            succeeded = false;
        }
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        subject.getPrincipals().removeAll(roles);
        roles.clear();
        userPrincipal = null;

        succeeded = false;
        commitSucceeded = false;

        return true;
    }


}

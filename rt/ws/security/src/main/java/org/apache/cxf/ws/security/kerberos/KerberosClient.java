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

package org.apache.cxf.ws.security.kerberos;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.xml.security.utils.XMLUtils;
import org.ietf.jgss.GSSCredential;

/**
 * A class that obtains a ticket from a KDC and wraps it in a SecurityToken object.
 */
public class KerberosClient implements Configurable {
    private static final Logger LOG = LogUtils.getL7dLogger(KerberosClient.class);

    String name = "default.kerberos-client";

    private String serviceName;
    private CallbackHandler callbackHandler;
    private String contextName;
    private WSSConfig wssConfig = WSSConfig.getNewInstance();
    private boolean requestCredentialDelegation;
    private boolean isUsernameServiceNameForm;
    private boolean useDelegatedCredential;

    public KerberosClient() {
    }

    public String getBeanName() {
        return name;
    }

    /**
     * Get the JAAS Login context name to use.
     * @return the JAAS Login context name to use
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Set the JAAS Login context name to use.
     * @param contextName the JAAS Login context name to use
     */
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    /**
     * Get the CallbackHandler to use with the LoginContext
     * @return the CallbackHandler to use with the LoginContext
     */
    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    /**
     * Set the CallbackHandler to use with the LoginContext. It can be null.
     * @param callbackHandler the CallbackHandler to use with the LoginContext
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    /**
     * The name of the service to use when contacting the KDC.
     * @param serviceName the name of the service to use when contacting the KDC
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Get the name of the service to use when contacting the KDC.
     * @return the name of the service to use when contacting the KDC
     */
    public String getServiceName() {
        return serviceName;
    }

    public SecurityToken requestSecurityToken() throws Exception {
        // See if we have a delegated Credential to use
        Message message = PhaseInterceptorChain.getCurrentMessage();
        GSSCredential delegatedCredential = null;
        if (message != null && useDelegatedCredential) {
            Object obj = message.getContextualProperty(SecurityConstants.DELEGATED_CREDENTIAL);
            if (obj instanceof GSSCredential) {
                delegatedCredential = (GSSCredential)obj;
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Requesting Kerberos ticket for " + serviceName
                    + " using JAAS Login Module: " + getContextName());
        }
        KerberosSecurity bst = createKerberosSecurity();
        bst.retrieveServiceTicket(getContextName(), callbackHandler, serviceName,
                                  isUsernameServiceNameForm, requestCredentialDelegation,
                                  delegatedCredential);
        bst.addWSUNamespace();
        bst.setID(wssConfig.getIdAllocator().createSecureId("BST-", bst));
        bst.addWSUNamespace();

        SecurityToken token = new SecurityToken(bst.getID());
        token.setToken(bst.getElement());
        token.setWsuId(bst.getID());
        SecretKey secretKey = bst.getSecretKey();
        if (secretKey != null) {
            token.setKey(secretKey);
            token.setSecret(secretKey.getEncoded());
        }
        String sha1 = XMLUtils.encodeToString(KeyUtils.generateDigest(bst.getToken()));
        token.setSHA1(sha1);
        token.setTokenType(bst.getValueType());

        return token;
    }

    protected KerberosSecurity createKerberosSecurity() {
        return new KerberosSecurity(DOMUtils.getEmptyDocument());
    }

    public boolean isUsernameServiceNameForm() {
        return isUsernameServiceNameForm;
    }

    public void setUsernameServiceNameForm(boolean usernameServiceNameForm) {
        this.isUsernameServiceNameForm = usernameServiceNameForm;
    }

    public boolean isRequestCredentialDelegation() {
        return requestCredentialDelegation;
    }

    public void setRequestCredentialDelegation(boolean requestCredentialDelegation) {
        this.requestCredentialDelegation = requestCredentialDelegation;
    }

    public boolean isUseDelegatedCredential() {
        return useDelegatedCredential;
    }

    public void setUseDelegatedCredential(boolean useDelegatedCredential) {
        this.useDelegatedCredential = useDelegatedCredential;
    }

}

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

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.message.token.KerberosSecurity;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * A class that obtains a ticket from a KDC and wraps it in a SecurityToken object.
 */
public class KerberosClient implements Configurable {
    private static final Logger LOG = LogUtils.getL7dLogger(KerberosClient.class);
    
    Bus bus;
    String name = "default.kerberos-client";
    
    private String serviceName;
    private CallbackHandler callbackHandler;
    private String contextName;
    private WSSConfig wssConfig = WSSConfig.getNewInstance();
    
    public KerberosClient(Bus b) {
        bus = b;
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
     * @deprecated
     * Get the JAAS Login module name to use.
     * @return the JAAS Login module name to use
     */
    public String getJaasLoginModuleName() {
        return contextName;
    }

    /**
     * @deprecated
     * Set the JAAS Login module name to use.
     * @param jaasLoginModuleName the JAAS Login module name to use
     */
    public void setJaasLoginModuleName(String jaasLoginModuleName) {
        this.contextName = jaasLoginModuleName;
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
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Requesting Kerberos ticket for " + serviceName 
                    + " using JAAS Login Module: " + getContextName());
        }
        KerberosSecurity bst = new KerberosSecurity(DOMUtils.createDocument());
        bst.retrieveServiceTicket(getContextName(), callbackHandler, serviceName);
        bst.addWSUNamespace();
        bst.setID(wssConfig.getIdAllocator().createSecureId("BST-", bst));
        
        SecurityToken token = new SecurityToken(bst.getID());
        token.setToken(bst.getElement());
        token.setWsuId(bst.getID());
        SecretKey secretKey = bst.getSecretKey();
        if (secretKey != null) {
            token.setSecret(secretKey.getEncoded());
        }
        String sha1 = Base64.encode(WSSecurityUtil.generateDigest(bst.getToken()));
        token.setSHA1(sha1);
        token.setTokenType(bst.getValueType());

        return token;
    }

}

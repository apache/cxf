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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.rs.security.saml.sso.state.SPStateManager;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;

public class AbstractSSOSpHandler {
    private static final Logger LOG = 
            LogUtils.getL7dLogger(AbstractSSOSpHandler.class);
    
    private SPStateManager stateProvider;
    private long stateTimeToLive = SSOConstants.DEFAULT_STATE_TIME;
    private Crypto signatureCrypto;
    private String signaturePropertiesFile;
    private CallbackHandler callbackHandler;
    private String callbackHandlerClass;
    
    static {
        OpenSAMLUtil.initSamlEngine();
    }
    
    @PreDestroy
    public void close() throws IOException {
        if (stateProvider != null) {
            stateProvider.close();
            stateProvider = null;
        }
    }
    
    public void setSignatureCrypto(Crypto crypto) {
        signatureCrypto = crypto;
    }
    
    /**
     * Set the String corresponding to the signature Properties class
     * @param signaturePropertiesFile the String corresponding to the signature properties file
     */
    public void setSignaturePropertiesFile(String signaturePropertiesFile) {
        this.signaturePropertiesFile = signaturePropertiesFile;
        LOG.fine("Setting signature properties: " + signaturePropertiesFile);
    }
    
    /**
     * Set the CallbackHandler object. 
     * @param callbackHandler the CallbackHandler object. 
     */
    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
        LOG.fine("Setting callbackHandler: " + callbackHandler);
    }
    
    /**
     * Set the String corresponding to the CallbackHandler class. 
     * @param callbackHandlerClass the String corresponding to the CallbackHandler class. 
     */
    public void setCallbackHandlerClass(String callbackHandlerClass) {
        this.callbackHandlerClass = callbackHandlerClass;
        LOG.fine("Setting callbackHandlerClass: " + callbackHandlerClass);
    }
    
    //TODO: support attaching a signature to the cookie value
    protected String createCookie(String name, 
                                  String value, 
                                  String path,
                                  String domain) { 
        
        String contextCookie = name + "=" + value;
        // Setting a specific path restricts the browsers
        // to return a cookie only to the web applications
        // listening on that specific context path
        if (path != null) {
            contextCookie += ";Path=" + path;
        }
        
        // Setting a specific domain further restricts the browsers
        // to return a cookie only to the web applications
        // listening on the specific context path within a particular domain
        if (domain != null) {
            contextCookie += ";Domain=" + domain;
        }
        
        // Keep the cookie across the browser restarts until it actually expires.
        // Note that the Expires property has been deprecated but apparently is 
        // supported better than 'max-age' property by different browsers 
        // (Firefox, IE, etc)
        Date expiresDate = new Date(System.currentTimeMillis() + stateTimeToLive);
        String cookieExpires = HttpUtils.getHttpDateFormat().format(expiresDate);
        contextCookie += ";Expires=" + cookieExpires;
        //TODO: Consider adding an 'HttpOnly' attribute        
        
        return contextCookie;
    }
    
    protected boolean isStateExpired(long stateCreatedAt, long expiresAt) {
        Date currentTime = new Date();
        if (currentTime.after(new Date(stateCreatedAt + getStateTimeToLive()))) {
            return true;
        }
        
        if (expiresAt > 0 && currentTime.after(new Date(expiresAt))) {
            return true;
        }
        
        return false;
    }
    
    public void setStateProvider(SPStateManager stateProvider) {
        this.stateProvider = stateProvider;
    }

    public SPStateManager getStateProvider() {
        return stateProvider;
    }

    public void setStateTimeToLive(long stateTimeToLive) {
        this.stateTimeToLive = stateTimeToLive;
    }

    public long getStateTimeToLive() {
        return stateTimeToLive;
    }
    
    protected static Properties getProps(Object o) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            URL url = null;
            Bus bus = PhaseInterceptorChain.getCurrentMessage().getExchange().getBus();
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, AbstractSSOSpHandler.class);
                }
                if (url == null) {
                    url = new URL((String)o);
                }
                if (url != null) {
                    properties = new Properties();
                    InputStream ins = url.openStream();
                    properties.load(ins);
                    ins.close();
                }
            } catch (IOException e) {
                LOG.fine(e.getMessage());
                properties = null;
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                InputStream ins = ((URL)o).openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                LOG.fine(e.getMessage());
                properties = null;
            }            
        }
        return properties;
    }
    
    protected Crypto getSignatureCrypto() {
        if (signatureCrypto == null && signaturePropertiesFile != null) {
            Properties sigProperties = getProps(signaturePropertiesFile);
            if (sigProperties == null) {
                LOG.fine("Cannot load signature properties using: " + signaturePropertiesFile);
                return null;
            }
            try {
                signatureCrypto = CryptoFactory.getInstance(sigProperties);
            } catch (WSSecurityException ex) {
                LOG.fine("Error in loading the signature Crypto object: " + ex.getMessage());
                return null;
            }
        }
        return signatureCrypto;
    }
    
    protected CallbackHandler getCallbackHandler() {
        if (callbackHandler == null && callbackHandlerClass != null) {
            callbackHandler = getCallbackHandler(callbackHandlerClass);
            if (callbackHandler == null) {
                LOG.fine("Cannot load CallbackHandler using: " + callbackHandlerClass);
                return null;
            }
        }
        return callbackHandler;
    }
    
    private CallbackHandler getCallbackHandler(Object o) {
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = 
                    (CallbackHandler)ClassLoaderUtils.loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                LOG.fine(e.getMessage());
                handler = null;
            }
        }
        return handler;
    }
}

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

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandler;
import org.apache.ws.security.handler.WSHandlerConstants;

public abstract class AbstractWSS4JInterceptor extends WSHandler implements SoapInterceptor, 
    PhaseInterceptor<SoapMessage> {

    private static final Set<QName> HEADERS = new HashSet<QName>();
    static {
        HEADERS.add(new QName(WSConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSConstants.WSSE11_NS, "Security"));
        HEADERS.add(new QName(WSConstants.ENC_NS, "EncryptedData"));
    }

    private Map<String, Object> properties = new HashMap<String, Object>();
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private String phase;
    private String id;
    private Map<String, Crypto> cryptoTable = new ConcurrentHashMap<String, Crypto>();
    
    public AbstractWSS4JInterceptor() {
        super();
        id = getClass().getName();
    }

    public Set<URI> getRoles() {
        return null;
    }

    public void handleFault(SoapMessage message) {
    }

    public void postHandleMessage(SoapMessage message) throws Fault {
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Object getOption(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getPassword(Object msgContext) {
        return (String)((Message)msgContext).getContextualProperty("password");
    }

    public Object getProperty(Object msgContext, String key) {
        Object obj = ((Message)msgContext).getContextualProperty(key);
        if (obj == null) {
            obj = getOption(key);
        }
        return obj;
    }

    public void setPassword(Object msgContext, String password) {
        ((Message)msgContext).put("password", password);
    }

    public void setProperty(Object msgContext, String key, Object value) {
        ((Message)msgContext).put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Set<String> getAfter() {
        return after;
    }

    public void setAfter(Set<String> after) {
        this.after = after;
    }

    public Set<String> getBefore() {
        return before;
    }

    public void setBefore(Set<String> before) {
        this.before = before;
    }
    
    protected boolean isRequestor(SoapMessage message) {
        return MessageUtils.isRequestor(message);
    }  


    public Crypto loadSignatureCrypto(RequestData reqData) 
        throws WSSecurityException {
        Crypto crypto = null;
        /*
         *Get crypto property file for signature. If none specified throw
         * fault, otherwise get a crypto instance.
         */
        String sigPropFile = getString(WSHandlerConstants.SIG_PROP_FILE,
                   reqData.getMsgContext());
        String refId = null;
        if (sigPropFile != null) {
            crypto = cryptoTable.get(sigPropFile);
            if (crypto == null) {
                crypto = CryptoFactory.getInstance(sigPropFile, this
                        .getClassLoader(reqData.getMsgContext()));
                cryptoTable.put(sigPropFile, crypto);
            }
        } else if (getString(WSHandlerConstants.SIG_PROP_REF_ID, reqData
            .getMsgContext()) != null) {
            /*
             * If the property file is missing then 
             * look for the Properties object 
             */
            refId = getString(WSHandlerConstants.SIG_PROP_REF_ID,
                reqData.getMsgContext());
            if (refId != null) {
                Object propObj = getProperty(reqData.getMsgContext(), refId);
                if (propObj instanceof Properties) {
                    crypto = cryptoTable.get(refId);
                    if (crypto == null) {
                        crypto = CryptoFactory.getInstance((Properties)propObj);
                        cryptoTable.put(refId, crypto);
                    }
                }
            }
        } 
        return crypto;
    }

    protected Crypto loadDecryptionCrypto(RequestData reqData) 
        throws WSSecurityException {
        Crypto crypto = null;
        String decPropFile = getString(WSHandlerConstants.DEC_PROP_FILE,
                 reqData.getMsgContext());
        String refId = null;
        if (decPropFile != null) {
            crypto = cryptoTable.get(decPropFile);
            if (crypto == null) {
                crypto = CryptoFactory.getInstance(decPropFile, this
                        .getClassLoader(reqData.getMsgContext()));
                cryptoTable.put(decPropFile, crypto);
            }
        } else if (getString(WSHandlerConstants.DEC_PROP_REF_ID, reqData
            .getMsgContext()) != null) {
            /*
             * If the property file is missing then 
             * look for the Properties object 
             */
            refId = getString(WSHandlerConstants.DEC_PROP_REF_ID,
                reqData.getMsgContext());
            if (refId != null) {
                Object propObj = getProperty(reqData.getMsgContext(), refId);
                if (propObj instanceof Properties) {
                    crypto = cryptoTable.get(refId);
                    if (crypto == null) {
                        crypto = CryptoFactory.getInstance((Properties)propObj);
                        cryptoTable.put(refId, crypto);
                    }
                }
            }
        } 
        return crypto;
    }
    
    protected Crypto loadEncryptionCrypto(RequestData reqData) 
        throws WSSecurityException {
        Crypto crypto = null;
        /*
        * Get encryption crypto property file. If non specified take crypto
        * instance from signature, if that fails: throw fault
        */
        String encPropFile = getString(WSHandlerConstants.ENC_PROP_FILE,
                       reqData.getMsgContext());
        String refId = null;
        if (encPropFile != null) {
            crypto = cryptoTable.get(encPropFile);
            if (crypto == null) {
                crypto = CryptoFactory.getInstance(encPropFile, this
                        .getClassLoader(reqData.getMsgContext()));
                cryptoTable.put(encPropFile, crypto);
            }
        } else if (getString(WSHandlerConstants.ENC_PROP_REF_ID, reqData
                .getMsgContext()) != null) {
            /*
             * If the property file is missing then 
             * look for the Properties object 
             */
            refId = getString(WSHandlerConstants.ENC_PROP_REF_ID,
                    reqData.getMsgContext());
            if (refId != null) {
                Object propObj = getProperty(reqData.getMsgContext(), refId);
                if (propObj instanceof Properties) {
                    crypto = cryptoTable.get(refId);
                    if (crypto == null) {
                        crypto = CryptoFactory.getInstance((Properties)propObj);
                        cryptoTable.put(refId, crypto);
                    }
                }
            }
        } else if (reqData.getSigCrypto() == null) {
            return crypto;
        }
        return crypto;
    }

}

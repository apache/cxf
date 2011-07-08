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
package org.apache.cxf.systest.jaxrs.security.saml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.SAMLParms;

public class SamlOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final String CRYPTO_CACHE = "ws-security.crypto.cache";
    
    public SamlOutInterceptor() {
        super(Phase.PRE_MARSHAL);
    } 

    public void handleMessage(Message message) throws Fault {
        try {
            SAMLParms samlParms = new SAMLParms();
            samlParms.setCallbackHandler(new SamlCallbackHandler());
            AssertionWrapper assertion = new AssertionWrapper(samlParms);
            boolean selfSignAssertion = 
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.SELF_SIGN_SAML_ASSERTION, false
                );
            if (selfSignAssertion) {
                Crypto crypto = getCrypto(message, 
                                          SecurityConstants.SIGNATURE_CRYPTO,
                                          SecurityConstants.SIGNATURE_PROPERTIES);
                
                String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
                String user = (String)message.getContextualProperty(userNameKey);
                if (crypto != null && StringUtils.isEmpty(user)) {
                    try {
                        user = crypto.getDefaultX509Identifier();
                    } catch (WSSecurityException e1) {
                        throw new Fault(e1);
                    }
                }
                if (StringUtils.isEmpty(user)) {
                    return;
                }
        
                CallbackHandler handler = getCallbackHandler(message);
                String password = getPassword(handler, user, WSPasswordCallback.SIGNATURE);
                if (password == null) {
                    password = "";
                }
             
                // TODO configure using a KeyValue here
                assertion.signAssertion(user, password, crypto, false);
                
                String assertionValue = assertion.assertionToString();
                
                Deflater compresser = new Deflater();
                compresser.setInput(assertionValue.getBytes("UTF-8"));
                compresser.finish();
                
                byte[] output = new byte[4096];
                int compressedDataLength = compresser.deflate(output);
                
                StringWriter writer = new StringWriter();
                Base64Utility.encode(output, 0, compressedDataLength, writer);
                
                Map<String, List<String>> headers = 
                    CastUtils.cast((Map)message.get(Message.PROTOCOL_HEADERS));
                if (headers == null) {
                    headers = new HashMap<String, List<String>>();
                }
                
                StringBuilder builder = new StringBuilder();
                builder.append("SAML").append(" ").append(writer.toString());
                headers.put("Authorization", 
                    CastUtils.cast(Collections.singletonList(builder.toString()), String.class));
            }
        } catch (Exception ex) {
            // ignore
        }
    }
        
    private String getPassword(CallbackHandler handler, String userName, int type) {
        if (handler == null) {
            return null;
        }
        
        WSPasswordCallback[] cb = {new WSPasswordCallback(userName, type)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            return null;
        }
        
        //get the password
        return cb[0].getPassword();
    }
    
    private CallbackHandler getCallbackHandler(Message message) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
    
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
    
    private Crypto getCrypto(Message message,
                             String cryptoKey, 
                             String propKey) {
        Crypto crypto = (Crypto)message.getContextualProperty(cryptoKey);
        if (crypto != null) {
            return crypto;
        }
        
        Object o = message.getContextualProperty(propKey);
        if (o == null) {
            return null;
        }
        
        crypto = getCryptoCache(message).get(o);
        if (crypto != null) {
            return crypto;
        }
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            URL url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, this.getClass());
                }
                if (url == null) {
                    try {
                        url = new URL((String)o);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (url != null) {
                    InputStream ins = url.openStream();
                    properties = new Properties();
                    properties.load(ins);
                    ins.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                InputStream ins = ((URL)o).openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }            
        }
        
        if (properties != null) {
            try {
                crypto = CryptoFactory.getInstance(properties);
            } catch (Exception ex) {
                return null;
            }
            getCryptoCache(message).put(o, crypto);
        }
        return crypto;
    }
    
    protected final Map<Object, Crypto> getCryptoCache(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            Map<Object, Crypto> o = 
                CastUtils.cast((Map<?, ?>)message.getContextualProperty(CRYPTO_CACHE));
            if (o == null) {
                o = new ConcurrentHashMap<Object, Crypto>();
                info.setProperty(CRYPTO_CACHE, o);
            }
            return o;
        }
    }
    
}

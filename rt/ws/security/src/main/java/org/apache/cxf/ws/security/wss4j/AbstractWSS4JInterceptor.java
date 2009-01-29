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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.ws.security.WSConstants;
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
    
    private boolean isRequestor(SoapMessage message) {
        return Boolean.TRUE.equals(message.get(
            org.apache.cxf.message.Message.REQUESTOR_ROLE));
    }  
    
    protected void policyAsserted(AssertionInfoMap aim, PolicyAssertion assertion) {
        if (assertion == null) {
            return;
        }
        Collection<AssertionInfo> ais;
        ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setAsserted(true);
                }
            }
        }
    }
    protected void policyAsserted(AssertionInfoMap aim, QName qn) {
        Collection<AssertionInfo> ais;
        ais = aim.get(qn);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }
    }
    private static Properties getProps(Object o, SoapMessage message) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            URL url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, AbstractWSS4JInterceptor.class);
                }
                if (url != null) {
                    properties = new Properties();
                    properties.load(url.openStream());
                }
            } catch (IOException e) {
                properties = null;
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                properties.load(((URL)o).openStream());
            } catch (IOException e) {
                properties = null;
            }            
        }
        
        return properties;
    }
    
    String addToAction(String action, String val, boolean pre) {
        if (action.contains(val)) {
            return action;
        }
        if (pre) {
            return val + " " + action; 
        } 
        return action + " " + val;
    }
    boolean assertPolicy(AssertionInfoMap aim, QName q) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    String assertAsymetricBinding(AssertionInfoMap aim, String action, SoapMessage message) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                AsymmetricBinding abinding = (AsymmetricBinding)ai.getAssertion();
                if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
                    action = addToAction(action, "Signature", true);
                    action = addToAction(action, "Encrypt", true);
                } else {
                    action = addToAction(action, "Encrypt", true);
                    action = addToAction(action, "Signature", true);
                }
                Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
                Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
                if (e != null) {
                    message.put("SignaturePropRefId", "RefId-" + e.toString());
                    message.put("RefId-" + e.toString(), getProps(e, message));
                }
                if (s != null) {
                    message.put("decryptionPropRefId", "RefId-" + s.toString());
                    message.put("RefId-" + s.toString(), getProps(s, message));
                }
                ai.setAsserted(true);
                policyAsserted(aim, abinding.getInitiatorToken());
                policyAsserted(aim, abinding.getRecipientToken());
                policyAsserted(aim, abinding.getInitiatorToken().getToken());
                policyAsserted(aim, abinding.getRecipientToken().getToken());
                policyAsserted(aim, SP12Constants.ENCRYPTED_PARTS);
            }
        }
     
        return action;
    }
    String assertSymetricBinding(AssertionInfoMap aim, String action, SoapMessage message) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                SymmetricBinding abinding = (SymmetricBinding)ai.getAssertion();
                if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
                    action = addToAction(action, "Signature", true);
                    action = addToAction(action, "Encrypt", true);
                } else {
                    action = addToAction(action, "Encrypt", true);
                    action = addToAction(action, "Signature", true);
                }
                Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
                Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
                if (abinding.getProtectionToken() != null) {
                    s = e;
                }
                if (isRequestor(message)) {
                    if (e != null) {
                        message.put("SignaturePropRefId", "RefId-" + e.toString());
                        message.put("RefId-" + e.toString(), getProps(e, message));
                    }
                    if (s != null) {
                        message.put("decryptionPropRefId", "RefId-" + s.toString());
                        message.put("RefId-" + s.toString(), getProps(s, message));
                    }
                } else {
                    if (s != null) {
                        message.put("SignaturePropRefId", "RefId-" + s.toString());
                        message.put("RefId-" + s.toString(), getProps(s, message));
                    }
                    if (e != null) {
                        message.put("decryptionPropRefId", "RefId-" + e.toString());
                        message.put("RefId-" + e.toString(), getProps(e, message));
                    }
                }
                ai.setAsserted(true);
                policyAsserted(aim, abinding.getEncryptionToken());
                policyAsserted(aim, abinding.getSignatureToken());
                policyAsserted(aim, abinding.getProtectionToken());
                policyAsserted(aim, SP12Constants.ENCRYPTED_PARTS);
            }
        }
        return action;
    }
    void assertWSS11(AssertionInfoMap aim, SoapMessage message) {
        if (isRequestor(message)) {
            message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "false");
        }
        Collection<AssertionInfo> ais = aim.get(SP12Constants.WSS11);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
                Wss11 wss11 = (Wss11)ai.getAssertion();
                if (isRequestor(message)) {
                    message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, 
                                wss11.isRequireSignatureConfirmation() ? "true" : "false");
                }
            }
        }
    }
    
    protected PolicyAssertion findAndAssertPolicy(AssertionInfoMap aim, QName n) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(n);
        if (ais != null && !ais.isEmpty()) {
            AssertionInfo ai = ais.iterator().next();
            ai.setAsserted(true);
            return ai.getAssertion();
        }
        return null;
    }
    protected String assertSupportingTokens(AssertionInfoMap aim,
                                          SoapMessage message, 
                                          String action,
                                          QName n) {
        SupportingToken sp = (SupportingToken)findAndAssertPolicy(aim, n);
        if (sp != null) {
            action = doTokens(sp.getTokens(), action, aim, message);
        }
        return action;
    }
    protected void checkPolicies(SoapMessage message, RequestData data) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        String action = getString(WSHandlerConstants.ACTION, message);
        if (action == null) {
            action = "";
        }
        if (aim != null) {
            if (assertPolicy(aim, SP12Constants.INCLUDE_TIMESTAMP)) {
                action = addToAction(action, WSHandlerConstants.TIMESTAMP, true);
            }
            assertPolicy(aim, SP12Constants.LAYOUT);
            assertPolicy(aim, SP12Constants.TRANSPORT_BINDING);
            action = assertAsymetricBinding(aim, action, message);
            action = assertSymetricBinding(aim, action, message);
            
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.SIGNED_SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.ENDORSING_SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, 
                                            SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.SUPPORTING_TOKENS);
            action = assertSupportingTokens(aim, message, 
                                            action, SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
            assertWSS11(aim, message);
            assertPolicy(aim, SP12Constants.WSS10);
            assertPolicy(aim, SP12Constants.TRUST_13);
            assertPolicy(aim, SP11Constants.TRUST_10);
            policyAsserted(aim, SP12Constants.SIGNED_PARTS);

            message.put(WSHandlerConstants.ACTION, action.trim());
        }
    }
    
    private String doTokens(List<Token> tokens, 
                            String action, 
                            AssertionInfoMap aim,
                            SoapMessage msg) {
        for (Token token : tokens) {
            if (token instanceof UsernameToken) {
                if (!action.contains(WSHandlerConstants.USERNAME_TOKEN)
                    && !isRequestor(msg)) {
                    action = WSHandlerConstants.USERNAME_TOKEN + " " + action;
                }
                Collection<AssertionInfo> ais2 = aim.get(SP12Constants.USERNAME_TOKEN);
                if (ais2 != null && !ais2.isEmpty()) {
                    for (AssertionInfo ai2 : ais2) {
                        if (ai2.getAssertion() == token) {
                            ai2.setAsserted(true);
                        }
                    }                    
                }
            } else {
                Collection<AssertionInfo> ais2 = aim.get(token.getName());
                if (ais2 != null && !ais2.isEmpty()) {
                    for (AssertionInfo ai2 : ais2) {
                        if (ai2.getAssertion() == token) {
                            ai2.setAsserted(true);
                        }
                    }                    
                }
            }
        }        
        return action;
    }
}

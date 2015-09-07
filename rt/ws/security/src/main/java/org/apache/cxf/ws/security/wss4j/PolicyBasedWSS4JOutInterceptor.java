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

import java.security.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.policyhandlers.AsymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.SymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.TransportBindingHandler;
import org.apache.neethi.Policy;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AsymmetricBinding;
import org.apache.wss4j.policy.model.SymmetricBinding;
import org.apache.wss4j.policy.model.TransportBinding;

public class PolicyBasedWSS4JOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public static final String SECURITY_PROCESSED = PolicyBasedWSS4JOutInterceptor.class.getName() + ".DONE";
    public static final PolicyBasedWSS4JOutInterceptor INSTANCE = new PolicyBasedWSS4JOutInterceptor();
    
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyBasedWSS4JOutInterceptor.class);

    
    private PolicyBasedWSS4JOutInterceptorInternal ending;
    private SAAJOutInterceptor saajOut = new SAAJOutInterceptor();    

    public PolicyBasedWSS4JOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJOutInterceptor.class.getName());
        ending = createEndingInterceptor();
    }


    public void handleMessage(SoapMessage mc) throws Fault {
        boolean enableStax = 
            MessageUtils.isTrue(mc.getContextualProperty(SecurityConstants.ENABLE_STREAMING_SECURITY));
        if (!enableStax) {
            if (mc.getContent(SOAPMessage.class) == null) {
                saajOut.handleMessage(mc);
            }
            mc.put(SECURITY_PROCESSED, Boolean.TRUE);
            mc.getInterceptorChain().add(ending);
        }
    }    
    public void handleFault(SoapMessage message) {
        saajOut.handleFault(message);
    } 
    
    public final PolicyBasedWSS4JOutInterceptorInternal createEndingInterceptor() {
        return new PolicyBasedWSS4JOutInterceptorInternal();
    }
    
    public final class PolicyBasedWSS4JOutInterceptorInternal 
        implements PhaseInterceptor<SoapMessage> {
        public PolicyBasedWSS4JOutInterceptorInternal() {
            super();
        }

        public void handleMessage(SoapMessage message) throws Fault {
            Object provider = message.getExchange().get(Provider.class);
            final boolean useCustomProvider = provider != null && ThreadLocalSecurityProvider.isInstalled();
            try {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.setProvider((Provider)provider);
                }
                handleMessageInternal(message);
            } finally {
                if (useCustomProvider) {
                    ThreadLocalSecurityProvider.unsetProvider();
                }
            }
        }
        
        private void handleMessageInternal(SoapMessage message) throws Fault {
            Collection<AssertionInfo> ais;
            SOAPMessage saaj = message.getContent(SOAPMessage.class);

            boolean mustUnderstand = 
                MessageUtils.getContextualBoolean(
                    message, SecurityConstants.MUST_UNDERSTAND, true
                );
            String actor = (String)message.getContextualProperty(SecurityConstants.ACTOR);
            
            if (AttachmentUtil.isMtomEnabled(message) && hasAttachments(message)) {
                LOG.warning("MTOM is enabled with WS-Security. Please note that if an attachment is"
                    + "referenced in the SOAP Body, only the reference will be signed and not the"
                    + "SOAP Body!");
            }

            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                AbstractBinding transport = null;
                ais = getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
                if (!ais.isEmpty()) {
                    for (AssertionInfo ai : ais) {
                        transport = (AbstractBinding)ai.getAssertion();
                        ai.setAsserted(true);
                    }                    
                }
                ais = getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
                if (!ais.isEmpty()) {
                    for (AssertionInfo ai : ais) {
                        transport = (AbstractBinding)ai.getAssertion();
                        ai.setAsserted(true);
                    }                    
                }
                ais = getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
                if (!ais.isEmpty()) {
                    for (AssertionInfo ai : ais) {
                        transport = (AbstractBinding)ai.getAssertion();
                        ai.setAsserted(true);
                    }                    
                }

                if (transport == null && isRequestor(message)) {
                    Policy policy = new Policy();
                    transport = new TransportBinding(org.apache.wss4j.policy.SPConstants.SPVersion.SP11,
                                                     policy);
                }
                
                if (transport != null) {
                    WSSecHeader secHeader = new WSSecHeader(actor, mustUnderstand);
                    Element el = null;
                    try {
                        el = secHeader.insertSecurityHeader(saaj.getSOAPPart());
                    } catch (WSSecurityException e) {
                        throw new SoapFault(
                            new Message("SECURITY_FAILED", LOG), e, message.getVersion().getSender()
                        );
                    }
                    try {
                        //move to end
                        SAAJUtils.getHeader(saaj).removeChild(el);
                        SAAJUtils.getHeader(saaj).appendChild(el);
                    } catch (SOAPException e) {
                        //ignore
                    }
                    
                    WSSConfig config = (WSSConfig)message.getContextualProperty(WSSConfig.class.getName());
                    if (config == null) {
                        config = WSSConfig.getNewInstance();
                    }
                    translateProperties(message);
                    
                    String asymSignatureAlgorithm = 
                        (String)message.getContextualProperty(SecurityConstants.ASYMMETRIC_SIGNATURE_ALGORITHM);
                    if (asymSignatureAlgorithm != null && transport.getAlgorithmSuite() != null) {
                        transport.getAlgorithmSuite().setAsymmetricSignature(asymSignatureAlgorithm);
                    }
                    String symSignatureAlgorithm = 
                        (String)message.getContextualProperty(SecurityConstants.SYMMETRIC_SIGNATURE_ALGORITHM);
                    if (symSignatureAlgorithm != null && transport.getAlgorithmSuite() != null) {
                        transport.getAlgorithmSuite().setSymmetricSignature(symSignatureAlgorithm);
                    }

                    if (transport instanceof TransportBinding) {
                        new TransportBindingHandler(config, (TransportBinding)transport, saaj,
                                                    secHeader, aim, message).handleBinding();
                    } else if (transport instanceof SymmetricBinding) {
                        new SymmetricBindingHandler(config, (SymmetricBinding)transport, saaj,
                                                     secHeader, aim, message).handleBinding();
                    } else {
                        new AsymmetricBindingHandler(config, (AsymmetricBinding)transport, saaj,
                                                     secHeader, aim, message).handleBinding();
                    }
                    
                    if (el.getFirstChild() == null) {
                        el.getParentNode().removeChild(el);
                    }
                }
            }
            
        }
        
        private boolean hasAttachments(SoapMessage mc) {
            final Collection<org.apache.cxf.message.Attachment> attachments = mc.getAttachments();
            return attachments != null && attachments.size() > 0;
        }
        
        public Set<String> getAfter() {
            return Collections.emptySet();
        }

        public Set<String> getBefore() {
            return Collections.emptySet();
        }

        public String getId() {
            return PolicyBasedWSS4JOutInterceptorInternal.class.getName();
        }

        public String getPhase() {
            return Phase.POST_PROTOCOL;
        }

        public void handleFault(SoapMessage message) {
            //nothing
        }

        public Collection<PhaseInterceptor<? extends org.apache.cxf.message.Message>> 
        getAdditionalInterceptors() {
            
            return null;
        }
        
        private void translateProperties(SoapMessage msg) {
            String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
            if (bspCompliant != null) {
                msg.put(WSHandlerConstants.IS_BSP_COMPLIANT, bspCompliant);
            }
        }
        
        private Collection<AssertionInfo> getAllAssertionsByLocalname(
            AssertionInfoMap aim,
            String localname
        ) {
            Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
            Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
            
            if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
                Collection<AssertionInfo> ais = new HashSet<AssertionInfo>();
                if (sp11Ais != null) {
                    ais.addAll(sp11Ais);
                }
                if (sp12Ais != null) {
                    ais.addAll(sp12Ais);
                }
                return ais;
            }
                
            return Collections.emptySet();
        }
    }
}

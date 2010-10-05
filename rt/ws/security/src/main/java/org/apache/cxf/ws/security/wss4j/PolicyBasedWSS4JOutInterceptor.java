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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.cxf.ws.security.policy.model.TransportBinding;
import org.apache.cxf.ws.security.wss4j.policyhandlers.AsymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.SymmetricBindingHandler;
import org.apache.cxf.ws.security.wss4j.policyhandlers.TransportBindingHandler;
import org.apache.ws.security.message.WSSecHeader;

public class PolicyBasedWSS4JOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public static final String SECURITY_PROCESSED = PolicyBasedWSS4JOutInterceptor.class.getName() + ".DONE";
    public static final PolicyBasedWSS4JOutInterceptor INSTANCE = new PolicyBasedWSS4JOutInterceptor();
    
    private PolicyBasedWSS4JOutInterceptorInternal ending;
    private SAAJOutInterceptor saajOut = new SAAJOutInterceptor();    

    public PolicyBasedWSS4JOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJOutInterceptor.class.getName());
        ending = createEndingInterceptor();
    }


    public void handleMessage(SoapMessage mc) throws Fault {
        if (mc.getContent(SOAPMessage.class) == null) {
            saajOut.handleMessage(mc);
        }
        mc.put(SECURITY_PROCESSED, Boolean.TRUE);
        mc.getInterceptorChain().add(ending);
    }    
    public void handleFault(SoapMessage message) {
        saajOut.handleFault(message);
    } 
    
    public final PolicyBasedWSS4JOutInterceptorInternal createEndingInterceptor() {
        return new PolicyBasedWSS4JOutInterceptorInternal();
    }
    
    final class PolicyBasedWSS4JOutInterceptorInternal 
        implements PhaseInterceptor<SoapMessage> {
        public PolicyBasedWSS4JOutInterceptorInternal() {
            super();
        }

        public void handleMessage(SoapMessage message) throws Fault {
            Collection<AssertionInfo> ais;
            SOAPMessage saaj = message.getContent(SOAPMessage.class);

            boolean mustUnderstand = true;
            String actor = null;
            

            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Binding transport = null;
                ais = aim.get(SP12Constants.TRANSPORT_BINDING);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        transport = (Binding)ai.getAssertion();
                        ai.setAsserted(true);
                    }                    
                }
                ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        transport = (Binding)ai.getAssertion();
                        ai.setAsserted(true);
                    }                    
                }
                ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        transport = (Binding)ai.getAssertion();
                        ai.setAsserted(true);
                    }                    
                }
                if (transport == null && isRequestor(message)) {
                    transport = new TransportBinding(SP12Constants.INSTANCE);
                }
                
                if (transport != null) {
                    WSSecHeader secHeader = new WSSecHeader(actor, mustUnderstand);
                    Element el = secHeader.insertSecurityHeader(saaj.getSOAPPart());
                    try {
                        //move to end
                        saaj.getSOAPHeader().removeChild(el);
                        saaj.getSOAPHeader().appendChild(el);
                    } catch (SOAPException e) {
                        //ignore
                    }
                    
                    
                    if (transport instanceof TransportBinding) {
                        new TransportBindingHandler((TransportBinding)transport, saaj,
                                                    secHeader, aim, message).handleBinding();
                    } else if (transport instanceof SymmetricBinding) {
                        new SymmetricBindingHandler((SymmetricBinding)transport, saaj,
                                                     secHeader, aim, message).handleBinding();
                    } else {
                        new AsymmetricBindingHandler((AsymmetricBinding)transport, saaj,
                                                     secHeader, aim, message).handleBinding();
                    }
                    
                    if (el.getFirstChild() == null) {
                        el.getParentNode().removeChild(el);
                    }
                }
                
                ais = aim.get(SP12Constants.WSS10);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
                ais = aim.get(SP12Constants.WSS11);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }

            }
            
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
    }
}

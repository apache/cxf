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

import javax.xml.soap.SOAPMessage;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecTimestamp;

public class PolicyBasedWSS4JOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
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
            
            WSSecHeader secHeader = new WSSecHeader(actor, mustUnderstand);
            secHeader.insertSecurityHeader(saaj.getSOAPPart());

            
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                WSSecTimestamp timestamp = null;
                ais = aim.get(SP12Constants.INCLUDE_TIMESTAMP);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        timestamp = new WSSecTimestamp();
                        timestamp.prepare(saaj.getSOAPPart());
                        ai.setAsserted(true);
                    }                    
                }
                ais = aim.get(SP12Constants.LAYOUT);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        Layout layout = (Layout)ai.getAssertion();
                        if (SPConstants.LAYOUT_LAX_TIMESTAMP_LAST.equals(layout.getValue())) {
                            if (timestamp == null) {
                                ai.setAsserted(false);
                            } else {
                                ai.setAsserted(true);
                                //get the timestamp into the header first before anything else
                                timestamp.prependToHeader(secHeader);
                                timestamp = null;
                            }
                        } else if (SPConstants.LAYOUT_STRICT.equals(layout.getValue())) {
                            //FIXME - don't have strict writing working yet
                            ai.setAsserted(false);
                        } else {
                            ai.setAsserted(true);                            
                        }
                    }                    
                }
                ais = aim.get(SP12Constants.TRANSPORT_BINDING);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }                    
                }
                if (timestamp != null) {
                    timestamp.prependToHeader(secHeader);
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

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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.util.WSSecurityUtil;

public class WSS4JOutInterceptor extends AbstractWSS4JInterceptor {
    private static final Logger LOG = LogUtils
            .getL7dLogger(WSS4JOutInterceptor.class);

    private static final Logger TIME_LOG = LogUtils
            .getL7dLogger(WSS4JOutInterceptor.class,
                          null,
                          WSS4JOutInterceptor.class.getName() + "-Time");
    private WSS4JOutInterceptorInternal ending;
    private SAAJOutInterceptor saajOut = new SAAJOutInterceptor();
    private boolean mtomEnabled;
    
    

    public WSS4JOutInterceptor() {
        super();
        setPhase(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJOutInterceptor.class.getName());
        
        ending = createEndingInterceptor();
    }

    public WSS4JOutInterceptor(Map<String, Object> props) {
        this();
        setProperties(props);
    }
    
    public boolean isAllowMTOM() {
        return mtomEnabled;
    }
    /**
     * Enable or disable mtom with WS-Security.   By default MTOM is disabled as
     * attachments would not get encrypted or be part of the signature.
     * @param mtomEnabled
     */
    public void setAllowMTOM(boolean allowMTOM) {
        this.mtomEnabled = allowMTOM;
    }

    public void handleMessage(SoapMessage mc) throws Fault {
        //must turn off mtom when using WS-Sec so binary is inlined so it can
        //be properly signed/encrypted/etc...
        if (!mtomEnabled) {
            mc.put(org.apache.cxf.message.Message.MTOM_ENABLED, false);
        }
        
        if (mc.getContent(SOAPMessage.class) == null) {
            saajOut.handleMessage(mc);
        }
        
        mc.getInterceptorChain().add(ending);
    }    
    public void handleFault(SoapMessage message) {
        saajOut.handleFault(message);
    } 
    
    public final WSS4JOutInterceptorInternal createEndingInterceptor() {
        return new WSS4JOutInterceptorInternal();
    }
    
    final class WSS4JOutInterceptorInternal 
        implements PhaseInterceptor<SoapMessage> {
        public WSS4JOutInterceptorInternal() {
            super();
        }

        public void handleMessage(SoapMessage mc) throws Fault {
            
            boolean doDebug = LOG.isLoggable(Level.FINE);
            boolean doTimeDebug = TIME_LOG.isLoggable(Level.FINE);
            SoapVersion version = mc.getVersion();
    
            long t0 = 0;
            long t1 = 0;
            long t2 = 0;
    
            if (doTimeDebug) {
                t0 = System.currentTimeMillis();
            }
    
            if (doDebug) {
                LOG.fine("WSS4JOutInterceptor: enter handleMessage()");
            }
    
            RequestData reqData = new RequestData();
    
            reqData.setMsgContext(mc);
            
            /*
             * The overall try, just to have a finally at the end to perform some
             * housekeeping.
             */
            try {
                /*
                 * Get the action first.
                 */
                Vector actions = new Vector();
                String action = getString(WSHandlerConstants.ACTION, mc);
                if (action == null) {
                    throw new SoapFault(new Message("NO_ACTION", LOG), version
                            .getReceiver());
                }
    
                int doAction = WSSecurityUtil.decodeAction(action, actions);
                if (doAction == WSConstants.NO_SECURITY) {
                    return;
                }
    
                /*
                 * For every action we need a username, so get this now. The
                 * username defined in the deployment descriptor takes precedence.
                 */
                reqData.setUsername((String) getOption(WSHandlerConstants.USER));
                if (reqData.getUsername() == null
                        || reqData.getUsername().equals("")) {
                    String username = (String) getProperty(reqData.getMsgContext(),
                            WSHandlerConstants.USER);
                    if (username != null) {
                        reqData.setUsername(username);
                    }
                }
    
                /*
                 * Now we perform some set-up for UsernameToken and Signature
                 * functions. No need to do it for encryption only. Check if
                 * username is available and then get a passowrd.
                 */
                if ((doAction & (WSConstants.SIGN | WSConstants.UT | WSConstants.UT_SIGN)) != 0
                        && (reqData.getUsername() == null
                        || reqData.getUsername().equals(""))) {
                    /*
                     * We need a username - if none throw an SoapFault. For
                     * encryption there is a specific parameter to get a username.
                     */
                    throw new SoapFault(new Message("NO_USERNAME", LOG), version
                            .getReceiver());
                }
                if (doDebug) {
                    LOG.fine("Action: " + doAction);
                    LOG.fine("Actor: " + reqData.getActor());
                }
                /*
                 * Now get the SOAP part from the request message and convert it
                 * into a Document. This forces CXF to serialize the SOAP request
                 * into FORM_STRING. This string is converted into a document.
                 * During the FORM_STRING serialization CXF performs multi-ref of
                 * complex data types (if requested), generates and inserts
                 * references for attachements and so on. The resulting Document
                 * MUST be the complete and final SOAP request as CXF would send it
                 * over the wire. Therefore this must shall be the last (or only)
                 * handler in a chain. Now we can perform our security operations on
                 * this request.
                 */
                SOAPMessage saaj = mc.getContent(SOAPMessage.class);
    
                if (saaj == null) {
                    LOG.warning("SAAJOutHandler must be enabled for WS-Security!");
                    throw new SoapFault(new Message("NO_SAAJ_DOC", LOG), version
                            .getReceiver());
                }
    
                Document doc = saaj.getSOAPPart();
                /**
                 * There is nothing to send...Usually happens when the provider
                 * needs to send a HTTP 202 message (with no content)
                 */
                if (mc == null) {
                    return;
                }
    
                if (doTimeDebug) {
                    t1 = System.currentTimeMillis();
                }
    
                doSenderAction(doAction, doc, reqData, actions, Boolean.TRUE
                        .equals(getProperty(mc, org.apache.cxf.message.Message.REQUESTOR_ROLE)));
    
                if (doTimeDebug) {
                    t2 = System.currentTimeMillis();
                    TIME_LOG.fine("Send request: total= " + (t2 - t0)
                            + " request preparation= " + (t1 - t0)
                            + " request processing= " + (t2 - t1)
                            + "\n");
                }
    
                if (doDebug) {
                    LOG.fine("WSS4JOutInterceptor: exit handleMessage()");
                }
            } catch (WSSecurityException e) {
                throw new SoapFault(new Message("SECURITY_FAILED", LOG), e, version
                        .getSender());
            } finally {
                reqData.clear();
                reqData = null;
            }
        }

        public Set<String> getAfter() {
            return Collections.emptySet();
        }

        public Set<String> getBefore() {
            return Collections.emptySet();
        }

        public String getId() {
            return WSS4JOutInterceptorInternal.class.getName();
        }

        public String getPhase() {
            return Phase.POST_PROTOCOL;
        }

        public void handleFault(SoapMessage message) {
            //nothing
        }
    }
}

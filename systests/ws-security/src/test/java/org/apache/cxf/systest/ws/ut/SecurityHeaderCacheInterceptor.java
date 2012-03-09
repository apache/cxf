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

package org.apache.cxf.systest.ws.ut;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;

/**
 * Cache the first security header and then use it instead of all subsequent security headers, until
 * clear() is called.
 */
public class SecurityHeaderCacheInterceptor implements PhaseInterceptor<SoapMessage> {
    
    private static final QName SEC_HEADER = 
        new QName(WSConstants.WSSE_NS, WSConstants.WSSE_LN, WSConstants.WSSE_PREFIX);
    private Set<String> afterInterceptors = new HashSet<String>();
    private SOAPHeaderElement cachedSecurityHeader;
    
    public SecurityHeaderCacheInterceptor() {
        getAfter().add(PolicyBasedWSS4JOutInterceptor.class.getName());
    }
    
    public void handleMessage(SoapMessage mc) throws Fault {
        SOAPMessage saaj = mc.getContent(SOAPMessage.class);
        if (cachedSecurityHeader == null) {
            try {
                Iterator<?> cachedHeadersIterator = 
                    saaj.getSOAPHeader().getChildElements(SEC_HEADER);
                if (cachedHeadersIterator.hasNext()) {
                    cachedSecurityHeader = (SOAPHeaderElement)cachedHeadersIterator.next();
                }
            } catch (SOAPException e) {
                // Ignore
            }
        } else {
            try {
                saaj.getSOAPHeader().removeContents();
                
                SOAPHeaderElement secHeaderElement = 
                    saaj.getSOAPHeader().addHeaderElement(SEC_HEADER);
                
                Iterator<?> cachedHeadersIterator = 
                    cachedSecurityHeader.getChildElements();
                while (cachedHeadersIterator.hasNext()) {
                    secHeaderElement.addChildElement((SOAPElement)cachedHeadersIterator.next());
                }
                
            } catch (SOAPException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void clear() {
        cachedSecurityHeader = null;
    }
    
    public void handleFault(SoapMessage arg0) {
        // Complete
    }

    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    public Set<String> getAfter() {
        return afterInterceptors;
    }

    public Set<String> getBefore() {
        return Collections.emptySet();
    }

    public String getId() {
        return SecurityHeaderCacheInterceptor.class.getName();
    }

    public String getPhase() {
        return Phase.PRE_PROTOCOL_ENDING;
    }
    
}

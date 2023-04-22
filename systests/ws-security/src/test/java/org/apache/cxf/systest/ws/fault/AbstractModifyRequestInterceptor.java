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

package org.apache.cxf.systest.ws.fault;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.wss4j.dom.WSConstants;

public abstract class AbstractModifyRequestInterceptor implements PhaseInterceptor<SoapMessage> {

    private static final QName SEC_HEADER =
        new QName(WSConstants.WSSE_NS, WSConstants.WSSE_LN, WSConstants.WSSE_PREFIX);
    private Set<String> afterInterceptors = new HashSet<>();

    public AbstractModifyRequestInterceptor() {
        getAfter().add(PolicyBasedWSS4JOutInterceptor.class.getName());
    }

    public void handleMessage(SoapMessage mc) throws Fault {
        SOAPMessage saaj = mc.getContent(SOAPMessage.class);
        try {
            Iterator<?> secHeadersIterator =
                SAAJUtils.getHeader(saaj).getChildElements(SEC_HEADER);
            if (secHeadersIterator.hasNext()) {
                SOAPHeaderElement securityHeader =
                    (SOAPHeaderElement)secHeadersIterator.next();
                modifySecurityHeader(securityHeader);
            }

            modifySOAPBody(SAAJUtils.getBody(saaj));
        } catch (SOAPException ex) {
            throw new Fault(ex);
        }
    }

    public abstract void modifySecurityHeader(Element securityHeader);

    public abstract void modifySOAPBody(Element soapBody);

    public void clear() {
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
        return AbstractModifyRequestInterceptor.class.getName();
    }

    public String getPhase() {
        return Phase.PRE_PROTOCOL_ENDING;
    }

}

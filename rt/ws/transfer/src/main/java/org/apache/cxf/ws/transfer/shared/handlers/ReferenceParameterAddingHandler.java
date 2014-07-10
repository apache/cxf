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

package org.apache.cxf.ws.transfer.shared.handlers;

import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.Element;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.shared.TransferConstants;

/**
 * Handler for adding reference parameter to the outgoing SOAP messages.
 * 
 * @author Erich Duda
 */
public class ReferenceParameterAddingHandler implements SOAPHandler<SOAPMessageContext> {
    
    private final ReferenceParametersType refParams;
    
    public ReferenceParameterAddingHandler(ReferenceParametersType refParams) {
        this.refParams = refParams;
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // we are interested only in outbound messages here
        if (!(Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            return true;
        }
        if (refParams == null) {
            return true;
        }
        for (Object o : refParams.getAny()) {
            try {
                SOAPElement refParamEl = SOAPFactory.newInstance().createElement((Element) o);
                refParamEl.addAttribute(new QName(
                        TransferConstants.REF_PARAMS_EL_NAMESPACE, TransferConstants.REF_PARAMS_ATTR_NAME, "wsa"),
                        "true");
                context.getMessage().getSOAPHeader().addChildElement(refParamEl);
            } catch (SOAPException ex) {
                throw new RuntimeException(ex);
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext c) {
        return true;
    }

    @Override
    public void close(MessageContext mc) {
    }
    
}

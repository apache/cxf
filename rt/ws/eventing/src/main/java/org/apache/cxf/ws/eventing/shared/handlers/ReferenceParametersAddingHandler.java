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

package org.apache.cxf.ws.eventing.shared.handlers;

import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

public class ReferenceParametersAddingHandler implements SOAPHandler<SOAPMessageContext> {

    private final ReferenceParametersType params;

    public ReferenceParametersAddingHandler(ReferenceParametersType parametersType) {
        this.params = parametersType;
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
        if (params == null) {
            return true;
        }
        try {
            SOAPFactory factory = SOAPFactory.newInstance();
            for (Object o : params.getAny()) {
                SOAPElement elm = factory.createElement((Element)o);
                context.getMessage().getSOAPHeader()
                        .addChildElement(SOAPFactory.newInstance().createElement(elm));
            }
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }
}
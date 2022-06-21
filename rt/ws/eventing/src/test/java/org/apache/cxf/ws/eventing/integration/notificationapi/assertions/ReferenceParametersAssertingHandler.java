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

package org.apache.cxf.ws.eventing.integration.notificationapi.assertions;

import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

public class ReferenceParametersAssertingHandler implements SOAPHandler<SOAPMessageContext> {

    private ReferenceParametersType params;

    public ReferenceParametersAssertingHandler(ReferenceParametersType params) {
        this.params = params;
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        if ((Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            return true;
        }
        if (params == null) {
            return true;
        }
        try {
            // every element in the ReferenceParametersType should be present somewhere in the headers
            for (Object exp : params.getAny()) {
                JAXBElement<?> expectedElement = (JAXBElement<?>)exp;
                boolean found = false;
                Element actualHeaderelement = DOMUtils.getFirstElement(context.getMessage().getSOAPHeader());
                while (actualHeaderelement != null) {
                    if (expectedElement.getName().getLocalPart().equals(actualHeaderelement.getLocalName())
                            && expectedElement.getName().getNamespaceURI()
                            .equals(actualHeaderelement.getNamespaceURI())) {
                        found = true;
                        break;
                    }
                    actualHeaderelement = DOMUtils.getNextElement(actualHeaderelement);
                }
                if (!found) {
                    throw new RuntimeException("Event sink should have received Reference parameter: "
                        + expectedElement.getName());
                }
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

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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.eventing.shared.EventingConstants;

/**
 * Subscription reference parsing handler is a SOAP handler on the Subscription Manager's side
 * which takes care of parsing the reference parameters and retrieving the subscription
 * ID from SOAP headers before the message is passed to the Subscription Manager itself.
 * In handleMessage method, it is supposed to retrieve the UUID of the subscription and
 * save it into the SOAPMessageContext as a String with the key 'uuid'
 */
public class SubscriptionReferenceParsingHandler implements SOAPHandler<SOAPMessageContext> {

    private static final Logger LOG = LogUtils.getLogger(SubscriptionReferenceParsingHandler.class);

    private final String namespace;
    private final String elementName;

    public SubscriptionReferenceParsingHandler(String namespace, String elementName) {
        this.namespace = namespace;
        this.elementName = elementName;
    }

    public SubscriptionReferenceParsingHandler() {
        this.namespace = EventingConstants.SUBSCRIPTION_ID_DEFAULT_NAMESPACE;
        this.elementName = EventingConstants.SUBSCRIPTION_ID_DEFAULT_ELEMENT_NAME;
    }



    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // we are interested only in inbound messages here
        if ((Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            return true;
        }
        try {
            // read headers
            LOG.finer("Examining header elements");
            Element el = DOMUtils.getFirstElement(context.getMessage().getSOAPHeader());
            while (el != null) {
                if (el.getNamespaceURI().equals(namespace)
                    && el.getLocalName().equals(elementName)) {
                    LOG.log(Level.FINE, "found UUID parameter in header, uuid={0}", el.getTextContent());
                    context.put("uuid", el.getTextContent());
                }
                el = DOMUtils.getNextElement(el);
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

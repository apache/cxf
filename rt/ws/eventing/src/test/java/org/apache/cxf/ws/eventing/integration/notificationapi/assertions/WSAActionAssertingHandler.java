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

import java.util.Iterator;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Element;

/**
 * Handler that asserts a particular value of WS-Addressing Action in the headers
 * of an incoming message. Used only for testing.
 */
public class WSAActionAssertingHandler implements SOAPHandler<SOAPMessageContext> {

    /**
     * The action which we expect to be set for incoming events into an event sink
     * which uses this handler.
     */
    private String action;

    public WSAActionAssertingHandler(String action) {
        this.action = action;
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // only inbound messages are of use
        if ((Boolean)context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            return true;
        }
        try {
            Iterator i = context.getMessage().getSOAPHeader().examineAllHeaderElements();
            Object header;
            while (i.hasNext()) {
                header = i.next();
                Element elm = (Element)header;
                if (elm.getTagName().equals("Action") && elm.getNamespaceURI().contains("addressing")) {
                    if (!elm.getTextContent().equals(action)) {
                        throw new RuntimeException("The event sink should have received "
                                + "WSA-Action: " + action + " but received: "
                                + elm.getTextContent());
                    }
                    return true;
                }
            }
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("The event sink should have received a WSA-Action associated with"
                + "the notification");
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {
    }

}

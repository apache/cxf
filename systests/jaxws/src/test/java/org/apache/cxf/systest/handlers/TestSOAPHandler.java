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
package org.apache.cxf.systest.handlers;


import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;


import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.xml.soap.Detail;
import jakarta.xml.soap.DetailEntry;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.ProtocolException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;


/**
 * Describe class TestSOAPHandler here.
 */
public class  TestSOAPHandler extends TestHandlerBase
    implements SOAPHandler<SOAPMessageContext> {



    @Resource
    Bus bus;
    @Resource(name = "org.apache.cxf.wsdl.WSDLManager")
    org.apache.cxf.wsdl.WSDLManager manager;

    public TestSOAPHandler() {
        this(true);
    }

    public TestSOAPHandler(boolean serverSide) {
        super(serverSide);
    }


    @PostConstruct
    public void initPost() {
        if (bus == null) {
            throw new RuntimeException("No BUS");
        }
        if (manager == null) {
            throw new RuntimeException("No WSDL Manager");
        }
    }

    // Implementation of jakarta.xml.ws.handler.soap.SOAPHandler

    public final Set<QName> getHeaders() {
        return null;
    }

    public String getHandlerId() {
        return "soapHandler" + getId();
    }

    public boolean handleMessage(SOAPMessageContext ctx) {

        boolean continueProcessing = true;

        if (!isValidWsdlDescription(ctx.get(MessageContext.WSDL_DESCRIPTION))) {
            throw new RuntimeException("can't find WsdlDescription throws RuntimeException");
        }

        try {
            methodCalled("handleMessage");
            printHandlerInfo("handleMessage", isOutbound(ctx));

            Object b = ctx.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            boolean outbound = (Boolean)b;
            SOAPMessage msg = ctx.getMessage();

            if (isServerSideHandler()) {
                if (outbound) {
                    continueProcessing = getReturnValue(outbound, ctx);
                } else {
                    continueProcessing = getReturnValue(outbound, ctx);
                    if (!continueProcessing) {
                        outbound = true;
                    }
                }

                if (outbound) {
                    try {
                        // append handler id to SOAP response message
                        SOAPBody body = msg.getSOAPPart().getEnvelope().getBody();
                        Node resp = body.getFirstChild();

                        if (resp.getNodeName().contains("pingResponse")) {
                            Node child = resp.getFirstChild();
                            Document doc = resp.getOwnerDocument();
                            Node info = doc.createElementNS(child.getNamespaceURI(), child.getLocalName());
                            info.setPrefix("ns4");
                            info.appendChild(doc.createTextNode(getHandlerId()));
                            resp.appendChild(info);
                            msg.saveChanges();
                        }
                    } catch (DOMException e) {
                        e.printStackTrace();
                    }
                } else {
                    getHandlerInfoList(ctx).add(getHandlerId());
                }
            }
        } catch (SOAPException e) {
            e.printStackTrace();
        }
        return continueProcessing;
    }

    public boolean handleFault(SOAPMessageContext ctx) {
        methodCalled("handleFault");
        printHandlerInfo("handleFault", isOutbound(ctx));

        if (isServerSideHandler()) {

            if (!"soapHandler4".equals(getHandlerId())) {
                return true;
            }

            try {
                SOAPMessage msg = ctx.getMessage();
                String fs = msg.getSOAPPart().getEnvelope().getBody().getFault().getFaultString();
                if ("soapHandler4HandleFaultThrowsRunException".equals(fs)) {
                    throw new RuntimeException("soapHandler4 HandleFault throws RuntimeException");
                } else if ("soapHandler4HandleFaultThrowsSOAPFaultException".equals(fs)) {
                    throw createSOAPFaultException("soapHandler4 HandleFault throws SOAPFaultException");
                }
            } catch (SOAPException e) {
                // do nothing
            }
        }

        return true;
    }

    public final void destroy() {
        methodCalled("destroy");
    }

    public final void close(MessageContext messageContext) {
        methodCalled("close");
    }

    private boolean getReturnValue(boolean outbound, SOAPMessageContext ctx) {
        boolean ret = true;
        try {
            SOAPMessage msg = ctx.getMessage();
            SOAPBody body = msg.getSOAPPart().getEnvelope().getBody();

            if (body.getFirstChild().getFirstChild() == null) {
                return true;
            }

            Node commandNode = body.getFirstChild().getFirstChild().getFirstChild();
            String arg = commandNode.getNodeValue();
            String namespace = body.getFirstChild().getFirstChild().getNamespaceURI();

            StringTokenizer strtok = new StringTokenizer(arg, " ");
            String hid = "";
            String direction = "";
            String command = "";
            if (strtok.countTokens() >= 3) {
                hid = strtok.nextToken();
                direction = strtok.nextToken();
                command = strtok.nextToken();
            }

            if (!getHandlerId().equals(hid)) {
                return true;
            }

            if ("stop".equals(command)) {
                if (!outbound && "inbound".equals(direction)) {
                     // remove the incoming request body.
                    Document doc = body.getOwnerDocument();
                    // build the SOAP response for this message
                    //
                    Node wrapper = doc.createElementNS(namespace, "pingResponse");
                    wrapper.setPrefix("ns4");
                    body.removeChild(body.getFirstChild());
                    body.appendChild(wrapper);

                    for (String info : getHandlerInfoList(ctx)) {
                        // copy the previously invoked handler list into the response.
                        // Ignore this handler's information as it will be added again later.
                        //
                        if (!info.contains(getHandlerId())) {
                            Node newEl = doc.createElementNS(namespace, "HandlersInfo");
                            newEl.setPrefix("ns4");
                            newEl.appendChild(doc.createTextNode(info));
                            wrapper.appendChild(newEl);
                        }
                    }
                    ret = false;
                } else if (outbound && "outbound".equals(direction)) {
                    ret = false;
                }
            } else if ("throw".equals(command)) {
                String exceptionType = null;
                String exceptionText = "HandleMessage throws exception";
                if (strtok.hasMoreTokens()) {
                    exceptionType = strtok.nextToken();
                }
                if (strtok.hasMoreTokens()) {
                    exceptionText = strtok.nextToken();
                }
                if (exceptionType != null && !outbound && "inbound".equals(direction)) {
                    if ("RuntimeException".equals(exceptionType)) {
                        throw new RuntimeException(exceptionText);
                    } else if ("ProtocolException".equals(exceptionType)) {
                        throw new ProtocolException(exceptionText);
                    } else if ("SOAPFaultException".equals(exceptionType)) {
                        throw createSOAPFaultException(exceptionText);
                    } else if ("SOAPFaultExceptionWDetail".equals(exceptionType)) {
                        throw createSOAPFaultExceptionWithDetail(exceptionText);
                    }
                } else if (exceptionType != null && outbound && "outbound".equals(direction)) {
                    if ("RuntimeException".equals(exceptionType)) {
                        throw new RuntimeException(exceptionText);
                    } else if ("ProtocolException".equals(exceptionType)) {
                        throw new ProtocolException(exceptionText);
                    } else if ("SOAPFaultException".equals(exceptionType)) {
                        throw createSOAPFaultException(exceptionText);
                    } else if ("SOAPFaultExceptionWDetail".equals(exceptionType)) {
                        throw createSOAPFaultExceptionWithDetail(exceptionText);
                    }
                }

            }

        } catch (SOAPException e) {
            e.printStackTrace();
        }

        return ret;
    }

    private SOAPFaultException createSOAPFaultException(String faultString) throws SOAPException {
        SOAPFault fault = SOAPFactory.newInstance().createFault();
        fault.setFaultString(faultString);
        SAAJUtils.setFaultCode(fault, new QName("http://cxf.apache.org/faultcode", "Server"));
        return new SOAPFaultException(fault);
    }
    private SOAPFaultException createSOAPFaultExceptionWithDetail(String faultString)
        throws SOAPException {

        SOAPFault fault = SOAPFactory.newInstance().createFault();

        QName faultName = new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE,
                        "Server");
        SAAJUtils.setFaultCode(fault, faultName);
        fault.setFaultActor("http://gizmos.com/orders");
        fault.setFaultString(faultString);

        Detail detail = fault.addDetail();

        QName entryName = new QName("http://gizmos.com/orders/",
                        "order", "PO");
        DetailEntry entry = detail.addDetailEntry(entryName);
        entry.addTextNode("Quantity element does not have a value");

        QName entryName2 = new QName("http://gizmos.com/orders/",
                        "order", "PO");
        DetailEntry entry2 = detail.addDetailEntry(entryName2);
        entry2.addTextNode("Incomplete address: no zip code");

        return new SOAPFaultException(fault);
    }

    public String toString() {
        return getHandlerId();
    }

    private boolean isValidWsdlDescription(Object wsdlDescription) {
        return (wsdlDescription != null)
               && (wsdlDescription instanceof java.net.URI || wsdlDescription instanceof java.net.URL);
    }
}

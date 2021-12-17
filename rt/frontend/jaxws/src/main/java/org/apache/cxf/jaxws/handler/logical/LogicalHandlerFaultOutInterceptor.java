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

package org.apache.cxf.jaxws.handler.logical;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Binding;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.jaxws.handler.AbstractJAXWSHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;


public class LogicalHandlerFaultOutInterceptor
    extends AbstractJAXWSHandlerInterceptor<Message> {

    public static final String ORIGINAL_WRITER = "original_writer";

    LogicalHandlerFaultOutEndingInterceptor ending;

    public LogicalHandlerFaultOutInterceptor(Binding binding) {
        super(binding, Phase.PRE_MARSHAL);
        ending = new LogicalHandlerFaultOutEndingInterceptor(binding);
    }

    public void handleMessage(Message message) throws Fault {
        if (binding.getHandlerChain().isEmpty()) {
            return;
        }
        HandlerChainInvoker invoker = getInvoker(message);
        if (invoker.getLogicalHandlers().isEmpty()) {
            return;
        }

        XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
        Document doc = DOMUtils.newDocument();
        message.setContent(Node.class, doc);
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter(doc);
        // set up the namespace context
        try {
            writer.setNamespaceContext(origWriter.getNamespaceContext());
        } catch (XMLStreamException ex) {
            // don't set the namespaceContext
        }
        // Replace stax writer with DomStreamWriter
        message.setContent(XMLStreamWriter.class, writer);
        message.put(ORIGINAL_WRITER, origWriter);

        message.getInterceptorChain().add(ending);
    }


    private static class LogicalHandlerFaultOutEndingInterceptor
        extends AbstractJAXWSHandlerInterceptor<Message> {

        LogicalHandlerFaultOutEndingInterceptor(Binding binding) {
            super(binding, Phase.POST_MARSHAL);
        }

        public void handleMessage(Message message) throws Fault {
            W3CDOMStreamWriter domWriter = (W3CDOMStreamWriter)message.getContent(XMLStreamWriter.class);
            XMLStreamWriter origWriter = (XMLStreamWriter)message
                .get(LogicalHandlerFaultOutInterceptor.ORIGINAL_WRITER);

            HandlerChainInvoker invoker = getInvoker(message);
            LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
            invoker.setLogicalMessageContext(lctx);
            boolean requestor = isRequestor(message);

            XMLStreamReader reader = (XMLStreamReader)message.get("LogicalHandlerInterceptor.INREADER");
            SOAPMessage origMessage = null;
            if (reader != null) {
                origMessage = message.getContent(SOAPMessage.class);
                message.setContent(XMLStreamReader.class, reader);
                message.removeContent(SOAPMessage.class);
            } else if (domWriter.getDocument().getDocumentElement() != null) {
                Source source = new DOMSource(domWriter.getDocument());
                message.setContent(Source.class, source);
                message.setContent(Node.class, domWriter.getDocument());
                message.setContent(XMLStreamReader.class,
                                   StaxUtils.createXMLStreamReader(domWriter.getDocument()));
            }

            try {
                if (!invoker.invokeLogicalHandlersHandleFault(requestor, lctx)) {
                    // handleAbort(message, context);
                }
            } catch (RuntimeException exception) {
                Exchange exchange = message.getExchange();

                Exception ex = new Fault(exception);

                FaultMode mode = message.get(FaultMode.class);

                Message faultMessage = exchange.getOutMessage();
                if (null == faultMessage) {
                    faultMessage = new MessageImpl();
                    faultMessage.setExchange(message.getExchange());
                    faultMessage = exchange.getEndpoint().getBinding().createMessage(faultMessage);
                }
                faultMessage.setContent(Exception.class, ex);
                if (null != mode) {
                    faultMessage.put(FaultMode.class, mode);
                }
                exchange.setOutMessage(null);
                exchange.setOutFaultMessage(faultMessage);

                InterceptorChain ic = message.getInterceptorChain();
                ic.reset();

                onCompletion(message);

                faultMessage.setInterceptorChain(ic);
                ic.doIntercept(faultMessage);

                return;
            }

            if (origMessage != null) {
                message.setContent(SOAPMessage.class, origMessage);
            }

            try {
                reader = message.getContent(XMLStreamReader.class);
                message.removeContent(XMLStreamReader.class);
                if (reader != null) {
                    StaxUtils.copy(reader, origWriter);
                } else if (domWriter.getDocument().getDocumentElement() != null) {
                    StaxUtils.copy(domWriter.getDocument(), origWriter);
                }
                message.setContent(XMLStreamWriter.class, origWriter);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
    }

}

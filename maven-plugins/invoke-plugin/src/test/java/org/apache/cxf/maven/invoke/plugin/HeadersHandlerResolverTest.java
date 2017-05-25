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
package org.apache.cxf.maven.invoke.plugin;

import java.util.List;

import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Rule;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HeadersHandlerResolverTest extends EasyMockSupport {

    private static final Node ANY_NODE = null;

    private static final PortInfo NOT_USED = null;

    @Rule
    public EasyMockRule mocks = new EasyMockRule(this);

    @Mock
    private SOAPMessageContext context;

    @Mock
    private SOAPEnvelope envelope;

    @Mock
    private SOAPHeader header;

    @Mock
    private Node importedHeader1;

    @Mock
    private Node importedHeader2;

    @Mock
    private SOAPMessage message;

    @Mock
    private SOAPPart part;

    @Mock
    private Document soapDocument;

    @Test
    public void shouldAddHeadersToSoapMessage() throws SOAPException {
        final Document document = XmlUtil.document();

        final Element header1 = document.createElement("header1");
        final Element header2 = document.createElement("header2");

        expect(context.getMessage()).andReturn(message);
        expect(message.getSOAPPart()).andReturn(part);
        expect(part.getEnvelope()).andReturn(envelope);
        expect(envelope.getHeader()).andReturn(null);
        expect(envelope.addHeader()).andReturn(header);
        expect(header.getOwnerDocument()).andReturn(soapDocument);
        expect(soapDocument.importNode(header1, true)).andReturn(importedHeader1);
        expect(soapDocument.importNode(header2, true)).andReturn(importedHeader2);
        expect(header.appendChild(importedHeader1)).andReturn(ANY_NODE);
        expect(header.appendChild(importedHeader2)).andReturn(ANY_NODE);

        replayAll();

        final Node[] headers = new Node[] {header1, header2};

        final HeadersHandlerResolver resolver = new HeadersHandlerResolver(headers);

        @SuppressWarnings("rawtypes")
        final List<Handler> handlerChain = resolver.getHandlerChain(NOT_USED);

        assertThat("Handler should be registered", handlerChain, hasSize(1));

        @SuppressWarnings("rawtypes")
        final Handler handler = handlerChain.get(0);

        @SuppressWarnings("unchecked")
        final boolean proceed = handler.handleMessage(context);

        assertTrue("Should proceed with execution", proceed);

        verifyAll();
    }
}

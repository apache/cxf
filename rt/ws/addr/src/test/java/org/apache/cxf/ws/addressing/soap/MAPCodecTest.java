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

package org.apache.cxf.ws.addressing.soap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.headers.Header;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextJAXBUtils;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200403;
import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.apache.cxf.ws.addressing.v200408.Relationship;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cxf.message.Message.MIME_HEADERS;
import static org.apache.cxf.message.Message.REQUESTOR_ROLE;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MAPCodecTest {

    private MAPCodec codec;
    private QName[] expectedNames;
    private Object[] expectedValues;
    private String expectedNamespaceURI;
    private Map<String, List<String>> mimeHeaders;
    private Exchange correlatedExchange;
    private boolean expectRelatesTo;
    private String nonReplyRelationship;
    private boolean expectFaultTo;

    @Before
    public void setUp() {
        codec = new MAPCodec();
    }

    @After
    public void tearDown() throws Exception {
        expectedNames = null;
        expectedValues = null;
        expectedNamespaceURI = null;
        mimeHeaders = null;
        correlatedExchange = null;
        ContextJAXBUtils.setJAXBContext(null);
        nonReplyRelationship = null;
    }

    @Test
    public void testGetHeaders() throws Exception {
        Set<QName> headers = codec.getUnderstoodHeaders();
        assertTrue("expected From header", headers.contains(Names.WSA_FROM_QNAME));
        assertTrue("expected To header", headers.contains(Names.WSA_TO_QNAME));
        assertTrue("expected ReplyTo header", headers.contains(Names.WSA_REPLYTO_QNAME));
        assertTrue("expected FaultTo header", headers.contains(Names.WSA_FAULTTO_QNAME));
        assertTrue("expected Action header", headers.contains(Names.WSA_ACTION_QNAME));
        assertTrue("expected MessageID header", headers.contains(Names.WSA_MESSAGEID_QNAME));
    }

    @Test
    public void testRequestorInboundNonNative200403() throws Exception {
        String uri = Names200403.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(true, false, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, true, false, false);
    }

    @Test
    public void testResponderInboundNonNative200403() throws Exception {
        String uri = Names200403.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(false, false, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, false, false, false);
    }

    @Test
    public void testRequestorOutboundNonNative200403() throws Exception {
        String uri = Names200403.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(true, true, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testResponderOutboundNonNative200403() throws Exception {
        String uri = Names200403.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(false, true, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, false, true, false);
    }

    @Test
    public void testRequestorOutbound() throws Exception {
        SoapMessage message = setUpMessage(true, true);
        codec.handleMessage(message);
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundPreExistingSOAPAction() throws Exception {
        SoapMessage message = setUpMessage(true, true, false, true);
        codec.handleMessage(message);
        verifyAction();
        verifyMessage(message, true, true, true);
    }

    @Test
    public void testRequestorOutboundNonNative() throws Exception {
        String uri = Names200408.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(true, true, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, true, true, false);
    }

    @Test
    public void testResponderInbound() throws Exception {
        SoapMessage message = setUpMessage(false, false);
        codec.handleMessage(message);
        verifyMessage(message, false, false, true);
    }

    @Test
    public void testResponderOutbound() throws Exception {
        SoapMessage message = setUpMessage(false, true);
        codec.handleMessage(message);
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testResponderInboundWithRelatesTo() throws Exception {
        SoapMessage message = setUpMessage(false, false, false, false, Boolean.TRUE,
                                           Names.WSA_NAMESPACE_NAME);
        //empty the uncorrelatedExchanges in responder
        for (String key : codec.uncorrelatedExchanges.keySet()) {
            codec.uncorrelatedExchanges.remove(key);
        }
        codec.handleMessage(message);
        verifyMessage(message, false, false, false);
    }

    @Test
    public void testResponderInboundNonNative() throws Exception {
        String uri = Names200408.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(false, false, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, false, false, false);
    }

    @Test
    public void testResponderOutboundInvalidMAP() throws Exception {
        SoapMessage message = setUpMessage(false, true, true);
        try {
            codec.handleMessage(message);
            fail("expected SOAPFaultException on invalid MAP");
        } catch (SoapFault sfe) {
            assertEquals("unexpected fault string", "Duplicate Message ID urn:uuid:12345", sfe.getMessage());
        }
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testResponderOutboundPreExistingSOAPAction() throws Exception {
        SoapMessage message = setUpMessage(false, true, false, true);
        codec.handleMessage(message);
        verifyAction();
        verifyMessage(message, false, true, true);
    }

    @Test
    public void testResponderOutboundNonNative() throws Exception {
        String uri = Names200408.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(false, true, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, false, true, false);
    }

    @Test
    public void testRequestorInbound() throws Exception {
        SoapMessage message = setUpMessage(true, false);
        codec.handleMessage(message);
        verifyMessage(message, true, false, true);
    }

    @Test
    public void testRequestorInboundNonNative() throws Exception {
        String uri = Names200408.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(true, false, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, true, false, false);
    }

    @Test
    public void testRequestorInboundNonReply() throws Exception {
        nonReplyRelationship = "wsat:correlatedOneway";
        SoapMessage message = setUpMessage(true, false);
        codec.handleMessage(message);
        verifyMessage(message, true, false, true);
    }

    @Test
    public void testRequestorInboundNonNativeNonReply() throws Exception {
        nonReplyRelationship = "wsat:correlatedOneway";
        String uri = Names200408.WSA_NAMESPACE_NAME;
        SoapMessage message = setUpMessage(true, false, false, false, uri);
        codec.handleMessage(message);
        verifyMessage(message, true, false, false);
    }

    private SoapMessage setUpMessage(boolean requestor, boolean outbound) throws Exception {
        return setUpMessage(requestor, outbound, false);
    }

    private SoapMessage setUpMessage(boolean requestor, boolean outbound, boolean invalidMAP)
        throws Exception {
        return setUpMessage(requestor, outbound, invalidMAP, false);
    }

    private SoapMessage setUpMessage(boolean requestor, boolean outbound, boolean invalidMAP,
                                     boolean preExistingSOAPAction) throws Exception {
        return setUpMessage(requestor, outbound, invalidMAP, preExistingSOAPAction, Names.WSA_NAMESPACE_NAME);
    }

    private SoapMessage setUpMessage(boolean requestor, boolean outbound, boolean invalidMAP,
                                     boolean preExistingSOAPAction, String exposeAs) throws Exception {
        return setUpMessage(requestor, outbound, invalidMAP, preExistingSOAPAction, null, exposeAs);
    }

    private SoapMessage setUpMessage(boolean requestor, boolean outbound, boolean invalidMAP,
                                     boolean preExistingSOAPAction, Boolean generateRelatesTo,
                                     String exposeAs) throws Exception {
        SoapMessage message = new SoapMessage(new MessageImpl());
        setUpOutbound(message, outbound);
        expectRelatesTo = generateRelatesTo != null ? generateRelatesTo
            : (requestor && !outbound) || (!requestor && outbound);
        message.put(REQUESTOR_ROLE, Boolean.valueOf(requestor));
        String mapProperty = getMAPProperty(requestor, outbound);
        AddressingProperties maps = getMAPs(requestor, outbound, exposeAs);
        final Element header = mock(Element.class);
        codec.setHeaderFactory(new MAPCodec.HeaderFactory() {
            public Element getHeader(SoapVersion version) {
                return header;
            }
        });
        List<Header> headers = message.getHeaders();
        JAXBContext jaxbContext = mock(JAXBContext.class);
        ContextJAXBUtils.setJAXBContext(jaxbContext);
        Names200408.setJAXBContext(jaxbContext);
        Names200403.setJAXBContext(jaxbContext);
        if (outbound) {
            setUpEncode(requestor, message, header, maps, mapProperty, invalidMAP, preExistingSOAPAction);
        } else {
            setUpDecode(message, headers, maps, mapProperty, requestor);
        }
        return message;
    }

    private void setUpEncode(boolean requestor, SoapMessage message, Element header,
                             AddressingProperties maps, String mapProperty, boolean invalidMAP,
                             boolean preExistingSOAPAction) throws Exception {
        message.put(mapProperty, maps);


        mimeHeaders = new HashMap<>();
        message.put(MIME_HEADERS, mimeHeaders);
        if (preExistingSOAPAction) {
            List<String> soapAction = new ArrayList<>();
            soapAction.add("\"foobar\"");
            mimeHeaders.put(SoapBindingConstants.SOAP_ACTION, soapAction);
        }
        if (invalidMAP) {
            message.put("org.apache.cxf.ws.addressing.map.fault.name", Names.DUPLICATE_MESSAGE_ID_NAME);
            message.put("org.apache.cxf.ws.addressing.map.fault.reason",
                        "Duplicate Message ID urn:uuid:12345");
        }
    }

    private void setUpDecode(SoapMessage message, List<Header> headers, AddressingProperties maps,
                             String mapProperty, boolean requestor) throws Exception {
        Unmarshaller unmarshaller = mock(Unmarshaller.class);
        when(ContextJAXBUtils.getJAXBContext().createUnmarshaller()).thenReturn(unmarshaller);
        String uri = maps.getNamespaceURI();
        boolean exposedAsNative = Names.WSA_NAMESPACE_NAME.equals(uri);
        boolean exposedAs200408 = Names200408.WSA_NAMESPACE_NAME.equals(uri);
        boolean exposedAs200403 = Names200403.WSA_NAMESPACE_NAME.equals(uri);
        assertTrue("unexpected namescape URI: " + uri, exposedAsNative || exposedAs200408 || exposedAs200403);
        setUpHeaderDecode(headers, uri, Names.WSA_ACTION_NAME, exposedAsNative
            ? AttributedURIType.class : exposedAs200408 ? AttributedURI.class : exposedAs200403
                ? org.apache.cxf.ws.addressing.v200403.AttributedURI.class : null, 0, unmarshaller);
        setUpHeaderDecode(headers, uri, Names.WSA_MESSAGEID_NAME, exposedAsNative
            ? AttributedURIType.class : exposedAs200408 ? AttributedURI.class : exposedAs200403
                ? org.apache.cxf.ws.addressing.v200403.AttributedURI.class : null, 1, unmarshaller);
        setUpHeaderDecode(headers, uri, Names.WSA_TO_NAME, exposedAsNative
            ? AttributedURIType.class : exposedAs200408 ? AttributedURI.class : exposedAs200403
                ? org.apache.cxf.ws.addressing.v200403.AttributedURI.class : null, 2, unmarshaller);
        setUpHeaderDecode(headers, uri, Names.WSA_REPLYTO_NAME, exposedAsNative
            ? EndpointReferenceType.class : exposedAs200408
                ? Names200408.EPR_TYPE : exposedAs200403
                    ? Names200403.EPR_TYPE : null, 3, unmarshaller);
        setUpHeaderDecode(headers, uri, Names.WSA_RELATESTO_NAME, exposedAsNative
            ? RelatesToType.class : exposedAs200408 ? Relationship.class : exposedAs200403
                ? org.apache.cxf.ws.addressing.v200403.Relationship.class : null, 4, unmarshaller);
        setUpHeaderDecode(headers, uri, Names.WSA_FAULTTO_NAME, exposedAsNative
            ? EndpointReferenceType.class : exposedAs200408
                ? Names200408.EPR_TYPE : exposedAs200403
                    ? Names200403.EPR_TYPE : null, 5, unmarshaller);
        setUpHeaderDecode(headers, uri, Names.WSA_FROM_NAME, exposedAsNative
            ? EndpointReferenceType.class : exposedAs200408
                ? Names200408.EPR_TYPE : exposedAs200403
                    ? Names200403.EPR_TYPE : null, 6, unmarshaller);
    }

    private <T> void setUpHeaderDecode(List<Header> headers, String uri, String name, Class<?> clz,
                                       int index, Unmarshaller unmarshaller) throws Exception {
        Element headerElement = mock(Element.class);
        headers.add(new Header(new QName(uri, name), headerElement));
        when(headerElement.getNamespaceURI()).thenReturn(uri);
        when(headerElement.getLocalName()).thenReturn(name);
        Object v = expectedValues[index];
        @SuppressWarnings("unchecked")
        JAXBElement<?> jaxbElement = new JAXBElement<>(new QName(uri, name), (Class<Object>)clz, clz.cast(v));
        when(unmarshaller.unmarshal(headerElement, clz)).thenAnswer(i -> jaxbElement);
    }

    private void setUpOutbound(Message message, boolean outbound) {
        Exchange exchange = new ExchangeImpl();
        exchange.setOutMessage(outbound ? message : new MessageImpl());
        message.setExchange(exchange);
    }

    private String getMAPProperty(boolean requestor, boolean outbound) {
        return outbound ? ADDRESSING_PROPERTIES_OUTBOUND : ADDRESSING_PROPERTIES_INBOUND;
    }

    private AddressingProperties getMAPs(boolean requestor, boolean outbound, String uri) {
        AddressingProperties maps = new AddressingProperties();
        boolean exposeAsNative = Names.WSA_NAMESPACE_NAME.equals(uri);
        boolean exposeAs200408 = Names200408.WSA_NAMESPACE_NAME.equals(uri);
        boolean exposeAs200403 = Names200403.WSA_NAMESPACE_NAME.equals(uri);

        AttributedURIType id = ContextUtils.getAttributedURI("urn:uuid:12345");
        maps.setMessageID(id);
        AttributedURIType to = ContextUtils.getAttributedURI("foobar");
        EndpointReferenceType toEpr = EndpointReferenceUtils.getEndpointReference(to);
        maps.setTo(toEpr);
        EndpointReferenceType replyTo = new EndpointReferenceType();
        String anonymous = exposeAsNative ? Names.WSA_ANONYMOUS_ADDRESS : exposeAs200408
            ? Names200408.WSA_ANONYMOUS_ADDRESS
            : Names200403.WSA_ANONYMOUS_ADDRESS;
        replyTo.setAddress(ContextUtils.getAttributedURI(anonymous));
        maps.setReplyTo(replyTo);
        EndpointReferenceType from = EndpointReferenceUtils.getEndpointReference("snafu");
        maps.setFrom(from);
        EndpointReferenceType faultTo = new EndpointReferenceType();
        anonymous = exposeAsNative ? Names.WSA_ANONYMOUS_ADDRESS : exposeAs200408
            ? Names200408.WSA_ANONYMOUS_ADDRESS
            : Names200403.WSA_ANONYMOUS_ADDRESS;
        faultTo.setAddress(ContextUtils.getAttributedURI(anonymous));
        maps.setFaultTo(faultTo);
        RelatesToType relatesTo = null;
        if (expectRelatesTo) {
            String correlationID = "urn:uuid:67890";
            relatesTo = new RelatesToType();
            relatesTo.setValue(correlationID);
            maps.setRelatesTo(relatesTo);
            if (nonReplyRelationship == null) {
                correlatedExchange = new ExchangeImpl();
                codec.uncorrelatedExchanges.put(correlationID, correlatedExchange);
            } else {
                relatesTo.setRelationshipType(nonReplyRelationship);
            }
        }
        AttributedURIType action = ContextUtils.getAttributedURI("http://foo/bar/SEI/opRequest");
        maps.setAction(action);
        maps.exposeAs(uri);
        expectedNamespaceURI = uri;

        expectedNames = new QName[] {
            new QName(uri, Names.WSA_ACTION_NAME),
            new QName(uri, Names.WSA_MESSAGEID_NAME),
            new QName(uri, Names.WSA_TO_NAME),
            new QName(uri, Names.WSA_REPLYTO_NAME),
            new QName(uri, Names.WSA_RELATESTO_NAME),
            new QName(uri, Names.WSA_FROM_NAME),
            new QName(uri, Names.WSA_FAULTTO_NAME),
        };
        if (exposeAsNative) {
            expectedValues = new Object[] {
                action, id, to, replyTo, relatesTo, from, faultTo
            };
        } else if (exposeAs200408) {
            expectedValues = new Object[] {
                org.apache.cxf.ws.addressing.VersionTransformer.convert(action),
                org.apache.cxf.ws.addressing.VersionTransformer.convert(id),
                org.apache.cxf.ws.addressing.VersionTransformer.convert(to),
                org.apache.cxf.ws.addressing.VersionTransformer.convert(replyTo),
                org.apache.cxf.ws.addressing.VersionTransformer.convert(relatesTo),
                org.apache.cxf.ws.addressing.VersionTransformer.convert(from),
                org.apache.cxf.ws.addressing.VersionTransformer.convert(faultTo),
            };
            if (!outbound) {
                // conversion from 2004/08 to 2005/08 anonymous address
                // occurs transparently in VersionTransformer
                Names200408.EPR_TYPE.cast(expectedValues[3]).getAddress()
                    .setValue(Names.WSA_ANONYMOUS_ADDRESS);
                Names200408.EPR_TYPE.cast(expectedValues[5]).getAddress()
                    .setValue(Names.WSA_ANONYMOUS_ADDRESS);
            }
        } else if (exposeAs200403) {
            expectedValues = new Object[] {
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(action),
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(id),
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(to),
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(replyTo),
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(relatesTo),
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(from),
                org.apache.cxf.ws.addressing.VersionTransformer.convertTo200403(faultTo),
            };
            if (!outbound) {
                // conversion from 2004/03 to 2005/08 anonymous address
                // occurs transparently in VersionTransformer
                Names200403.EPR_TYPE.cast(expectedValues[3]).getAddress()
                    .setValue(Names.WSA_ANONYMOUS_ADDRESS);
                Names200403.EPR_TYPE.cast(expectedValues[5]).getAddress()
                    .setValue(Names.WSA_ANONYMOUS_ADDRESS);
            }
        } else {
            fail("unexpected namespace URI: " + uri);
        }
        return maps;
    }

    private boolean verifyMAPs(Object obj) {
        if (obj instanceof AddressingProperties) {
            AddressingProperties other = (AddressingProperties)obj;
            return compareExpected(other);
        }
        return false;
    }

    private boolean compareExpected(AddressingProperties other) {
        boolean ret = false;
        String uri = other.getNamespaceURI();
        boolean exposedAsNative = Names.WSA_NAMESPACE_NAME.equals(uri);
        boolean exposedAs200408 = Names200408.WSA_NAMESPACE_NAME.equals(uri);
        boolean exposedAs200403 = Names200403.WSA_NAMESPACE_NAME.equals(uri);

        if (exposedAsNative || exposedAs200408 || exposedAs200403) {
            String expectedMessageID = exposedAsNative
                ? ((AttributedURIType)expectedValues[1]).getValue() : exposedAs200408
                    ? ((AttributedURI)expectedValues[1]).getValue()
                    : ((org.apache.cxf.ws.addressing.v200403.AttributedURI)expectedValues[1]).getValue();

            String expectedTo = exposedAsNative
                ? ((AttributedURIType)expectedValues[2]).getValue() : exposedAs200408
                    ? ((AttributedURI)expectedValues[2]).getValue()
                    : ((org.apache.cxf.ws.addressing.v200403.AttributedURI)expectedValues[2]).getValue();

            String expectedReplyTo = exposedAsNative ? ((EndpointReferenceType)expectedValues[3])
                .getAddress().getValue() : exposedAs200408 ? (Names200408.EPR_TYPE
                .cast(expectedValues[3])).getAddress().getValue() : (Names200403.EPR_TYPE
                .cast(expectedValues[3])).getAddress().getValue();
            String expectedAction = exposedAsNative
                ? ((AttributedURIType)expectedValues[0]).getValue() : exposedAs200408
                    ? ((AttributedURI)expectedValues[0]).getValue()
                    : ((org.apache.cxf.ws.addressing.v200403.AttributedURI)expectedValues[0]).getValue();

            ret = expectedMessageID.equals(other.getMessageID().getValue())
                  && expectedTo.equals(other.getTo().getValue())
                  && expectedReplyTo.equals(other.getReplyTo().getAddress().getValue())
                  && expectedAction.equals(other.getAction().getValue())
                  && expectedNamespaceURI.equals(other.getNamespaceURI());
            if (expectRelatesTo) {
                String expectedRelatesTo = exposedAsNative
                    ? ((RelatesToType)expectedValues[4]).getValue() : exposedAs200408
                        ? ((Relationship)expectedValues[4]).getValue()
                        : ((org.apache.cxf.ws.addressing.v200403.Relationship)expectedValues[4]).getValue();
                ret = ret && expectedRelatesTo.equals(other.getRelatesTo().getValue());
            }
        }
        return ret;
    }

    private void verifyAction() {
        List<?> soapAction = mimeHeaders.get("SOAPAction");
        assertNotNull("expected propogated action", soapAction);
        assertEquals("expected single action", 1, soapAction.size());
        String expectedAction = "\"" + ((AttributedURIType)expectedValues[0]).getValue() + "\"";
        assertEquals("expected propogated action", expectedAction, soapAction.get(0));
    }

    private void verifyMessage(SoapMessage message, boolean requestor, boolean outbound,
                               boolean exposedAsNative) {
        if (requestor) {
            if (outbound) {
                String id = expectedValues[1] instanceof AttributedURIType
                    ? ((AttributedURIType)expectedValues[1]).getValue()
                    : expectedValues[0] instanceof AttributedURI
                      ? ((AttributedURI)expectedValues[1]).getValue()
                      : ((org.apache.cxf.ws.addressing.v200403.AttributedURI)expectedValues[1]).getValue();
                assertSame("unexpected correlated exchange",
                           codec.uncorrelatedExchanges.get(id),
                           message.getExchange());
            } else {
                if (isReply(exposedAsNative)) {
                    assertSame("unexpected correlated exchange",
                               correlatedExchange,
                               message.getExchange());
                } else {
                    assertNotSame("unexpected correlated exchange",
                                  correlatedExchange,
                                  message.getExchange());
                }
                assertEquals("expected empty uncorrelated exchange cache",
                             0,
                             codec.uncorrelatedExchanges.size());
            }
        }
        if (outbound) {
            int expectedMarshals = requestor ? expectedValues.length - 1 : expectedValues.length;
            if (!expectFaultTo) {
                --expectedMarshals;
            }
            List<Header> headers = message.getHeaders();
            assertTrue("expected holders added to header list", headers.size() >= expectedMarshals);
            for (int i = 0; i < (expectFaultTo ? expectedValues.length : expectedValues.length - 1); i++) {
                if (i == 4 && !expectRelatesTo) {
                    i++;
                }
                assertTrue("expected " + expectedNames[i] + " added to headers", message
                    .hasHeader(expectedNames[i]));
            }
        }
        assertTrue("unexpected MAPs", verifyMAPs(message.get(getMAPProperty(requestor, outbound))));
    }

    private boolean isReply(boolean exposedAsNative) {
        final boolean isReply;
        if (exposedAsNative) {
            isReply =
                Names.WSA_RELATIONSHIP_REPLY.equals(
                    ((RelatesToType)expectedValues[4]).getRelationshipType());
        } else {
            QName relationship =
                expectedValues[4] instanceof Relationship
                ? ((Relationship)expectedValues[4]).getRelationshipType()
                : ((org.apache.cxf.ws.addressing.v200403.Relationship)expectedValues[4])
                      .getRelationshipType();
            isReply = relationship == null
                      || Names.WSA_REPLY_NAME.equalsIgnoreCase(relationship.getLocalPart());
        }

        return isReply;
    }
}
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

package org.apache.cxf.systest.ws.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.rm.RMConstants;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MessageFlow {

    private final String addressingNamespace;
    private final String rmNamespace;
    private List<byte[]> inStreams;
    private List<byte[]> outStreams;
    private final List<Document> outboundMessages = new ArrayList<>();
    private final List<Document> inboundMessages = new ArrayList<>();

    public MessageFlow(List<byte[]> out, List<byte[]> in, String addrns, String rmns) throws Exception {
        addressingNamespace = addrns;
        rmNamespace = rmns;
        reset(out, in);
    }

    public MessageFlow(List<byte[]> out, List<byte[]> in) throws Exception {
        this(out, in, Names.WSA_NAMESPACE_NAME, null);
    }

    public void clear() throws Exception {
        inStreams.clear();
        outStreams.clear();
    }

    public final void reset(List<byte[]> out, List<byte[]> in) throws Exception {
        for (int i = 0; i < inboundMessages.size(); i++) {
            in.remove(0);
        }
        inStreams = in;
        for (int i = 0; i < outboundMessages.size(); i++) {
            out.remove(0);
        }
        outStreams = out;
        inboundMessages.clear();
        for (byte[] bytes : inStreams) {
            inboundMessages.add(StaxUtils.read(new ByteArrayInputStream(bytes)));
        }
        outboundMessages.clear();
        for (byte[] bytes : outStreams) {
            outboundMessages.add(StaxUtils.read(new ByteArrayInputStream(bytes)));
        }
    }

    public Document getMessage(int i, boolean outbound) {
        return outbound ? outboundMessages.get(i) : inboundMessages.get(i);
    }

    public void verifyActions(String[] expectedActions, boolean outbound) throws Exception {

        assertEquals(expectedActions.length, outbound ? outboundMessages.size() : inboundMessages.size());

        for (int i = 0; i < expectedActions.length; i++) {
            Document doc = outbound ? outboundMessages.get(i) : inboundMessages.get(i);
            String action = getAction(doc);
            if (null == expectedActions[i]) {
                assertNull((outbound ? "Outbound " : "Inbound") + " message " + i + " has unexpected action: " + action,
                        action);
            } else {
                assertEquals((outbound ? "Outbound " : "Inbound") + " message " + i
                        + " does not contain expected action header", expectedActions[i], action);
            }
        }
    }

    public void verifyActionsIgnoringPartialResponses(String[] expectedActions) throws Exception {
        int j = 0;
        for (int i = 0; i < inboundMessages.size() && j < expectedActions.length; i++) {
            String action = getAction(inboundMessages.get(i));
            if (null == action && emptyBody(inboundMessages.get(i))) {
                continue;
            }
            if (null == expectedActions[j]) {
                assertNull("Inbound message " + i + " has unexpected action: " + action, action);
            } else {
                assertEquals("Inbound message " + i + " has unexpected action: ", expectedActions[j], action);
            }
            j++;
        }
        if (j < expectedActions.length) {
            fail("Inbound messages do not contain all expected actions.");
        }
    }

    public boolean checkActions(String[] expectedActions, boolean outbound) throws Exception {

        if (expectedActions.length != (outbound ? outboundMessages.size() : inboundMessages.size())) {
            return false;
        }

        for (int i = 0; i < expectedActions.length; i++) {
            String action = outbound ? getAction(outboundMessages.get(i)) : getAction(inboundMessages.get(i));
            if (null == expectedActions[i]) {
                if (action != null) {
                    return false;
                }
            } else {
                if (!expectedActions[i].equals(action)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void verifyAction(String expectedAction,
                             int expectedCount,
                             boolean outbound,
                             boolean exact) throws Exception {
        int messageCount = outbound ? outboundMessages.size() : inboundMessages.size();
        int count = 0;
        for (int i = 0; i < messageCount; i++) {
            String action = outbound ? getAction(outboundMessages.get(i)) : getAction(inboundMessages.get(i));
            if (null == expectedAction) {
                if (action == null) {
                    count++;
                }
            } else {
                if (expectedAction.equals(action)) {
                    count++;
                }
            }
        }
        if (exact) {
            assertEquals("unexpected count for action: " + expectedAction,
                         expectedCount,
                         count);
        } else {
            assertTrue("unexpected count for action: " + expectedAction + ": " + count,
                       expectedCount <= count);
        }

    }

    public void verifyMessageNumbers(String[] expectedMessageNumbers, boolean outbound) throws Exception {
        verifyMessageNumbers(expectedMessageNumbers, outbound, true);
    }

    public void verifyMessageNumbers(String[] expectedMessageNumbers,
                                     boolean outbound,
                                     boolean exact) throws Exception {

        int actualMessageCount =
            outbound ? outboundMessages.size() : inboundMessages.size();
        if (exact) {
            assertEquals(expectedMessageNumbers.length, actualMessageCount);
        } else {
            assertTrue(expectedMessageNumbers.length <= actualMessageCount);
        }

        if (exact) {
            for (int i = 0; i < expectedMessageNumbers.length; i++) {
                Document doc = outbound ? outboundMessages.get(i) : inboundMessages.get(i);
                Element e = getSequence(doc);
                if (null == expectedMessageNumbers[i]) {
                    assertNull((outbound ? "Outbound" : "Inbound") + " message " + i
                        + " contains unexpected message number ", e);
                } else {
                    assertEquals((outbound ? "Outbound" : "Inbound") + " message " + i
                        + " does not contain expected message number "
                                 + expectedMessageNumbers[i], expectedMessageNumbers[i],
                                 getMessageNumber(e));
                }
            }
        } else {
            boolean[] matches = new boolean[expectedMessageNumbers.length];
            for (int i = 0; i < actualMessageCount; i++) {
                String messageNumber = null;
                Element e = outbound ? getSequence(outboundMessages.get(i))
                    : getSequence(inboundMessages.get(i));
                messageNumber = null == e ? null : getMessageNumber(e);
                for (int j = 0; j < expectedMessageNumbers.length; j++) {
                    if (messageNumber == null) {
                        if (expectedMessageNumbers[j] == null && !matches[j]) {
                            matches[j] = true;
                            break;
                        }
                    } else {
                        if (messageNumber.equals(expectedMessageNumbers[j]) && !matches[j]) {
                            matches[j] = true;
                            break;
                        }
                    }
                }
            }
            for (int k = 0; k < expectedMessageNumbers.length; k++) {
                assertTrue("no match for message number: " + expectedMessageNumbers[k],
                           matches[k]);
            }
        }
    }

    public void verifyLastMessage(boolean[] expectedLastMessages,
                                  boolean outbound) throws Exception {
        verifyLastMessage(expectedLastMessages, outbound, true);
    }

    public void verifyLastMessage(boolean[] expectedLastMessages,
                                  boolean outbound,
                                  boolean exact) throws Exception {

        int actualMessageCount =
            outbound ? outboundMessages.size() : inboundMessages.size();
        if (exact) {
            assertEquals(expectedLastMessages.length, actualMessageCount);
        } else {
            assertTrue(expectedLastMessages.length <= actualMessageCount);
        }

        for (int i = 0; i < expectedLastMessages.length; i++) {
            boolean lastMessage;
            Element e = outbound ? getSequence(outboundMessages.get(i))
                : getSequence(inboundMessages.get(i));
            lastMessage = null != e && getLastMessage(e);
            assertEquals("Outbound message " + i
                         + (expectedLastMessages[i] ? " does not contain expected last message element."
                             : " contains last message element."),
                         expectedLastMessages[i], lastMessage);

        }
    }

    public void verifyAcknowledgements(boolean[] expectedAcks, boolean outbound) throws Exception {
        assertEquals(expectedAcks.length, outbound ? outboundMessages.size()
            : inboundMessages.size());

        for (int i = 0; i < expectedAcks.length; i++) {
            boolean ack = outbound ? (null != getAcknowledgment(outboundMessages.get(i)))
                : (null != getAcknowledgment(inboundMessages.get(i)));

            if (expectedAcks[i]) {
                assertTrue((outbound ? "Outbound" : "Inbound") + " message " + i
                           + " does not contain expected acknowledgement", ack);
            } else {
                assertFalse((outbound ? "Outbound" : "Inbound") + " message " + i
                           + " contains unexpected acknowledgement", ack);
            }
        }
    }

    public void verifyAcknowledgements(int expectedAcks,
                                       boolean outbound,
                                       boolean exact) throws Exception {

        int actualMessageCount =
            outbound ? outboundMessages.size() : inboundMessages.size();
        int ackCount = 0;
        for (int i = 0; i < actualMessageCount; i++) {
            boolean ack = outbound ? (null != getAcknowledgment(outboundMessages.get(i)))
                : (null != getAcknowledgment(inboundMessages.get(i)));
            if (ack) {
                ackCount++;
            }
        }
        if (exact) {
            assertEquals("unexpected number of acks", expectedAcks, ackCount);
        } else {
            assertTrue("unexpected number of acks: " + ackCount,
                       expectedAcks <= ackCount);
        }
    }


    public void verifyAckRequestedOutbound(boolean outbound) throws Exception {
        boolean found = false;
        List<Document> messages = outbound ? outboundMessages : inboundMessages;
        for (Document d : messages) {
            Element se = getAckRequested(d);
            if (se != null) {
                found = true;
                break;
            }
        }
        assertTrue("expected AckRequested", found);
    }

    public void verifySequenceFault(QName code, boolean outbound, int index) throws Exception {
        Document d = outbound ? outboundMessages.get(index) : inboundMessages.get(index);
        assertNotNull(getRMHeaderElement(d, RMConstants.SEQUENCE_FAULT_NAME));
    }

    public void verifyHeader(QName name, boolean outbound, int index) throws Exception {
        Document d = outbound ? outboundMessages.get(index) : inboundMessages.get(index);
        assertNotNull((outbound ? "Outbound" : "Inbound")
            + " message " + index + " does not have " + name + "header.",
            getHeaderElement(d, name.getNamespaceURI(), name.getLocalPart()));
    }

    public void verifyNoHeader(QName name, boolean outbound, int index) throws Exception {
        Document d = outbound ? outboundMessages.get(index) : inboundMessages.get(index);
        assertNull((outbound ? "Outbound" : "Inbound")
            + " message " + index + " has " + name + "header.",
            getHeaderElement(d, name.getNamespaceURI(), name.getLocalPart()));
    }

    private String getAction(Document document) throws Exception {
        Element e = getHeaderElement(document, addressingNamespace, "Action");
        if (null != e) {
            return getText(e);
        }
        return null;
    }

    protected Element getSequence(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.SEQUENCE_NAME);
    }

    public String getMessageNumber(Element elem) throws Exception {
        for (Node nd = elem.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "MessageNumber".equals(nd.getLocalName())) {
                return getText(nd);
            }
        }
        return null;
    }

    private boolean getLastMessage(Element element) throws Exception {
        for (Node nd = element.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "LastMessage".equals(nd.getLocalName())) {
                return true;
            }
        }
        return false;
    }

    protected Element getAcknowledgment(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.SEQUENCE_ACK_NAME);
    }

    private Element getAckRequested(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.ACK_REQUESTED_NAME);
    }

    private Element getRMHeaderElement(Document document, String name) throws Exception {
        return getHeaderElement(document, rmNamespace, name);
    }

    private static Element getHeaderElement(Document document, String namespace, String localName)
        throws Exception {
        Element envelopeElement = document.getDocumentElement();
        Element headerElement = null;
        for (Node nd = envelopeElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "Header".equals(nd.getLocalName())) {
                headerElement = (Element)nd;
                break;
            }
        }
        if (null == headerElement) {
            return null;
        }
        for (Node nd = headerElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE != nd.getNodeType()) {
                continue;
            }
            Element element = (Element)nd;
            String ns = element.getNamespaceURI();
            String ln = element.getLocalName();
            if (namespace.equals(ns)
                && localName.equals(ln)) {
                return element;
            }
        }
        return null;
    }


    public void verifyMessages(int nExpected, boolean outbound) {
        verifyMessages(nExpected, outbound, true);
    }

    public void verifyMessages(int nExpected, boolean outbound, boolean exact) {
        if (outbound) {
            if (exact) {
                assertEquals("Unexpected number of outbound messages" + dump(outStreams),
                             nExpected, outboundMessages.size());
            } else {
                assertTrue("Unexpected number of outbound messages: " + dump(outStreams),
                           nExpected <= outboundMessages.size());
            }
        } else {
            if (exact) {
                assertEquals("Unexpected number of inbound messages" + dump(inStreams),
                             nExpected, inboundMessages.size());
            } else {
                assertTrue("Unexpected number of inbound messages: " + dump(inStreams),
                           nExpected <= inboundMessages.size());
            }
        }
    }

    public void verifyAcknowledgementRange(long lower, long upper) throws Exception {
        long currentLower = 0;
        long currentUpper = 0;
        // get the final ack range
        for (Document doc : inboundMessages) {
            Element e = getRMHeaderElement(doc, RMConstants.SEQUENCE_ACK_NAME);
            // let the newer messages take precedence over the older messages in getting the final range
            if (null != e) {
                e = getNamedElement(e, "AcknowledgementRange");
                if (null != e) {
                    currentLower = Long.parseLong(e.getAttribute("Lower"));
                    currentUpper = Long.parseLong(e.getAttribute("Upper"));
                }
            }
        }
        assertEquals("Unexpected acknowledgement lower range",
                     lower, currentLower);
        assertEquals("Unexpected acknowledgement upper range",
                     upper, currentUpper);
    }


    // note that this method picks the first match and returns
    public static Element getNamedElement(Element element, String lcname) throws Exception {
        for (Node nd = element.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && lcname.equals(nd.getLocalName())) {
                return (Element)nd;
            }
        }
        return null;
    }

    public void purgePartialResponses() throws Exception {
        for (int i = inboundMessages.size() - 1; i >= 0; i--) {
            if (isPartialResponse(inboundMessages.get(i))) {
                inboundMessages.remove(i);
            }
        }
    }

    public void purge() {
        inboundMessages.clear();
        outboundMessages.clear();
        inStreams.clear();
        outStreams.clear();
    }

    public void verifyPartialResponses(int nExpected) throws Exception {
        verifyPartialResponses(nExpected, null);
    }

    public void verifyPartialResponses(int nExpected, boolean[] piggybackedAcks) throws Exception {
        int npr = 0;
        for (int i = 0; i < inboundMessages.size(); i++) {
            if (isPartialResponse(inboundMessages.get(i))) {
                if (piggybackedAcks != null) {
                    Element ack = getAcknowledgment(inboundMessages.get(i));
                    if (piggybackedAcks[npr]) {
                        assertNotNull("Partial response " + npr + " does not include acknowledgement.", ack);
                    } else {
                        assertNull("Partial response " + npr + " has unexpected acknowledgement.", ack);
                    }
                }
                npr++;
            }
        }
        assertEquals("Inbound messages did not contain expected number of partial responses.",
                     nExpected, npr);
    }

    public boolean isPartialResponse(Document d) throws Exception {
        return null == getAction(d) && emptyBody(d);
    }

    public boolean emptyBody(Document d) throws Exception {
        Element envelopeElement = d.getDocumentElement();
        Element bodyElement = null;
        for (Node nd = envelopeElement.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && "Body".equals(nd.getLocalName())) {
                bodyElement = (Element)nd;
                break;
            }
        }
        return !(null != bodyElement && bodyElement.hasChildNodes());
    }

    static String dump(List<byte[]> streams) {
        StringBuilder buf = new StringBuilder();
        try {
            for (int i = 0; i < streams.size(); i++) {
                buf.append(System.getProperty("line.separator"));
                buf.append('[').append(i).append("] : ").append(new String(streams.get(i)));
            }
        } catch (Exception ex) {
            return ex.getMessage();
        }

        return buf.toString();
    }

    public static String getText(Node node) {
        for (Node nd = node.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.TEXT_NODE == nd.getNodeType()) {
                return nd.getNodeValue();
            }
        }
        return null;
    }

}

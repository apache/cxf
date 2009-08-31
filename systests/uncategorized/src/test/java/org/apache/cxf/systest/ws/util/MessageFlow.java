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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.Assert;

import org.apache.cxf.ws.rm.RMConstants;



public class MessageFlow extends Assert {
    
    private List<byte[]> inStreams;
    private List<byte[]> outStreams;
    private List<Document> outboundMessages;
    private List<Document> inboundMessages;
      
    public MessageFlow(List<byte[]> out, List<byte[]> in) throws Exception {
        inboundMessages = new ArrayList<Document>();
        outboundMessages = new ArrayList<Document>();
        reset(out, in);
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();
        inboundMessages.clear();
        for (int i = 0; i < inStreams.size(); i++) {
            byte[] bytes = inStreams.get(i);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            Document document = parser.parse(is);
            inboundMessages.add(document);
        }
        outboundMessages.clear();
        for (int i = 0; i < outStreams.size(); i++) {
            byte[] bytes = outStreams.get(i);
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            Document document = parser.parse(is);
            outboundMessages.add(document);
        }
    }
    
    public void verifyActions(String[] expectedActions, boolean outbound) throws Exception {

        assertEquals(expectedActions.length, outbound ? outboundMessages.size() : inboundMessages.size());

        for (int i = 0; i < expectedActions.length; i++) {
            String action = outbound ? getAction(outboundMessages.get(i)) : getAction(inboundMessages.get(i));
            if (null == expectedActions[i]) {
                assertNull((outbound ? "Outbound " : "Inbound") + " message " + i
                           + " has unexpected action: " + action, action);
            } else {
                assertEquals((outbound ? "Outbound " : "Inbound") + " message " + i
                             + " does not contain expected action header"
                             + System.getProperty("line.separator"), expectedActions[i], action);
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
                Element e = outbound ? getSequence(outboundMessages.get(i))
                    : getSequence(inboundMessages.get(i));
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
            lastMessage = null == e ? false : getLastMessage(e);
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
        assert null != getRMHeaderElement(d, RMConstants.getSequenceFaultName());
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
   
    protected String getAction(Document document) throws Exception {
        Element e = getHeaderElement(document, RMConstants.getAddressingNamespace(), "Action");
        if (null != e) {
            return getText(e);
        }
        return null;
    }

    protected Element getSequence(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.getSequenceName());
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
        return getRMHeaderElement(document, RMConstants.getSequenceAckName());
    }
    
    private Element getAckRequested(Document document) throws Exception {
        return getRMHeaderElement(document, RMConstants.getAckRequestedName());
    }

    private Element getRMHeaderElement(Document document, String name) throws Exception {
        return getHeaderElement(document, RMConstants.getNamespace(),  name);
    }

    private Element getHeaderElement(Document document, String namespace, String localName)
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
                assertEquals("Unexpected number of outbound messages" + outboundDump(),
                             nExpected, outboundMessages.size());
            } else {
                assertTrue("Unexpected number of outbound messages: " + outboundDump(),
                           nExpected <= outboundMessages.size());
            }
        } else {
            if (exact) {
                assertEquals("Unexpected number of inbound messages", nExpected, inboundMessages.size());
            } else {
                assertTrue("Unexpected number of inbound messages: " + inboundMessages.size(),
                           nExpected <= inboundMessages.size());                
            }
        }
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
        for (int i =  0; i < inboundMessages.size(); i++) {
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
        if (null != bodyElement && bodyElement.hasChildNodes()) {
            return false;
        }
        return true;
    }
   
   
    private String outboundDump() {
        StringBuffer buf = new StringBuffer();
        try {
            buf.append(System.getProperty("line.separator"));
            for (int i = 0; i < outStreams.size(); i++) {
                buf.append("[");
                buf.append(i);
                buf.append("] : ");
                buf.append(new String(outStreams.get(i)));
                buf.append(System.getProperty("line.separator"));
            }
        } catch (Exception ex) {
            return "";
        }
        
        return buf.toString();
    }

    private String getText(Node node) {
        for (Node nd = node.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.TEXT_NODE == nd.getNodeType()) {
                return nd.getNodeValue();
            }
        }
        return null;
    }

    protected QName getNodeName(Node nd) {
        return new QName(nd.getNamespaceURI(), nd.getLocalName());
    }
    
}

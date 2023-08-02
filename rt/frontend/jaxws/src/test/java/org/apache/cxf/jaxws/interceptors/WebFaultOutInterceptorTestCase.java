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
package org.apache.cxf.jaxws.interceptors;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

import jakarta.xml.soap.Detail;
import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;

import org.junit.Assert;
import org.junit.Test;

/**
 * Let WebFaultOutInterceptor process a message containing a SoapFault.
 *
 * If SoapFault's exception or its' cause is instance of SOAPFaultException, values of SOAPFaultException.subCodes
 * should be copied to SoapFault.subCodes.
 */
public class WebFaultOutInterceptorTestCase {

    private static final QName CODE = new QName("ns", "code");
    private static final QName SUBCODE = new QName("ns", "subcode");
    private static final List<QName> SUBCODES = Collections.singletonList(SUBCODE);

    private WebFaultOutInterceptor interceptor = new WebFaultOutInterceptor();


    @Test
    public void testSoapFaultException() {
        // create message that contains Fault that contains exception
        SOAPFaultException soapFaultException = new SOAPFaultException(new SOAPFaultStub());
        SoapFault soapFault = new SoapFault("message", soapFaultException, CODE);
        Message message = createMessage(soapFault);

        interceptor.handleMessage(message);




        Assert.assertNotNull(soapFault.getSubCodes());
        Assert.assertEquals(1, soapFault.getSubCodes().size());
        Assert.assertEquals(SUBCODE, soapFault.getSubCodes().get(0));
        Assert.assertEquals(CODE, soapFault.getFaultCode());
    }

    @Test
    public void testSoapFaultCause() {
        SOAPFaultException cause = new SOAPFaultException(new SOAPFaultStub());
        Exception exception = new Exception(cause);
        SoapFault soapFault = new SoapFault("message", exception, CODE);
        Message message = createMessage(soapFault);

        interceptor.handleMessage(message);

        Assert.assertNotNull(soapFault.getSubCodes());
        Assert.assertEquals(1, soapFault.getSubCodes().size());
        Assert.assertEquals(SUBCODE, soapFault.getSubCodes().get(0));
        Assert.assertEquals(CODE, soapFault.getFaultCode());
    }

    @Test
    public void testSoapFaultCauseCause() {
        SOAPFaultException cause = new SOAPFaultException(new SOAPFaultStub());
        Exception innerException = new Exception(cause);
        Exception outerException = new Exception(innerException);
        SoapFault soapFault = new SoapFault("message", outerException, CODE);
        Message message = createMessage(soapFault);

        interceptor.handleMessage(message);

        Assert.assertTrue("SoapFault.subCodes are expected to be empty.",
                soapFault.getSubCodes() == null || soapFault.getSubCodes().isEmpty());
        Assert.assertEquals(CODE, soapFault.getFaultCode());
    }

    @Test
    public void testOtherException() {
        Exception exception = new Exception("test");
        SoapFault soapFault = new SoapFault("message", exception, CODE);
        Message message = createMessage(soapFault);

        interceptor.handleMessage(message);

        Assert.assertTrue("SoapFault.subCodes are expected to be empty.",
                soapFault.getSubCodes() == null || soapFault.getSubCodes().isEmpty());
        Assert.assertEquals(CODE, soapFault.getFaultCode());
    }

    private Message createMessage(SoapFault soapFault) {
        Message message = new SoapMessage(Soap12.getInstance());
        message.setContent(Exception.class, soapFault);
        return message;
    }

    private final class SOAPFaultStub implements SOAPFault {

        @Override
        public void setFaultCode(Name name) throws SOAPException {

        }

        @Override
        public void setFaultCode(QName qName) throws SOAPException {

        }

        @Override
        public void setFaultCode(String s) throws SOAPException {

        }

        @Override
        public Name getFaultCodeAsName() {
            return null;
        }

        @Override
        public QName getFaultCodeAsQName() {
            return null;
        }

        @Override
        public Iterator<QName> getFaultSubcodes() {
            return SUBCODES.iterator();
        }

        @Override
        public void removeAllFaultSubcodes() {

        }

        @Override
        public void appendFaultSubcode(QName qName) throws SOAPException {

        }

        @Override
        public String getFaultCode() {
            return null;
        }

        @Override
        public void setFaultActor(String s) throws SOAPException {

        }

        @Override
        public String getFaultActor() {
            return null;
        }

        @Override
        public void setFaultString(String s) throws SOAPException {

        }

        @Override
        public void setFaultString(String s, Locale locale) throws SOAPException {

        }

        @Override
        public String getFaultString() {
            return null;
        }

        @Override
        public Locale getFaultStringLocale() {
            return null;
        }

        @Override
        public boolean hasDetail() {
            return false;
        }

        @Override
        public Detail getDetail() {
            return null;
        }

        @Override
        public Detail addDetail() throws SOAPException {
            return null;
        }

        @Override
        public Iterator<Locale> getFaultReasonLocales() throws SOAPException {
            return Collections.emptyIterator();
        }

        @Override
        public Iterator<String> getFaultReasonTexts() throws SOAPException {
            return null;
        }

        @Override
        public String getFaultReasonText(Locale locale) throws SOAPException {
            return null;
        }

        @Override
        public void addFaultReasonText(String s, Locale locale) throws SOAPException {

        }

        @Override
        public String getFaultNode() {
            return null;
        }

        @Override
        public void setFaultNode(String s) throws SOAPException {

        }

        @Override
        public String getFaultRole() {
            return null;
        }

        @Override
        public void setFaultRole(String s) throws SOAPException {

        }

        @Override
        public SOAPElement addChildElement(Name name) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addChildElement(QName qName) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addChildElement(String s) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addChildElement(String s, String s1) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addChildElement(String s, String s1, String s2) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addChildElement(SOAPElement soapElement) throws SOAPException {
            return null;
        }

        @Override
        public void removeContents() {

        }

        @Override
        public SOAPElement addTextNode(String s) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addAttribute(Name name, String s) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addAttribute(QName qName, String s) throws SOAPException {
            return null;
        }

        @Override
        public SOAPElement addNamespaceDeclaration(String s, String s1) throws SOAPException {
            return null;
        }

        @Override
        public String getAttributeValue(Name name) {
            return null;
        }

        @Override
        public String getAttributeValue(QName qName) {
            return null;
        }

        @Override
        public Iterator<Name> getAllAttributes() {
            return null;
        }

        @Override
        public Iterator<QName> getAllAttributesAsQNames() {
            return null;
        }

        @Override
        public String getNamespaceURI(String s) {
            return null;
        }

        @Override
        public Iterator<String> getNamespacePrefixes() {
            return null;
        }

        @Override
        public Iterator<String> getVisibleNamespacePrefixes() {
            return null;
        }

        @Override
        public QName createQName(String s, String s1) throws SOAPException {
            return null;
        }

        @Override
        public Name getElementName() {
            return null;
        }

        @Override
        public QName getElementQName() {
            return null;
        }

        @Override
        public SOAPElement setElementQName(QName qName) throws SOAPException {
            return null;
        }

        @Override
        public boolean removeAttribute(Name name) {
            return false;
        }

        @Override
        public boolean removeAttribute(QName qName) {
            return false;
        }

        @Override
        public boolean removeNamespaceDeclaration(String s) {
            return false;
        }

        @Override
        public Iterator<jakarta.xml.soap.Node> getChildElements() {
            return null;
        }

        @Override
        public Iterator<jakarta.xml.soap.Node> getChildElements(Name name) {
            return null;
        }

        @Override
        public Iterator<jakarta.xml.soap.Node> getChildElements(QName qName) {
            return null;
        }

        @Override
        public void setEncodingStyle(String s) throws SOAPException {

        }

        @Override
        public String getEncodingStyle() {
            return null;
        }

        @Override
        public String getTagName() {
            return null;
        }

        @Override
        public String getAttribute(String s) {
            return null;
        }

        @Override
        public void setAttribute(String s, String s1) throws DOMException {

        }

        @Override
        public void removeAttribute(String s) throws DOMException {

        }

        @Override
        public Attr getAttributeNode(String s) {
            return null;
        }

        @Override
        public Attr setAttributeNode(Attr attr) throws DOMException {
            return null;
        }

        @Override
        public Attr removeAttributeNode(Attr attr) throws DOMException {
            return null;
        }

        @Override
        public NodeList getElementsByTagName(String s) {
            return null;
        }

        @Override
        public String getAttributeNS(String s, String s1) throws DOMException {
            return null;
        }

        @Override
        public void setAttributeNS(String s, String s1, String s2) throws DOMException {

        }

        @Override
        public void removeAttributeNS(String s, String s1) throws DOMException {

        }

        @Override
        public Attr getAttributeNodeNS(String s, String s1) throws DOMException {
            return null;
        }

        @Override
        public Attr setAttributeNodeNS(Attr attr) throws DOMException {
            return null;
        }

        @Override
        public NodeList getElementsByTagNameNS(String s, String s1) throws DOMException {
            return null;
        }

        @Override
        public boolean hasAttribute(String s) {
            return false;
        }

        @Override
        public boolean hasAttributeNS(String s, String s1) throws DOMException {
            return false;
        }

        @Override
        public TypeInfo getSchemaTypeInfo() {
            return null;
        }

        @Override
        public void setIdAttribute(String s, boolean b) throws DOMException {

        }

        @Override
        public void setIdAttributeNS(String s, String s1, boolean b) throws DOMException {

        }

        @Override
        public void setIdAttributeNode(Attr attr, boolean b) throws DOMException {

        }

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public void setValue(String s) {

        }

        @Override
        public void setParentElement(SOAPElement soapElement) throws SOAPException {

        }

        @Override
        public SOAPElement getParentElement() {
            return null;
        }

        @Override
        public void detachNode() {

        }

        @Override
        public void recycleNode() {

        }

        @Override
        public String getNodeName() {
            return null;
        }

        @Override
        public String getNodeValue() throws DOMException {
            return null;
        }

        @Override
        public void setNodeValue(String s) throws DOMException {

        }

        @Override
        public short getNodeType() {
            return 0;
        }

        @Override
        public Node getParentNode() {
            return null;
        }

        @Override
        public NodeList getChildNodes() {
            return null;
        }

        @Override
        public Node getFirstChild() {
            return null;
        }

        @Override
        public Node getLastChild() {
            return null;
        }

        @Override
        public Node getPreviousSibling() {
            return null;
        }

        @Override
        public Node getNextSibling() {
            return null;
        }

        @Override
        public NamedNodeMap getAttributes() {
            return null;
        }

        @Override
        public Document getOwnerDocument() {
            return null;
        }

        @Override
        public Node insertBefore(Node node, Node node1) throws DOMException {
            return null;
        }

        @Override
        public Node replaceChild(Node node, Node node1) throws DOMException {
            return null;
        }

        @Override
        public Node removeChild(Node node) throws DOMException {
            return null;
        }

        @Override
        public Node appendChild(Node node) throws DOMException {
            return null;
        }

        @Override
        public boolean hasChildNodes() {
            return false;
        }

        @Override
        public Node cloneNode(boolean b) {
            return null;
        }

        @Override
        public void normalize() {

        }

        @Override
        public boolean isSupported(String s, String s1) {
            return false;
        }

        @Override
        public String getNamespaceURI() {
            return null;
        }

        @Override
        public String getPrefix() {
            return null;
        }

        @Override
        public void setPrefix(String s) throws DOMException {

        }

        @Override
        public String getLocalName() {
            return null;
        }

        @Override
        public boolean hasAttributes() {
            return false;
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public short compareDocumentPosition(Node node) throws DOMException {
            return 0;
        }

        @Override
        public String getTextContent() throws DOMException {
            return null;
        }

        @Override
        public void setTextContent(String s) throws DOMException {

        }

        @Override
        public boolean isSameNode(Node node) {
            return false;
        }

        @Override
        public String lookupPrefix(String s) {
            return null;
        }

        @Override
        public boolean isDefaultNamespace(String s) {
            return false;
        }

        @Override
        public String lookupNamespaceURI(String s) {
            return null;
        }

        @Override
        public boolean isEqualNode(Node node) {
            return false;
        }

        @Override
        public Object getFeature(String s, String s1) {
            return null;
        }

        @Override
        public Object setUserData(String s, Object o, UserDataHandler userDataHandler) {
            return null;
        }

        @Override
        public Object getUserData(String s) {
            return null;
        }
    }

}

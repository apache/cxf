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
package org.apache.cxf.ws.security.wss4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJStreamWriter;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.dom.util.WSSecurityUtil;


public abstract class AbstractSecurityTest extends AbstractCXFTest {
    public AbstractSecurityTest() {
        super();

        addNamespace("wsse", WSS4JConstants.WSSE_NS);
        addNamespace("wsse11", WSS4JConstants.WSSE11_NS);
        addNamespace("ds", WSS4JConstants.SIG_NS);
        addNamespace("s", Soap11.getInstance().getNamespace());
        addNamespace("xenc", WSS4JConstants.ENC_NS);
        addNamespace("wsu", WSS4JConstants.WSU_NS);
        addNamespace("saml1", WSS4JConstants.SAML_NS);
        addNamespace("saml2", WSS4JConstants.SAML2_NS);
    }

    /**
     * Reads a classpath resource into a Document.
     * @param name the name of the classpath resource
     */
    protected Document readDocument(String name) throws Exception,
        ParserConfigurationException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return StaxUtils.read(inStream);
    }

    /**
     * Creates a {@link SoapMessage} from the contents of a document.
     * @param doc the document containing the SOAP content.
     */
    protected SoapMessage getSoapMessageForDom(Document doc) throws Exception {
        return getSoapMessageForDom(doc, SOAPConstants.SOAP_1_1_PROTOCOL);
    }

    protected SoapMessage getSoapMessageForDom(Document doc, String protocol) throws Exception {
        SOAPMessage saajMsg = MessageFactory.newInstance(protocol).createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        SAAJStreamWriter writer = new SAAJStreamWriter(part);
        StaxUtils.copy(doc, writer);
        saajMsg.saveChanges();

        MessageImpl message = new MessageImpl();
        SoapMessage msg = new SoapMessage(message);
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);
        msg.setContent(SOAPMessage.class, saajMsg);

        return msg;
    }

    protected byte[] getMessageBytes(Document doc) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLStreamWriter byteArrayWriter = StaxUtils.createXMLStreamWriter(outputStream);
        StaxUtils.writeDocument(doc, byteArrayWriter, false);
        byteArrayWriter.flush();
        return outputStream.toByteArray();
    }

    protected SoapMessage makeInvocation(
        Map<String, Object> outProperties,
        List<String> xpaths,
        Map<String, Object> inProperties
    ) throws Exception {
        Document doc = readDocument("wsse-request-clean.xml");

        WSS4JOutInterceptor ohandler = new WSS4JOutInterceptor();
        PhaseInterceptor<SoapMessage> handler = ohandler.createEndingInterceptor();

        SoapMessage msg = getSoapMessageForDom(doc);

        for (String key : outProperties.keySet()) {
            msg.put(key, outProperties.get(key));
        }

        handler.handleMessage(msg);

        SOAPMessage saajMsg = msg.getContent(SOAPMessage.class);
        doc = saajMsg.getSOAPPart();

        for (String xpath : xpaths) {
            assertValid(xpath, doc);
        }

        byte[] docbytes = getMessageBytes(doc);
        doc = StaxUtils.read(new ByteArrayInputStream(docbytes));

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(inProperties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        Element securityHeaderElem = WSSecurityUtil.getSecurityHeader(doc, "");
        SoapHeader securityHeader = new SoapHeader(new QName(securityHeaderElem.getNamespaceURI(),
                                                             securityHeaderElem.getLocalName()), securityHeaderElem);
        inmsg.getHeaders().add(securityHeader);

        inHandler.handleMessage(inmsg);

        return inmsg;
    }

    @org.junit.AfterClass
    public static void cleanup() throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            File[] tmpFiles = new File(tmpDir).listFiles();
            if (tmpFiles != null) {
                for (File tmpFile : tmpFiles) {
                    if (tmpFile.exists() && (tmpFile.getName().startsWith("ws-security.nonce.cache.instance")
                            || tmpFile.getName().startsWith("ws-security.timestamp.cache.instance")
                            || tmpFile.getName().startsWith("ws-security.saml.cache.instance")
                            || tmpFile.getName().startsWith("wss4j-nonce-cache")
                            || tmpFile.getName().startsWith("wss4j-timestamp-cache"))) {
                        FileUtils.forceDeleteOnExit(tmpFile);
                    }
                }
            }
        }
    }

    protected int getJDKVersion() {
        try {
            return Integer.getInteger("java.specification.version", 0);
        } catch (NumberFormatException ex) {
            // ignore
        }
        return 0;
    }
}

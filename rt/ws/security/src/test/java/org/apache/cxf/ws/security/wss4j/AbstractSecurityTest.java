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
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJStreamWriter;
import org.apache.cxf.helpers.DOMUtils.NullResolver;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.wss4j.dom.WSConstants;


public abstract class AbstractSecurityTest extends AbstractCXFTest {
    public AbstractSecurityTest() {
        super();

        addNamespace("wsse", WSConstants.WSSE_NS);
        addNamespace("wsse11", WSConstants.WSSE11_NS);
        addNamespace("ds", WSConstants.SIG_NS);
        addNamespace("s", Soap11.getInstance().getNamespace());
        addNamespace("xenc", WSConstants.ENC_NS);
        addNamespace("wsu", WSConstants.WSU_NS);
        addNamespace("saml1", WSConstants.SAML_NS);
        addNamespace("saml2", WSConstants.SAML2_NS);
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
        SOAPMessage saajMsg = MessageFactory.newInstance().createMessage();
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
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new ByteArrayInputStream(docbytes));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new NullResolver());
        doc = StaxUtils.read(db, reader, false);

        WSS4JInInterceptor inHandler = new WSS4JInInterceptor(inProperties);

        SoapMessage inmsg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(inmsg);
        inmsg.setContent(SOAPMessage.class, saajMsg);

        
        inHandler.handleMessage(inmsg);

        return inmsg;
    }
    
    protected static boolean checkUnrestrictedPoliciesInstalled() {
        try {
            byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

            SecretKey key192 = new SecretKeySpec(
                new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17},
                            "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key192);
            c.doFinal(data);
            return true;
        } catch (Exception e) {
            //ignore
        }
        return false;
    }
    
    @org.junit.AfterClass
    public static void cleanup() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            File[] tmpFiles = new File(tmpDir).listFiles();
            if (tmpFiles != null) {
                for (File tmpFile : tmpFiles) {
                    if (tmpFile.exists() && (tmpFile.getName().startsWith("ws-security.nonce.cache.instance")
                            || tmpFile.getName().startsWith("wss4j-nonce-cache")
                            || tmpFile.getName().startsWith("ws-security.timestamp.cache.instance")
                            || tmpFile.getName().startsWith("wss4j-timestamp-cache"))) {
                        tmpFile.delete();
                    }
                }
            }
        }
    }
}

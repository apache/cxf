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

import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.ws.security.WSConstants;


public abstract class AbstractSecurityTest extends AbstractCXFTest {
    public AbstractSecurityTest() {
        super();

        addNamespace("wsse", WSConstants.WSSE_NS);
        addNamespace("wsse11", WSConstants.WSSE11_NS);
        addNamespace("ds", WSConstants.SIG_NS);
        addNamespace("s", Soap11.getInstance().getNamespace());
        addNamespace("xenc", WSConstants.ENC_NS);
        addNamespace("wsu", WSConstants.WSU_NS);
    }

    /**
     * Reads a classpath resource into a Document.
     * @param name the name of the classpath resource
     */
    protected Document readDocument(String name) throws SAXException, IOException,
        ParserConfigurationException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return DOMUtils.readXml(inStream);
    }

    /**
     * Reads a classpath resource into a SAAJ structure.
     * @param name the name of the classpath resource
     */
    protected SOAPMessage readSAAJDocument(String name) throws SAXException, IOException,
        ParserConfigurationException, SOAPException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return MessageFactory.newInstance().createMessage(null, inStream);
    }
    
    /**
     * Creates a {@link SoapMessage} from the contents of a document.
     * @param doc the document containing the SOAP content.
     */
    protected SoapMessage getSoapMessageForDom(Document doc) throws SOAPException {
        SOAPMessage saajMsg = MessageFactory.newInstance().createMessage();
        SOAPPart part = saajMsg.getSOAPPart();
        part.setContent(new DOMSource(doc));
        saajMsg.saveChanges();

        SoapMessage msg = new SoapMessage(new MessageImpl());
        Exchange ex = new ExchangeImpl();
        ex.setInMessage(msg);
        msg.setContent(SOAPMessage.class, saajMsg);
        return msg;
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
}

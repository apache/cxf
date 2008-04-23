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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.helpers.DOMUtils;
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

    protected Document readDocument(String name) throws SAXException, IOException,
        ParserConfigurationException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return DOMUtils.readXml(inStream);
    }
    

    protected SOAPMessage readSAAJDocument(String name) throws SAXException, IOException,
        ParserConfigurationException, SOAPException {
        InputStream inStream = getClass().getResourceAsStream(name);
        return MessageFactory.newInstance().createMessage(null, inStream);
    }
}

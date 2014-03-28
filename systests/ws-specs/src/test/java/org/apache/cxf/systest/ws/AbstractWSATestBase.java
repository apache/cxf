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

package org.apache.cxf.systest.ws;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

public class AbstractWSATestBase extends AbstractBusClientServerTestBase {

    protected ByteArrayOutputStream setupInLogging() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bos, true);
        LoggingInInterceptor in = new LoggingInInterceptor(writer);
        this.bus.getInInterceptors().add(in);
        return bos;
    }

    protected ByteArrayOutputStream setupOutLogging() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bos, true);

        LoggingOutInterceptor out = new LoggingOutInterceptor(writer);
        this.bus.getOutInterceptors().add(out);

        return bos;
    }
    
    
    protected String getLogValue(String log, String xpath) throws XMLStreamException {
        String s = log.substring(log.indexOf("Payload: ") + 9);
        Document doc = StaxUtils.read(new StringReader(s));
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("wsa", "http://www.w3.org/2005/08/addressing");
        ns.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        XPathUtils xpathu = new XPathUtils(ns);
        return xpathu.getValueString(xpath, doc.getDocumentElement());
    }
    protected void assertLogNotContains(String log, String xpath) throws XMLStreamException {
        String s = log.substring(log.indexOf("Payload: ") + 9);
        Document doc = StaxUtils.read(new StringReader(s));
        
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("wsa", "http://www.w3.org/2005/08/addressing");
        ns.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        XPathUtils xpathu = new XPathUtils(ns);
        assertNull(xpathu.getValueNode(xpath, doc.getDocumentElement()));
    }
    protected void assertLogContains(String log, String xpath, String value) throws XMLStreamException {
        assertEquals(value, getLogValue(log, xpath));
    }
}

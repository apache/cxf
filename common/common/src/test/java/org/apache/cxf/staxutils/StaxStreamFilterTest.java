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

package org.apache.cxf.staxutils;

import java.io.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.junit.Assert;
import org.junit.Test;

public class StaxStreamFilterTest extends Assert {
    public static final QName  SOAP_ENV = 
        new QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
    public static final QName  SOAP_BODY = 
        new QName("http://schemas.xmlsoap.org/soap/envelope/", "Body");

    @Test
    public void testFilter() throws Exception {
        StaxStreamFilter filter = new StaxStreamFilter(new QName[]{SOAP_ENV, SOAP_BODY});
        String soapMessage = "./resources/sayHiRpcLiteralReq.xml";
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        reader = StaxUtils.createFilteredReader(reader, filter);
        
        DepthXMLStreamReader dr = new DepthXMLStreamReader(reader);

        StaxUtils.toNextElement(dr);
        QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHi");
        
        assertEquals(sayHi, dr.getName());
    }

    @Test
    public void testFilterRPC() throws Exception {
        StaxStreamFilter filter = new StaxStreamFilter(new QName[]{SOAP_ENV, SOAP_BODY});
        String soapMessage = "./resources/greetMeRpcLitReq.xml";
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getTestStream(soapMessage));
        reader = StaxUtils.createFilteredReader(reader, filter);
        
        DepthXMLStreamReader dr = new DepthXMLStreamReader(reader);

        StaxUtils.toNextElement(dr);
        assertEquals(new QName("http://apache.org/hello_world_rpclit", "sendReceiveData"), dr.getName());

        StaxUtils.nextEvent(dr);
        StaxUtils.toNextElement(dr);
        assertEquals(new QName("", "in"), dr.getName());

        StaxUtils.nextEvent(dr);
        StaxUtils.toNextElement(dr);
        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "elem1"), dr.getName());

        StaxUtils.nextEvent(dr);
        StaxUtils.toNextText(dr);
        assertEquals("this is element 1", dr.getText());
        
        StaxUtils.toNextElement(dr);
        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "elem1"), dr.getName());
        assertEquals(XMLStreamConstants.END_ELEMENT, dr.getEventType());

        StaxUtils.nextEvent(dr);
        StaxUtils.toNextElement(dr);
        
        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "elem2"), dr.getName());
    } 

    private InputStream getTestStream(String file) {
        return getClass().getResourceAsStream(file);
    }
}

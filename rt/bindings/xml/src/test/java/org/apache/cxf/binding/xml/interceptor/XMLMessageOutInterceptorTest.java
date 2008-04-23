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

package org.apache.cxf.binding.xml.interceptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_xml_http.bare.types.MyComplexStructType;
import org.apache.hello_world_xml_http.wrapped.types.GreetMe;
import org.junit.Before;
import org.junit.Test;

public class XMLMessageOutInterceptorTest extends TestBase {

    static String bareNs = "http://apache.org/hello_world_xml_http/bare";

    static String wrapNs = "http://apache.org/hello_world_xml_http/wrapped";

    static String bareNsType = "http://apache.org/hello_world_xml_http/bare/types";

    static String wrapNsType = "http://apache.org/hello_world_xml_http/wrapped/types";

    OutputStream outputStream;

    XMLStreamWriter writer;

    XMLMessageOutInterceptor out = new XMLMessageOutInterceptor("phase1");

    MessageContentsList params = new MessageContentsList();

    QName bareMyComplexStructTypeQName = new QName(bareNs, "in");

    QName bareMyComplexStructQName = new QName(bareNsType, "myComplexStruct");

    QName bareRequestTypeQName = new QName(bareNsType, "requestType");

    QName wrapGreetMeQName = new QName(wrapNsType, "greetMe");

    QName wrapRequestTypeQName = new QName(wrapNsType, "requestType");

    @Before
    public void setUp() throws Exception {
        super.setUp();
        chain.add(out);
        prepareMessage(params);
    }
   
    @Test
    public void testBareOutSingle() throws Exception {

        MyComplexStructType myComplexStruct = new MyComplexStructType();
        myComplexStruct.setElem1("elem1");
        myComplexStruct.setElem2("elem2");
        myComplexStruct.setElem3(45);
        params.add(myComplexStruct);

        common("/wsdl/hello_world_xml_bare.wsdl", new QName(bareNs, "XMLPort"),
                        MyComplexStructType.class);

        BindingInfo bi = super.serviceInfo.getBinding(new QName(bareNs, "Greeter_XMLBinding"));
        BindingOperationInfo boi = bi.getOperation(new QName(bareNs, "sendReceiveData"));
        xmlMessage.getExchange().put(BindingOperationInfo.class, boi);

        out.handleMessage(xmlMessage);

        XMLStreamReader reader = getXMLReader();
        DepthXMLStreamReader dxr = new DepthXMLStreamReader(reader);
        StaxUtils.nextEvent(dxr);
        StaxUtils.toNextElement(dxr);
        
        assertEquals(bareMyComplexStructTypeQName.getLocalPart(), dxr.getLocalName());
        StaxUtils.toNextElement(dxr);
        StaxUtils.toNextText(dxr);
        assertEquals(myComplexStruct.getElem1(), dxr.getText());
    }

    @Test
    public void testBareOutMultiWithRoot() throws Exception {

        MyComplexStructType myComplexStruct = new MyComplexStructType();
        myComplexStruct.setElem1("elem1");
        myComplexStruct.setElem2("elem2");
        myComplexStruct.setElem3(45);
        params.add("tli");
        params.add(myComplexStruct);
        
        common("/wsdl/hello_world_xml_bare.wsdl", new QName(bareNs, "XMLPort"),
                        MyComplexStructType.class);

        BindingInfo bi = super.serviceInfo.getBinding(new QName(bareNs, "Greeter_XMLBinding"));
        BindingOperationInfo boi = bi.getOperation(new QName(bareNs, "testMultiParamPart"));
        xmlMessage.getExchange().put(BindingOperationInfo.class, boi);

        out.handleMessage(xmlMessage);

        XMLStreamReader reader = getXMLReader();
        DepthXMLStreamReader dxr = new DepthXMLStreamReader(reader);
        StaxUtils.nextEvent(dxr);
        StaxUtils.toNextElement(dxr);

        assertEquals(bareNs, dxr.getNamespaceURI());
        assertEquals("multiParamRootReq", dxr.getLocalName());
        StaxUtils.nextEvent(dxr);
        StaxUtils.toNextElement(dxr);
        
        assertEquals(bareRequestTypeQName, dxr.getName());
        StaxUtils.nextEvent(dxr);
        if (StaxUtils.toNextText(dxr)) {
            assertEquals("tli", dxr.getText());
        }
        
        boolean foundRequest = false;
        while (true) {
            StaxUtils.nextEvent(dxr);
            StaxUtils.toNextElement(dxr);
            QName requestType = new QName(dxr.getNamespaceURI(), dxr.getLocalName());
            if (requestType.equals(bareMyComplexStructQName)) {
                foundRequest = true;
                break;
            }
        }
        assertEquals("found request type", true, foundRequest);
    }

    @Test
    public void testWrapOut() throws Exception {
        GreetMe greetMe = new GreetMe();
        greetMe.setRequestType("tli");
        params.add(greetMe);
        common("/wsdl/hello_world_xml_wrapped.wsdl", new QName(wrapNs, "XMLPort"), GreetMe.class);

        BindingInfo bi = super.serviceInfo.getBinding(new QName(wrapNs, "Greeter_XMLBinding"));
        BindingOperationInfo boi = bi.getOperation(new QName(wrapNs, "greetMe"));
        xmlMessage.getExchange().put(BindingOperationInfo.class, boi);

        out.handleMessage(xmlMessage);

        XMLStreamReader reader = getXMLReader();
        DepthXMLStreamReader dxr = new DepthXMLStreamReader(reader);
        StaxUtils.nextEvent(dxr);
        StaxUtils.toNextElement(dxr);
        assertEquals(wrapGreetMeQName.getNamespaceURI(), dxr.getNamespaceURI());
        assertEquals(wrapGreetMeQName.getLocalPart(), dxr.getLocalName());
        StaxUtils.toNextElement(dxr);
        StaxUtils.toNextText(dxr);
        assertEquals(greetMe.getRequestType(), dxr.getText());
    }

    private void prepareMessage(List paramsList) throws Exception {
        outputStream = new ByteArrayOutputStream();
        // all test case here use input message to do test,
        // that means the out interceptor's role is Server-Out
        xmlMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        xmlMessage.setContent(OutputStream.class, outputStream);
        writer = StaxUtils.createXMLStreamWriter(outputStream);
        xmlMessage.setContent(XMLStreamWriter.class, writer);
        xmlMessage.setContent(List.class, paramsList);
    }

    private XMLStreamReader getXMLReader() throws Exception {
        ByteArrayOutputStream o = (ByteArrayOutputStream) xmlMessage.getContent(OutputStream.class);
        writer.flush();        
        InputStream in = new ByteArrayInputStream(o.toByteArray());
        return StaxUtils.createXMLStreamReader(in);
    }
}

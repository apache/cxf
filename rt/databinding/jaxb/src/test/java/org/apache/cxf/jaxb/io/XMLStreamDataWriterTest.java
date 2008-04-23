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

package org.apache.cxf.jaxb.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;
import org.apache.hello_world_rpclit.types.MyComplexStruct;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XMLStreamDataWriterTest extends Assert {

    private ByteArrayOutputStream baos;
    private XMLStreamWriter streamWriter;
    private XMLInputFactory inFactory;

    @Before
    public void setUp() throws Exception {
        baos =  new ByteArrayOutputStream();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        streamWriter = factory.createXMLStreamWriter(baos);
        assertNotNull(streamWriter);
        inFactory = XMLInputFactory.newInstance();
    }

    @After
    public void tearDown() throws Exception {
        baos.close();
    }

    @Test
    public void testWriteRPCLit1() throws Exception {
        JAXBDataBinding db = getTestWriterFactory();
        
        DataWriter<XMLStreamWriter> dw = db.createWriter(XMLStreamWriter.class);
        assertNotNull(dw);
        
        String val = new String("TESTOUTPUTMESSAGE");
        QName elName = new QName("http://apache.org/hello_world_rpclit/types", 
                                 "in");
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        dw.write(val, part, streamWriter);
        streamWriter.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = inFactory.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "in"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextText(reader);
        assertEquals("TESTOUTPUTMESSAGE", reader.getText());
    }

    @Test
    public void testWriteRPCLit2() throws Exception {
        JAXBDataBinding db = getTestWriterFactory(MyComplexStruct.class);
        
        DataWriter<XMLStreamWriter> dw = db.createWriter(XMLStreamWriter.class);
        assertNotNull(dw);
        
        MyComplexStruct val = new MyComplexStruct();
        val.setElem1("This is element 1");
        val.setElem2("This is element 2");
        val.setElem3(1);
        
        QName elName = new QName("http://apache.org/hello_world_rpclit/types", 
                                 "in");
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        
        dw.write(val, part, streamWriter);
        streamWriter.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = inFactory.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "in"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_rpclit/types", "elem1"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextText(reader);
        assertEquals("This is element 1", reader.getText());
    }

    @Test
    public void testWriteBare() throws Exception {
        JAXBDataBinding db = getTestWriterFactory(TradePriceData.class);
        
        DataWriter<XMLStreamWriter> dw = db.createWriter(XMLStreamWriter.class);
        assertNotNull(dw);
        
        TradePriceData val = new TradePriceData();
        val.setTickerSymbol("This is a symbol");
        val.setTickerPrice(1.0f);
        
        QName elName = new QName("http://apache.org/hello_world_doc_lit_bare/types", "inout");
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        dw.write(val, part, streamWriter);
        streamWriter.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = inFactory.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_doc_lit_bare/types", "inout"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_doc_lit_bare/types", "tickerSymbol"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextText(reader);
        assertEquals("This is a symbol", reader.getText());
    }
    
    @Test
    public void testWriteWrapper() throws Exception {
        JAXBDataBinding db = getTestWriterFactory(GreetMe.class);
        
        DataWriter<XMLStreamWriter> dw = db.createWriter(XMLStreamWriter.class);
        assertNotNull(dw);

        GreetMe val = new GreetMe();
        val.setRequestType("Hello");
        
        dw.write(val, streamWriter);
        streamWriter.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = inFactory.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "greetMe"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "requestType"),
                     reader.getName());
        
        StaxUtils.nextEvent(reader);
        StaxUtils.toNextText(reader);
        assertEquals("Hello", reader.getText());
    }

    @Test
    public void testWriteWrapperReturn() throws Exception {
        JAXBDataBinding db = getTestWriterFactory(GreetMeResponse.class);
        
        DataWriter<XMLStreamWriter> dw = db.createWriter(XMLStreamWriter.class);
        assertNotNull(dw);

        GreetMeResponse retVal = new GreetMeResponse();
        retVal.setResponseType("TESTOUTPUTMESSAGE");
        
        dw.write(retVal, streamWriter);
        streamWriter.flush();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        XMLStreamReader xr = inFactory.createXMLStreamReader(bais);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xr);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "greetMeResponse"),
                     reader.getName());

        StaxUtils.nextEvent(reader);
        StaxUtils.toNextElement(reader);
        assertEquals(new QName("http://apache.org/hello_world_soap_http/types", "responseType"),
                     reader.getName());
        
        StaxUtils.nextEvent(reader);
        StaxUtils.toNextText(reader);
        assertEquals("TESTOUTPUTMESSAGE", reader.getText());
    }

    private JAXBDataBinding getTestWriterFactory(Class... clz) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(clz);
        return new JAXBDataBinding(ctx);
    }
}

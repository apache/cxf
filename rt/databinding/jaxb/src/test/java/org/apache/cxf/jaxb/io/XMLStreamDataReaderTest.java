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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxStreamFilter;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;
import org.apache.hello_world_rpclit.types.MyComplexStruct;
import org.apache.hello_world_soap_http.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
 
public class XMLStreamDataReaderTest extends Assert {

    private XMLInputFactory factory;
    private XMLStreamReader reader;
    private InputStream is;
    
    @Before
    public void setUp() throws Exception {
        factory = XMLInputFactory.newInstance();
    }

    @After
    public void tearDown() throws IOException {
        is.close();
    }

    @Test
    public void testReadWrapper() throws Exception {
        JAXBDataBinding db = getDataBinding(GreetMe.class);
        
        reader = getTestReader("../resources/GreetMeDocLiteralReq.xml");
        assertNotNull(reader);
        
        DataReader<XMLStreamReader> dr = db.createReader(XMLStreamReader.class);
        assertNotNull(dr);
        Object val = dr.read(reader);
        assertNotNull(val);
        assertTrue(val instanceof GreetMe);
        assertEquals("TestSOAPInputPMessage", ((GreetMe)val).getRequestType());
    }

    @Test
    public void testReadWrapperReturn() throws Exception {
        JAXBDataBinding db = getDataBinding(GreetMeResponse.class);

        reader = getTestReader("../resources/GreetMeDocLiteralResp.xml");
        assertNotNull(reader);

        DataReader<XMLStreamReader> dr = db.createReader(XMLStreamReader.class);
        assertNotNull(dr);
        
        Object retValue = dr.read(reader);
        
        assertNotNull(retValue);
        assertTrue(retValue instanceof GreetMeResponse);
        assertEquals("TestSOAPOutputPMessage", ((GreetMeResponse)retValue).getResponseType());
    }

    @Test
    public void testReadRPC() throws Exception {
        JAXBDataBinding db = getDataBinding(MyComplexStruct.class);

        QName[] tags = {new QName("http://apache.org/hello_world_rpclit", "sendReceiveData")};

        reader = getTestReader("../resources/greetMeRpcLitReq.xml");
        assertNotNull(reader);
        
        XMLStreamReader localReader = getTestFilteredReader(reader, tags);

        DataReader<XMLStreamReader> dr = db.createReader(XMLStreamReader.class);
        assertNotNull(dr);
        Object val = dr.read(new QName("http://apache.org/hello_world_rpclit", "in"),
                             localReader,
                             MyComplexStruct.class);
        assertNotNull(val);

        assertTrue(val instanceof MyComplexStruct);
        assertEquals("this is element 1", ((MyComplexStruct)val).getElem1());
        assertEquals("this is element 2", ((MyComplexStruct)val).getElem2());
        assertEquals(42, ((MyComplexStruct)val).getElem3());
    }


    @Test
    public void testReadBare() throws Exception {
        JAXBDataBinding db = getDataBinding(TradePriceData.class);

        reader = getTestReader("../resources/sayHiDocLitBareReq.xml");
        assertNotNull(reader);
        
        DataReader<XMLStreamReader> dr = db.createReader(XMLStreamReader.class);
        assertNotNull(dr);
        QName elName = new QName("http://apache.org/hello_world_doc_lit_bare/types", "inout");
        MessagePartInfo part = new MessagePartInfo(elName, null);
        part.setElement(true);
        part.setElementQName(elName);
        part.setTypeClass(TradePriceData.class);
        Object val = dr.read(part, reader);

        assertNotNull(val);
        assertTrue(val instanceof TradePriceData);
        assertEquals("CXF", ((TradePriceData)val).getTickerSymbol());
        assertEquals(new Float(1.0f), new Float(((TradePriceData)val).getTickerPrice()));
    }

    private JAXBDataBinding getDataBinding(Class... clz) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(clz);
        return new JAXBDataBinding(ctx);
    }

    private XMLStreamReader getTestFilteredReader(XMLStreamReader r, QName[] q) throws Exception {
        StaxStreamFilter filter = new StaxStreamFilter(q);
        return factory.createFilteredReader(r, filter);
    }

    private XMLStreamReader getTestReader(String resource) throws Exception {
        is = getTestStream(resource);
        assertNotNull(is);
        return factory.createXMLStreamReader(is);
    }

    private InputStream getTestStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }
}

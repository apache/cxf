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
package org.apache.cxf.jaxrs.provider.json;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.resources.BookStore;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WadlGeneratorTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        control.makeThreadSafe(true);
    }
    
    @Test
    public void testWadlInJsonFormat() throws Exception {
        WadlGenerator wg = new WadlGenerator();
        wg.setUseJaxbContextForQnames(false);
        wg.setIgnoreMessageWriters(false);
        
        wg.setExternalLinks(Collections.singletonList("json.schema"));
        
        ClassResourceInfo cri = 
            ResourceUtils.createClassResourceInfo(BookStore.class, BookStore.class, true, true);
        Message m = mockMessage("http://localhost:8080/baz", "/bar", WadlGenerator.WADL_QUERY, null);
        Map<String, List<String>> headers = new HashMap<String, List<String>>();
        headers.put("Accept", Collections.singletonList("application/json"));
        m.put(Message.PROTOCOL_HEADERS, headers);
        Response r = wg.handleRequest(m, cri);
        assertEquals("application/json",
                r.getMetadata().getFirst("Content-Type").toString());
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        new JSONProvider<Document>().writeTo(
                (Document)r.getEntity(), Document.class, Document.class, 
                  new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, 
                  new MetadataMap<String, Object>(), os);
        String s = os.toString();
        String expected1 = 
            "{\"application\":{\"grammars\":{\"include\":{\"@href\":\"http://localhost:8080/baz"
            + "/json.schema\"}},\"resources\":{\"@base\":\"http://localhost:8080/baz\","
            + "\"resource\":{\"@path\":\"/bookstore/{id}\"";
        assertTrue(s.startsWith(expected1));
        String expected2 =
            "\"response\":{\"representation\":[{\"@mediaType\":\"application/xml\"},"
            + "{\"@element\":\"Chapter\",\"@mediaType\":\"application/json\"}]}";
        assertTrue(s.contains(expected2));
    }
    
    private Message mockMessage(String baseAddress, String pathInfo, String query,
                                List<ClassResourceInfo> cris) throws Exception {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        e.put(Service.class, new JAXRSServiceImpl(cris));
        m.setExchange(e);
        control.reset();
        ServletDestination d = control.createMock(ServletDestination.class);
        EndpointInfo epr = new EndpointInfo();
        epr.setAddress(baseAddress);
        d.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(epr).anyTimes();

        Endpoint endpoint = new EndpointImpl(null, null, epr);
        e.put(Endpoint.class, endpoint);

        e.setDestination(d);
        BindingInfo bi = control.createMock(BindingInfo.class);
        epr.setBinding(bi);
        bi.getProperties();
        EasyMock.expectLastCall().andReturn(Collections.emptyMap()).anyTimes();
        m.put(Message.REQUEST_URI, pathInfo);
        m.put(Message.QUERY_STRING, query);
        m.put(Message.HTTP_REQUEST_METHOD, "GET");
        control.replay();
        return m;
    }
}

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
package org.apache.cxf.jaxrs.provider.dom4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class DOM4JProviderTest extends Assert {

    
    @Test
    public void testReadXML() throws Exception {
        String str = readXML().asXML();
        // starts with the xml PI
        assertTrue(str.contains("<a/>") || str.contains("<a></a>"));
    }
    @Test
    public void testReadXMLWithBom() throws Exception {
        String str = readXMLBom().asXML();
        // starts with the xml PI
        assertTrue(str.contains("<a/>") || str.contains("<a></a>"));
    }
    private org.dom4j.Document readXML() throws Exception {
        return readXML(MediaType.APPLICATION_XML_TYPE, "<a/>");
    }
    private org.dom4j.Document readXMLBom() throws Exception {
        byte[] bom = new byte[]{(byte)239, (byte)187, (byte)191};
        assertEquals("efbbbf", StringUtils.toHexString(bom));
        byte[] strBytes = "<a/>".getBytes(StandardCharsets.UTF_8);
        InputStream is = new SequenceInputStream(new ByteArrayInputStream(bom),
                                                 new ByteArrayInputStream(strBytes));
        DOM4JProvider p = new DOM4JProvider();
        p.setProviders(new ProvidersImpl(createMessage(false)));
        org.dom4j.Document dom = p.readFrom(org.dom4j.Document.class, org.dom4j.Document.class, 
            new Annotation[] {}, MediaType.valueOf("text/xml;a=b"), 
            new MetadataMap<String, String>(),
            is);
        return dom;
    }
    private org.dom4j.Document readXML(MediaType ct, final String xml) throws Exception {
        DOM4JProvider p = new DOM4JProvider();
        p.setProviders(new ProvidersImpl(createMessage(false)));
        org.dom4j.Document dom = p.readFrom(org.dom4j.Document.class, org.dom4j.Document.class, 
            new Annotation[] {}, ct, new MetadataMap<String, String>(),
            new ByteArrayInputStream(xml.getBytes("UTF-8")));
        return dom;
    }
    
    @Test
    public void testReadJSONConvertToXML() throws Exception {
        final String xml = "{\"a\":{\"b\":2}}";
        DOM4JProvider p = new DOM4JProvider();
        p.setProviders(new ProvidersImpl(createMessage(false)));
        org.dom4j.Document dom = p.readFrom(org.dom4j.Document.class, org.dom4j.Document.class, 
                   new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, String>(),
                   new ByteArrayInputStream(xml.getBytes("UTF-8")));
        String str = dom.asXML();
        // starts with the xml PI
        assertTrue(str.contains("<a><b>2</b></a>"));
    }

    @Test
    public void testWriteXML() throws Exception {
        doTestWriteXML(MediaType.APPLICATION_XML_TYPE, false);
    }
    
    @Test
    public void testWriteXMLCustomCt() throws Exception {
        doTestWriteXML(MediaType.valueOf("application/custom+xml"), false);
    }
    
    @Test
    public void testWriteXMLAsDOMW3C() throws Exception {
        doTestWriteXML(MediaType.APPLICATION_XML_TYPE, true);
    }
    
    @Test
    public void testWriteXMLSuppressDeclaration() throws Exception {
        org.dom4j.Document dom = readXML(MediaType.APPLICATION_XML_TYPE, "<a/>");
        final Message message = createMessage(true);
        Providers providers = new ProvidersImpl(message);
        DOM4JProvider p = new DOM4JProvider() {
            protected Message getCurrentMessage() {
                return message;
            }
        };
        p.setProviders(providers);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(dom, org.dom4j.Document.class, org.dom4j.Document.class, 
            new Annotation[]{}, MediaType.APPLICATION_XML_TYPE, new MetadataMap<String, Object>(), bos);
        String str = bos.toString();
        assertFalse(str.startsWith("<?xml"));
        assertTrue(str.contains("<a/>") || str.contains("<a></a>"));
    }
    
    private void doTestWriteXML(MediaType ct, boolean convert) throws Exception {
        org.dom4j.Document dom = readXML(ct, "<a/>");
        final Message message = createMessage(false);
        Providers providers = new ProvidersImpl(message);
        DOM4JProvider p = new DOM4JProvider() {
            protected Message getCurrentMessage() {
                return message;
            }
        };
        p.setProviders(providers);
        p.convertToDOMAlways(convert);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(dom, org.dom4j.Document.class, org.dom4j.Document.class, 
            new Annotation[]{}, ct, new MetadataMap<String, Object>(), bos);
        String str = bos.toString();
        if (convert) {
            assertFalse(str.startsWith("<?xml"));
        } else {
            assertTrue(str.startsWith("<?xml"));
        }
        assertTrue(str.contains("<a/>") || str.contains("<a></a>"));
    }
    
    @Test
    public void testWriteJSON() throws Exception {
        org.dom4j.Document dom = readXML();
        DOM4JProvider p = new DOM4JProvider();
        p.setProviders(new ProvidersImpl(createMessage(false)));
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(dom, org.dom4j.Document.class, org.dom4j.Document.class, 
                   new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(),
                   bos);
        String str = bos.toString();
        assertEquals("{\"a\":\"\"}", str);
    }
    
    @Test
    public void testWriteJSONDropRoot() throws Exception {
        org.dom4j.Document dom = readXML(MediaType.APPLICATION_XML_TYPE, "<root><a/></root>");
        DOM4JProvider p = new DOM4JProvider();
        p.setProviders(new ProvidersImpl(createMessageWithJSONProvider()));
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(dom, org.dom4j.Document.class, org.dom4j.Document.class, 
                   new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(),
                   bos);
        String str = bos.toString();
        assertEquals("{\"a\":\"\"}", str);
    }
    
    @Test
    public void testWriteJSONAsArray() throws Exception {
        org.dom4j.Document dom = readXML(MediaType.APPLICATION_XML_TYPE, "<root><a>1</a></root>");
        DOM4JProvider p = new DOM4JProvider();
        
        ProviderFactory factory = ServerProviderFactory.getInstance();
        JSONProvider<Object> provider = new JSONProvider<Object>();
        provider.setSerializeAsArray(true);
        provider.setDropRootElement(true);
        provider.setDropElementsInXmlStream(false);
        provider.setIgnoreNamespaces(true);
        factory.registerUserProvider(provider);
        p.setProviders(new ProvidersImpl(createMessage(factory)));
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        p.writeTo(dom, org.dom4j.Document.class, org.dom4j.Document.class, 
                   new Annotation[]{}, MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(),
                   bos);
        String str = bos.toString();
        assertEquals("[{\"a\":1}]", str);
    }
    
    private Message createMessage(boolean suppress) {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        e.put(DOM4JProvider.SUPPRESS_XML_DECLARATION, suppress);
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.get(Application.class.getName());
        EasyMock.expectLastCall().andReturn(null);
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
    
    private Message createMessageWithJSONProvider() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        JSONProvider<Object> provider = new JSONProvider<Object>();
        provider.setDropRootElement(true);
        provider.setIgnoreNamespaces(true);
        factory.registerUserProvider(provider);
        return createMessage(factory);
    }
    private Message createMessage(ProviderFactory factory) {
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.get(Application.class.getName());
        EasyMock.expectLastCall().andReturn(null);
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.get(ServerProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }
            
    
}

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

package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.fortest.AegisTestBean;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.ManyTags;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.jaxrs.resources.Tags;

import org.junit.Assert;
import org.junit.Test;

public class AegisJSONProviderTest extends Assert {
    
    @Test
    public void testIsWriteable() {
        MessageBodyWriter<Object> p = new AegisJSONProvider();
        assertTrue(p.isWriteable(AegisTestBean.class, null, null, null));
    }
    
    @Test
    public void testIsReadable() {
        MessageBodyReader<Object> p = new AegisJSONProvider();
        assertFalse(p.isReadable(AegisTestBean.class, null, null, null));
    }
    
    
    @Test
    public void testReadFrom() throws Exception {
        doTestRead(true);
    }
    
    @Test
    public void testReadFromNoMap() throws Exception {
        doTestRead(false);
    }
    
    @SuppressWarnings("unchecked")
    private void doTestRead(boolean setNsMap) throws Exception {
        AegisJSONProvider p = new AegisJSONProvider();
        AbstractAegisProvider.clearContexts();
        if (setNsMap) {
            Map<String, String> namespaceMap = new HashMap<String, String>();
            namespaceMap.put("http://fortest.jaxrs.cxf.apache.org", "ns1");
            namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsins");
            p.setNamespaceMap(namespaceMap);
        }
        String data = "{\"ns1.AegisTestBean\":{\"@xsins.type\":\"ns1:AegisTestBean\","
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}}";
        
        byte[] simpleBytes = data.getBytes("utf-8");
        Object beanObject = p.readFrom((Class)AegisTestBean.class, null, null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        AegisTestBean bean = (AegisTestBean) beanObject;
        assertEquals("hovercraft", bean.getStrValue());
        assertEquals(Boolean.TRUE, bean.getBoolValue());
    }
    
    @Test
    public void testWriteToWithXsiType() throws Exception {
        String data = "{\"ns1.AegisTestBean\":{\"@ns2.type\":\"ns1:AegisTestBean\","
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}}";
        doTestWriteTo(data, true, true);
    }
    
    @Test
    public void testWriteToWithXsiTypeNoNamespaces() throws Exception {
        String data = "{\"ns1.AegisTestBean\":{\"@xsins.type\":\"ns1:AegisTestBean\","
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}}";
        doTestWriteTo(data, true, false);
    }
    
    @Test
    public void testWriteToWithoutXsiType() throws Exception {
        String data = "{\"ns1.AegisTestBean\":{"
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}}";
        doTestWriteTo(data, false, true);
    }
    
    
    private void doTestWriteTo(String data, boolean writeXsi, boolean setNsMap) throws Exception {
        AegisJSONProvider p = new AegisJSONProvider();
        AbstractAegisProvider.clearContexts();
        p.setWriteXsiType(writeXsi);
        if (setNsMap) {
            Map<String, String> namespaceMap = new HashMap<String, String>();
            namespaceMap.put("http://fortest.jaxrs.cxf.apache.org", "ns1");
            namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "ns2");
            p.setNamespaceMap(namespaceMap);
        }    
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        AegisTestBean bean = new AegisTestBean();
        bean.setBoolValue(Boolean.TRUE);
        bean.setStrValue("hovercraft");
        p.writeTo(bean, (Class)AegisTestBean.class, AegisTestBean.class, 
                  AegisTestBean.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        byte[] bytes = os.toByteArray();
        String json = new String(bytes, "utf-8");
        assertEquals(data, json);
        
    }
    
    @Test
    public void testManyTags() throws Exception {
        AegisJSONProvider p = new AegisJSONProvider();
        AbstractAegisProvider.clearContexts();
        p.setWriteXsiType(false);
        p.setSerializeAsArray(true);
        
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        ManyTags many = new ManyTags();
        many.setTags(tags);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(many, (Class)ManyTags.class, ManyTags.class, ManyTags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        String data1 = "{\"ns1.ManyTags\":[{\"ns1.tags\":[{},{\"ns1.TagVO\""
            + ":{\"ns1.group\":\"b\",\"ns1.name\":\"a\"}}]}]}";
        String data2 = "{\"ns1.ManyTags\":[{\"ns1.tags\":[{},{\"ns1.TagVO\""
            + ":{\"ns1.name\":\"a\",\"ns1.group\":\"b\"}}]}]}";
        assertTrue(data1.equals(s) || data2.equals(s));
    }
    
    private TagVO createTag(String name, String group) {
        return new TagVO(name, group);
    }
}

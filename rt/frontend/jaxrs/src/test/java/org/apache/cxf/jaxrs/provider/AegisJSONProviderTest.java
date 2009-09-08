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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.fortest.AegisTestBean;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.CollectionsResource;
import org.apache.cxf.jaxrs.resources.ManyTags;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.jaxrs.resources.Tags;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AegisJSONProviderTest extends Assert {
    
    private Properties properties;
    
    @Before
    public void before() throws InvalidPropertiesFormatException, IOException {
        properties = new Properties();
        properties.loadFromXML(getClass().getResourceAsStream("jsonCases.xml"));
    }
    
    
    @Test
    public void testIsWriteable() {
        MessageBodyWriter<Object> p = new AegisJSONProvider<Object>();
        assertTrue(p.isWriteable(AegisTestBean.class, null, null, null));
    }
    
    @Test
    public void testIsReadable() {
        MessageBodyReader<Object> p = new AegisJSONProvider<Object>();
        assertTrue(p.isReadable(AegisTestBean.class, null, null, null));
    }
    
    
    @Test
    public void testReadFrom() throws Exception {
        doTestRead(true);
    }
    
    @Test
    public void testReadFromNoMap() throws Exception {
        doTestRead(false);
    }
    
    private void doTestRead(boolean setNsMap) throws Exception {
        AegisJSONProvider<AegisTestBean> p = new AegisJSONProvider<AegisTestBean>();
        AbstractAegisProvider.clearContexts();
        if (setNsMap) {
            Map<String, String> namespaceMap = new HashMap<String, String>();
            namespaceMap.put("http://fortest.jaxrs.cxf.apache.org", "ns1");
            namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
            p.setNamespaceMap(namespaceMap);
        }
        String data = "{\"ns1.AegisTestBean\":{\"@xsi.type\":\"ns1:AegisTestBean\","
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}}";
        
        byte[] simpleBytes = data.getBytes("utf-8");
        Object beanObject = p.readFrom(AegisTestBean.class, null, null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        AegisTestBean bean = (AegisTestBean) beanObject;
        assertEquals("hovercraft", bean.getStrValue());
        assertEquals(Boolean.TRUE, bean.getBoolValue());
    }
    
    @Test
    public void testWriteToWithXsiType() throws Exception {
        String data = "{\"ns1.AegisTestBean\":{\"@xsi.type\":\"ns1:AegisTestBean\","
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}}";
        doTestWriteTo(data, true, true);
    }
    
    @Test
    public void testWriteToWithXsiTypeNoNamespaces() throws Exception {
        String data = "{\"ns1.AegisTestBean\":{\"@xsi.type\":\"ns1:AegisTestBean\","
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
        AegisJSONProvider<Object> p = new AegisJSONProvider<Object>();
        AbstractAegisProvider.clearContexts();
        p.setWriteXsiType(writeXsi);
        if (setNsMap) {
            Map<String, String> namespaceMap = new HashMap<String, String>();
            namespaceMap.put("http://fortest.jaxrs.cxf.apache.org", "ns1");
            namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
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
    public void testWriteCollection() throws Exception {
        String json = writeCollection(true, false, null, true, false);
        assertEquals("{\"ns1.ArrayOfAegisTestBean\":{\"@xsi.type\":\"ns1:ArrayOfAegisTestBean\","
            + "\"ns1.AegisTestBean\":[{\"@xsi.type\":\"ns1:AegisTestBean\",\"ns1.boolValue\":true,"
            + "\"ns1.strValue\":\"hovercraft\"},{\"@xsi.type\":\"ns1:AegisTestBean\","
            + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft2\"}]}}", json);
    }
    
    @Test
    public void testWriteCollectionNoXsiType() throws Exception {
        String json = writeCollection(false, false, null, true, false);
        assertEquals("{\"ns1.ArrayOfAegisTestBean\":{"
                     + "\"ns1.AegisTestBean\":[{\"ns1.boolValue\":true,"
                     + "\"ns1.strValue\":\"hovercraft\"},{"
                     + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft2\"}]}}", json);
    }
    
    @Test
    public void testWriteCollectionNoXsiTypeArrayKey() throws Exception {
        String json = writeCollection(false, false, "ns1.AegisTestBean", true, false);
        assertEquals("{\"ns1.ArrayOfAegisTestBean\":{"
            + "\"ns1.AegisTestBean\":[{\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"},"
            + "{\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft2\"}]}}", json);
    }
    
    @Test
    public void testWriteCollectionIgnoreNs() throws Exception {
        String json = writeCollection(false, false, "ns1.AegisTestBean", true, true);
        assertEquals("{\"ArrayOfAegisTestBean\":{"
            + "\"AegisTestBean\":[{\"boolValue\":true,\"strValue\":\"hovercraft\"},"
            + "{\"boolValue\":true,\"strValue\":\"hovercraft2\"}]}}", json);
    }
    
    @Test
    public void testWriteCollectionNoXsiTypeSingleBeanArrayKey() throws Exception {
        String json = writeCollection(false, false, "AegisTestBean", false, true);
        assertEquals("{\"ArrayOfAegisTestBean\":{"
            + "\"AegisTestBean\":[{\"boolValue\":true,\"strValue\":\"hovercraft\"}"
            + "]}}", json);
    }
    
    @Test
    public void testWriteCollectionNoXsiTypeSingleBean() throws Exception {
        String json = writeCollection(false, false, null, false, false);
        assertEquals("{\"ns1.ArrayOfAegisTestBean\":{"
            + "\"ns1.AegisTestBean\":{\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft\"}"
            + "}}", json);
    }
    
    @Test
    public void testWriteCollectionNoXsiTypeDropRootElement() throws Exception {
        String json = writeCollection(false, true, null, true, false);
        assertEquals("{"
                     + "\"ns1.AegisTestBean\":[{\"ns1.boolValue\":true,"
                     + "\"ns1.strValue\":\"hovercraft\"},{"
                     + "\"ns1.boolValue\":true,\"ns1.strValue\":\"hovercraft2\"}]}", json);
    }
    
    private String writeCollection(boolean writeXsiType, 
                                   boolean dropRootElement,
                                   String arrayKey,
                                   boolean twoBeans,
                                   boolean ignoreNs) 
        throws Exception {
        AegisJSONProvider<List<AegisTestBean>> p = new AegisJSONProvider<List<AegisTestBean>>();
        p.setWriteXsiType(writeXsiType);
        p.setDropRootElement(dropRootElement);
        p.setIgnoreNamespaces(ignoreNs);
        if (arrayKey != null) {
            p.setSerializeAsArray(true);
            p.setArrayKeys(Collections.singletonList(arrayKey));
        }
        AbstractAegisProvider.clearContexts();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        AegisTestBean bean = new AegisTestBean();
        bean.setBoolValue(Boolean.TRUE);
        bean.setStrValue("hovercraft");
        List<AegisTestBean> beans = new ArrayList<AegisTestBean>();
        beans.add(bean);
        if (twoBeans) {
            AegisTestBean bean2 = new AegisTestBean();
            bean2.setBoolValue(Boolean.TRUE);
            bean2.setStrValue("hovercraft2");
            beans.add(bean2);
        }
        Method m = CollectionsResource.class.getMethod("getAegisBeans", new Class[] {});
        p.writeTo(beans, m.getReturnType(), m.getGenericReturnType(), AegisTestBean.class
            .getAnnotations(), MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        byte[] bytes = os.toByteArray();
        return new String(bytes, "utf-8");
    }
    
    
    @Test
    public void testReadCollection() throws Exception {
        String json = writeCollection(true, false, null, true, false);         
        byte[] simpleBytes = json.getBytes("utf-8");
        Method m = CollectionsResource.class.getMethod("getAegisBeans", new Class[] {});        
        AegisJSONProvider<List<AegisTestBean>> p = new AegisJSONProvider<List<AegisTestBean>>();
        // the only way to get the right class ref in there is to make a dummy list object.
        // is that reasonable?
        List<AegisTestBean> list = p.readFrom(null, m.getGenericReturnType(), null, 
                                              null, null, new ByteArrayInputStream(simpleBytes));
        assertEquals(2, list.size());
        AegisTestBean bean = list.get(0);
        assertEquals("hovercraft", bean.getStrValue());
        assertEquals(Boolean.TRUE, bean.getBoolValue());
        bean = list.get(1);
        assertEquals("hovercraft2", bean.getStrValue());
        assertEquals(Boolean.TRUE, bean.getBoolValue());
    }
    
    @Test
    public void testManyTags() throws Exception {
        AegisJSONProvider<ManyTags> p = new AegisJSONProvider<ManyTags>();
        p.setWriteXsiType(false);
        AbstractAegisProvider.clearContexts();
        p.setSerializeAsArray(true);
        
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        ManyTags many = new ManyTags();
        many.setTags(tags);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(many, ManyTags.class, ManyTags.class, ManyTags.class.getAnnotations(), 
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
    
    @org.junit.Ignore
    @Test
    public void testReadWriteComplexMap() throws Exception {
        Map<AegisTestBean, AegisSuperBean> testMap = 
            new HashMap<AegisTestBean, AegisSuperBean>();
        
        Class<InterfaceWithMap> iwithMapClass = InterfaceWithMap.class;
        Method method = iwithMapClass.getMethod("mapFunction");
        Type mapType = method.getGenericReturnType();

        AegisTestBean bean = new AegisTestBean();
        bean.setBoolValue(Boolean.TRUE);
        bean.setStrValue("hovercraft");
        
        AegisSuperBean bean2 = new AegisSuperBean();
        bean2.setBoolValue(Boolean.TRUE);
        bean2.setStrValue("hovercraft2");
        testMap.put(bean, bean2);
        
        AegisJSONProvider<Map<AegisTestBean, AegisSuperBean>> writer 
            = new AegisJSONProvider<Map<AegisTestBean, AegisSuperBean>>();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        writer.writeTo(testMap, testMap.getClass(), mapType, null, null, null, os);
        byte[] bytes = os.toByteArray();
        String xml = new String(bytes, "utf-8");
        String expected = properties.getProperty("testReadWriteComplexMap.expected");
        assertEquals(expected, xml);        
        AegisJSONProvider<Map<AegisTestBean, AegisSuperBean>> reader 
            = new AegisJSONProvider<Map<AegisTestBean, AegisSuperBean>>();       
        byte[] simpleBytes = xml.getBytes("utf-8");
        
        Map<AegisTestBean, AegisSuperBean> map2 = reader.readFrom(null, mapType, null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        assertEquals(1, map2.size());
        Map.Entry<AegisTestBean, AegisSuperBean> entry = map2.entrySet().iterator().next();
        AegisTestBean bean1 = entry.getKey();
        assertEquals("hovercraft", bean1.getStrValue());
        assertEquals(Boolean.TRUE, bean1.getBoolValue());
        AegisTestBean bean22 = entry.getValue();
        assertEquals("hovercraft2", bean22.getStrValue());
        assertEquals(Boolean.TRUE, bean22.getBoolValue());
        
    }
    
    public static class AegisSuperBean extends AegisTestBean {
    }
    
    private static interface InterfaceWithMap {
        Map<AegisTestBean, AegisSuperBean> mapFunction();
    }
}

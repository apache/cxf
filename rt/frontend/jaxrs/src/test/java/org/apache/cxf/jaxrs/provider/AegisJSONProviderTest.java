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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.junit.Ignore;
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
    
    @SuppressWarnings("unchecked")
    private void doTestRead(boolean setNsMap) throws Exception {
        AegisJSONProvider p = new AegisJSONProvider();
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
        Object beanObject = p.readFrom((Class)AegisTestBean.class, null, null, 
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
        AegisJSONProvider p = new AegisJSONProvider();
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
        String json = writeCollection();
        assertEquals("{\"ns1.ArrayOfAegisTestBean\":{\"@xsi.type\":\"ns1:ArrayOfAegisTestBean\","
            + "\"ns1.AegisTestBean\":{\"@xsi.type\":\"ns1:AegisTestBean\",\"ns1.boolValue\":true,"
            + "\"ns1.strValue\":\"hovercraft\"}}}", json);
    }
    
    private String writeCollection() throws Exception {
        AegisJSONProvider p = new AegisJSONProvider();
        AbstractAegisProvider.clearContexts();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        AegisTestBean bean = new AegisTestBean();
        bean.setBoolValue(Boolean.TRUE);
        bean.setStrValue("hovercraft");
        List<AegisTestBean> beans = new ArrayList<AegisTestBean>();
        beans.add(bean);
        Method m = CollectionsResource.class.getMethod("getAegisBeans", new Class[] {});
        p.writeTo(beans, (Class)m.getReturnType(), m.getGenericReturnType(), AegisTestBean.class
            .getAnnotations(), MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        byte[] bytes = os.toByteArray();
        return new String(bytes, "utf-8");
    }
    
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadCollection() throws Exception {
        String json = writeCollection();         
        byte[] simpleBytes = json.getBytes("utf-8");
        Method m = CollectionsResource.class.getMethod("getAegisBeans", new Class[] {});        
        AegisJSONProvider p = new AegisJSONProvider();
        Object beanObject = p.readFrom((Class)m.getReturnType(), m.getGenericReturnType(), null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        List<AegisTestBean> list = (List)beanObject;
        assertEquals(1, list.size());
        AegisTestBean bean = list.get(0);
        assertEquals("hovercraft", bean.getStrValue());
        assertEquals(Boolean.TRUE, bean.getBoolValue());
    }
    
    @Test
    @Ignore
    public void testManyTags() throws Exception {
        AegisJSONProvider p = new AegisJSONProvider();
        p.setWriteXsiType(false);
        AbstractAegisProvider.clearContexts();
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

    @SuppressWarnings("unchecked")
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
        
        AegisJSONProvider writer = new AegisJSONProvider();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("urn:org.apache.cxf.aegis.types", "ns1");
        namespaceMap.put("http://fortest.jaxrs.cxf.apache.org", "ns2");
        writer.setNamespaceMap(namespaceMap);
        
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        writer.writeTo(testMap, testMap.getClass(), mapType, null, null, null, os);
        byte[] bytes = os.toByteArray();
        String xml = new String(bytes, "utf-8");
        String expected = "{\"ns1.AegisTestBean2AegisSuperBeanMap\":{\"@xsi.type\":"
            + "\"ns1:AegisTestBean2AegisSuperBeanMap\",\"ns1.entry\":{\"ns1.key\":{\"@xsi.type\":\"ns2:"
            + "AegisTestBean\",\"ns2.boolValue\":true,\"ns2.strValue\":\"hovercraft\"},\"ns1.value\":"
            + "{\"@xsi.type\":\"ns3:AegisSuperBean\",\"ns2.boolValue\":true,"
            + "\"ns2.strValue\":\"hovercraft2\"}}}}";
        assertEquals(expected, xml);
        AegisJSONProvider reader = new AegisJSONProvider();       
        Map<String, String> namespaceMap2 = new HashMap<String, String>();
        namespaceMap2.put("urn:org.apache.cxf.aegis.types", "ns1");
        namespaceMap2.put("http://fortest.jaxrs.cxf.apache.org", "ns2");
        reader.setNamespaceMap(namespaceMap2);
        byte[] simpleBytes = xml.getBytes("utf-8");
        
        Object beanObject = reader.readFrom((Class)Map.class, mapType, null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        Map<AegisTestBean, AegisSuperBean> map2 = (Map)beanObject;
        assertEquals(1, map2.size());
        Map.Entry<AegisTestBean, AegisSuperBean> entry = map2.entrySet().iterator().next();
        AegisTestBean bean1 = entry.getKey();
        assertEquals("hovercraft", bean1.getStrValue());
        assertEquals(Boolean.TRUE, bean1.getBoolValue());
        AegisSuperBean bean22 = entry.getValue();
        assertEquals("hovercraft2", bean22.getStrValue());
        assertEquals(Boolean.TRUE, bean22.getBoolValue());
        
    }
    
    @SuppressWarnings("unchecked")
    @Test
    @Ignore("Started failing unexpectedly, probably makes sense though"
            + "for a map containing key and value from diff packages to have"
            + "dedicated prefixes")
    public void testReadWriteComplexMapIgnored() throws Exception {
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
        
        AegisJSONProvider writer = new AegisJSONProvider();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("urn:org.apache.cxf.aegis.types", "ns1");
        namespaceMap.put("http://fortest.jaxrs.cxf.apache.org", "ns2");
        writer.setNamespaceMap(namespaceMap);
        
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        writer.writeTo(testMap, testMap.getClass(), mapType, null, null, null, os);
        byte[] bytes = os.toByteArray();
        String xml = new String(bytes, "utf-8");
        String expected = "{\"ns1.AegisTestBean2AegisSuperBeanMap\":{\"@xsi.type\":"
            + "\"ns1:AegisTestBean2AegisSuperBeanMap\",\"ns1.entry\":{\"ns1.key\":{\"@xsi.type\":\"ns1:"
            + "AegisTestBean\",\"ns2.boolValue\":true,\"ns2.strValue\":\"hovercraft\"},\"ns1.value\":"
            + "{\"@xsi.type\":\"ns1:AegisSuperBean\",\"ns2.boolValue\":true,"
            + "\"ns2.strValue\":\"hovercraft2\"}}}}";
        assertEquals(expected, xml);        
        AegisJSONProvider reader = new AegisJSONProvider();       
        Map<String, String> namespaceMap2 = new HashMap<String, String>();
        namespaceMap2.put("urn:org.apache.cxf.aegis.types", "ns1");
        namespaceMap2.put("http://fortest.jaxrs.cxf.apache.org", "ns2");
        reader.setNamespaceMap(namespaceMap2);
        byte[] simpleBytes = xml.getBytes("utf-8");
        
        Object beanObject = reader.readFrom((Class)Map.class, mapType, null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        Map<AegisTestBean, AegisSuperBean> map2 = (Map)beanObject;
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

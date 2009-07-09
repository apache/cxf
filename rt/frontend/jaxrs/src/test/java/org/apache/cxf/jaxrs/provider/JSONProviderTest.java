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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.ManyTags;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.jaxrs.resources.TagVO2;
import org.apache.cxf.jaxrs.resources.Tags;

import org.junit.Assert;
import org.junit.Test;

public class JSONProviderTest extends Assert {

    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadFromTags() throws Exception {
        MessageBodyReader<Object> p = new JSONProvider();
        byte[] bytes = 
            "{\"Tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"},{\"group\":\"d\",\"name\":\"c\"}]}}"
            .getBytes();
        Object tagsObject = p.readFrom((Class)Tags.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        Tags tags = (Tags)tagsObject;
        List<TagVO> list = tags.getTags();
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).getName());
        assertEquals("b", list.get(0).getGroup());
        assertEquals("c", list.get(1).getName());
        assertEquals("d", list.get(1).getGroup());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadFromTag() throws Exception {
        MessageBodyReader<Object> p = new JSONProvider();
        byte[] bytes = "{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}"
            .getBytes();
        Object tagsObject = p.readFrom((Class)TagVO.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        TagVO tag = (TagVO)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testReadFromUnwrappedTagWrapperName() throws Exception {
        JSONProvider p = new JSONProvider();
        p.setSupportUnwrapped(true);
        p.setWrapperName("tagVO");
        readUnwrapped(p);
    }
    
    @Test
    public void testReadFromUnwrappedTagMap() throws Exception {
        JSONProvider p = new JSONProvider();
        p.setSupportUnwrapped(true);
        
        Map<String, String> map = new HashMap<String, String>();
        map.put(TagVO.class.getName(), "tagVO");
        p.setWrapperMap(map);
        readUnwrapped(p);
    }
    
    @Test
    public void testReadFromUnwrappedTagRoot() throws Exception {
        JSONProvider p = new JSONProvider();
        p.setSupportUnwrapped(true);
        readUnwrapped(p);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testReadFromUnwrappedQualifiedTagRoot() throws Exception {
        JSONProvider p = new JSONProvider();
        p.setSupportUnwrapped(true);
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        
        byte[] bytes = "{\"group\":\"b\",\"name\":\"a\"}"
            .getBytes();
        Object tagsObject = p.readFrom((Class)TagVO2.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        TagVO2 tag = (TagVO2)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @SuppressWarnings("unchecked")
    private void readUnwrapped(JSONProvider p) throws Exception {
        byte[] bytes = "{\"group\":\"b\",\"name\":\"a\"}"
            .getBytes();
        Object tagsObject = p.readFrom((Class)TagVO.class, null, null, 
                                          null, null, new ByteArrayInputStream(bytes));
        TagVO tag = (TagVO)tagsObject;
        assertEquals("a", tag.getName());
        assertEquals("b", tag.getGroup());
    }
    
    @Test
    public void testWriteToSingleTag() throws Exception {
        JSONProvider p = new JSONProvider();
        TagVO tag = createTag("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, (Class)TagVO.class, TagVO.class, TagVO.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"tagVO\":{\"group\":\"b\",\"name\":\"a\"}}", s);
        
    }
    
    @Test
    public void testWriteToSingleQualifiedTag() throws Exception {
        JSONProvider p = new JSONProvider();
        Map<String, String> namespaceMap = new HashMap<String, String>();
        namespaceMap.put("http://tags", "ns1");
        p.setNamespaceMap(namespaceMap);
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, (Class)TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"ns1.thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);
        
    }
    
    @Test
    public void testWriteToSingleQualifiedTag2() throws Exception {
        JSONProvider p = new JSONProvider();
        TagVO2 tag = createTag2("a", "b");
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tag, (Class)TagVO2.class, TagVO2.class, TagVO2.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals("{\"ns1.thetag\":{\"group\":\"b\",\"name\":\"a\"}}", s);
        
    }
    
    @Test
    public void testWriteToListWithManyValues() throws Exception {
        JSONProvider p = new JSONProvider();
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        tags.addTag(createTag("c", "d"));
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tags, (Class)Tags.class, Tags.class, Tags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"Tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"},{\"group\":\"d\",\"name\":\"c\"}]}}",
            s);
    }
    
    @Test
    public void testWriteToListWithSingleValue() throws Exception {
        JSONProvider p = new JSONProvider();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("list"));
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(tags, (Class)Tags.class, Tags.class, Tags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"Tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}",
            s);
    }
    
    @Test
    public void testManyTags() throws Exception {
        JSONProvider p = new JSONProvider();
        p.setSerializeAsArray(true);
        p.setArrayKeys(Collections.singletonList("list"));
        
        Tags tags = new Tags();
        tags.addTag(createTag("a", "b"));
        ManyTags many = new ManyTags();
        many.setTags(tags);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        p.writeTo(many, (Class)ManyTags.class, ManyTags.class, ManyTags.class.getAnnotations(), 
                  MediaType.APPLICATION_JSON_TYPE, new MetadataMap<String, Object>(), os);
        
        String s = os.toString();
        assertEquals(
            "{\"ManyTags\":{\"tags\":{\"list\":[{\"group\":\"b\",\"name\":\"a\"}]}}}",
            s);
    }
    
    private TagVO createTag(String name, String group) {
        return new TagVO(name, group);
    }
    
    private TagVO2 createTag2(String name, String group) {
        return new TagVO2(name, group);
    }
}

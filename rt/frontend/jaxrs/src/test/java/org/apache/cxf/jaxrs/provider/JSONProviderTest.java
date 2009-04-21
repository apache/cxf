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

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.resources.ManyTags;
import org.apache.cxf.jaxrs.resources.TagVO;
import org.apache.cxf.jaxrs.resources.Tags;
import org.junit.Assert;
import org.junit.Test;

public class JSONProviderTest extends Assert {

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
}

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

package org.apache.cxf.jaxrs.ext.multipart;

import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.apache.cxf.jaxrs.impl.MetadataMap;

import org.junit.Assert;
import org.junit.Test;

public class MultipartBodyTest extends Assert {

    @Test
    public void testGetAttachments() {
        List<Attachment> atts = new ArrayList<Attachment>();
        atts.add(createAttachment("p1"));
        atts.add(createAttachment("p2"));
        MultipartBody b = new MultipartBody(atts);
        assertEquals(atts, b.getAllAttachments());
        assertEquals(atts.get(0), b.getRootAttachment());
        assertEquals(atts.get(1), b.getChildAttachments().get(0));
    }
    
    @Test
    public void testGetAttachmentsById() {
        List<Attachment> atts = new ArrayList<Attachment>();
        atts.add(createAttachment("p1"));
        atts.add(createAttachment("p2"));
        MultipartBody b = new MultipartBody(atts);
        assertEquals(atts.get(0), b.getAttachment("p1"));
        assertEquals(atts.get(1), b.getAttachment("p2"));
        assertNull(b.getAttachment("p3"));
    }
    
    private Attachment createAttachment(String id) {
        return new Attachment(id, 
                       new DataHandler(new ByteArrayDataSource(new byte[]{1}, "application/octet-stream")),
                       new MetadataMap<String, String>());
    }
}

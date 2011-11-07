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
import java.lang.annotation.Annotation;

import javax.activation.DataHandler;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.impl.MetadataMap;

import org.junit.Assert;
import org.junit.Test;

public class DataHandlerProviderTest extends Assert {

    @Test
    public void testReadFrom() throws Exception {
        DataHandlerProvider p = new DataHandlerProvider();
        DataHandler ds = p.readFrom(DataHandler.class, DataHandler.class, new Annotation[]{}, 
                   MediaType.valueOf("image/png"), new MetadataMap<String, String>(), 
                   new ByteArrayInputStream("image".getBytes()));
        
        assertEquals("image", IOUtils.readStringFromStream(ds.getDataSource().getInputStream()));
        
    }
    
    @Test
    public void testWriteFrom() throws Exception {
        DataHandlerProvider p = new DataHandlerProvider();
        DataHandler ds = new DataHandler(new InputStreamDataSource(
                             new ByteArrayInputStream("image".getBytes()), 
                             "image/png")); 
        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
        p.writeTo(ds, DataHandler.class, DataHandler.class, new Annotation[]{}, 
                   MediaType.valueOf("image/png"), new MetadataMap<String, Object>(), os);
        assertEquals("image", os.toString());
        
    }
    
    
}

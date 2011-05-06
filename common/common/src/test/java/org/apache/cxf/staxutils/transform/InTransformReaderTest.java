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
package org.apache.cxf.staxutils.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;
import org.junit.Test;

public class InTransformReaderTest extends Assert {
    @Test
    public void testReadWithDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream("<test xmlns=\"http://bar\"/>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("{http://bar}test", "test2"),
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertTrue("<test2 xmlns=\"\"/>".equals(value));        
    }
    
    @Test
    public void testReadWithParentDefaultNamespace() throws Exception {
        InputStream is = new ByteArrayInputStream(
            "<test xmlns=\"http://bar\"><ns:subtest xmlns:ns=\"http://bar1\"/></test>".getBytes());
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        reader = new InTransformReader(reader, 
                                       Collections.singletonMap("{http://bar1}subtest", "subtest"),
                                       null, false);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
        StaxUtils.copy(reader, bos);
        String value = bos.toString();
        assertEquals("<ps1:test xmlns:ps1=\"http://bar\"><subtest xmlns=\"\"/></ps1:test>",
                     value);        
    }
}

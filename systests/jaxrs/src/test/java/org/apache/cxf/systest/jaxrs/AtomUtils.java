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


package org.apache.cxf.systest.jaxrs;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Entry;

public final class AtomUtils {
    
    private AtomUtils() {
        
    }
    
    public static Entry createBookEntry(Book b) throws Exception {
        return createBookEntry(b, null);
    }
    
    public static Entry createBookEntry(Book b, String baseUri) throws Exception {
        Factory factory = Abdera.getNewFactory();
        JAXBContext jc = JAXBContext.newInstance(Book.class);
        
        Entry e = factory.getAbdera().newEntry();
        if (baseUri != null) {
            e.setBaseUri(baseUri);
        }
        e.setTitle(b.getName());
        e.setId(Long.toString(b.getId()));
        
        
        StringWriter writer = new StringWriter();
        jc.createMarshaller().marshal(b, writer);
        
        e.setContentElement(factory.newContent());
        e.getContentElement().setContentType(Content.Type.XML);
        e.getContentElement().setValue(writer.toString());
        
        return e;
    }

}

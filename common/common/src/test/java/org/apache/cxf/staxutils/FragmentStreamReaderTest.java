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

package org.apache.cxf.staxutils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.Assert;
import org.junit.Test;

public class FragmentStreamReaderTest extends Assert {

    @Test
    public void testReader() throws Exception {
        XMLInputFactory ifactory = StaxUtils.getXMLInputFactory();
        XMLStreamReader reader = 
            ifactory.createXMLStreamReader(getClass().getResourceAsStream("./resources/amazon.xml"));
        
        DepthXMLStreamReader dr = new DepthXMLStreamReader(reader);
        
        StaxUtils.toNextElement(dr);
        assertEquals("ItemLookup", dr.getLocalName());
        assertEquals(XMLStreamReader.START_ELEMENT, reader.getEventType());
        
        FragmentStreamReader fsr = new FragmentStreamReader(dr);
        assertTrue(fsr.hasNext());
        
        assertEquals(XMLStreamReader.START_DOCUMENT, fsr.next());
        assertEquals(XMLStreamReader.START_DOCUMENT, fsr.getEventType());
        
        fsr.next();

        assertEquals("ItemLookup", fsr.getLocalName());
        assertEquals("ItemLookup", dr.getLocalName());
        assertEquals(XMLStreamReader.START_ELEMENT, reader.getEventType());
        
        fsr.close();
    }
}

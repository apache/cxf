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

package org.apache.cxf.helpers;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;

public class XMLUtilsTest extends Assert {

    @Test
    public void testToString() throws Exception {
        InputStream is = getClass().getResourceAsStream("resources/test.xml");
        Source source = new StreamSource(is);
        
        assertEquals("<test><ok/></test>", XMLUtils.toString(source));
    }
    
    @Test
    public void testXmlEncodeNoEscape() {
        assertEquals("12345", XMLUtils.xmlEncode("12345"));
    }
    
    @Test
    public void testXmlEncodeEscapeAtStart() {
        assertEquals("&quot;2345", XMLUtils.xmlEncode("\"2345"));
    }
    @Test
    public void testXmlEncodeEscapeAtEnd() {
        assertEquals("1234&apos;", XMLUtils.xmlEncode("1234'"));
    }
    
    @Test
    public void testXmlEncodeEscapeInMiddle() {
        assertEquals("12&amp;45", XMLUtils.xmlEncode("12&45"));
    }
    
    @Test
    public void testXmlEncodeEscapeMany() {
        assertEquals("&lt;2&amp;4&gt;", XMLUtils.xmlEncode("<2&4>"));
    }
    
    @Test
    public void testXmlEncodeEscapeAll() {
        assertEquals("&lt;&quot;&amp;&apos;&gt;", XMLUtils.xmlEncode("<\"&'>"));
    }
}

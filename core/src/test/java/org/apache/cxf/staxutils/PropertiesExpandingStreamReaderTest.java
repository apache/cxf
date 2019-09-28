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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PropertiesExpandingStreamReaderTest {

    @Test
    public void testSystemPropertyExpansion() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("bar", "BAR-VALUE");
        map.put("blah", "BLAH-VALUE");
        XMLStreamReader reader = new PropertiesExpandingStreamReader(
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("resources/sysprops.xml")), map);
        Document doc = StaxUtils.read(reader);
        Element abc = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
            "http://foo/bar", "abc").iterator().next();
        assertEquals("fooBAR-VALUEfoo", abc.getTextContent());
        Element def = DOMUtils.getChildrenWithName(doc.getDocumentElement(),
            "http://foo/bar", "def").iterator().next();
        assertEquals("ggggg", def.getTextContent());
        assertEquals("BLAH-VALUE2", def.getAttribute("myAttr"));
    }

}

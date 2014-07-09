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

import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.helpers.DOMUtils;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SysPropExpandingStreamReaderTest extends Assert {

    @Test
    public void testSystemPropertyExpansion() throws Exception {
        final String barProp = System.setProperty("bar", "BAR-VALUE");
        final String blahProp = System.setProperty("blah", "BLAH-VALUE");
        try {
            XMLStreamReader reader = new SysPropExpandingStreamReader(StaxUtils.createXMLStreamReader(getTestStream("./resources/sysprops.xml")));
            Document doc = StaxUtils.read(reader);
            Element abc = DOMUtils.getChildrenWithName(doc.getDocumentElement(), "http://foo/bar", "abc").iterator().next();
            assertEquals("fooBAR-VALUEfoo", abc.getTextContent());
            Element def = DOMUtils.getChildrenWithName(doc.getDocumentElement(), "http://foo/bar", "def").iterator().next();
            assertEquals("ggggg", def.getTextContent());
            assertEquals("BLAH-VALUE2", def.getAttribute("myAttr"));
        } finally {
            if (barProp != null) {
                System.setProperty("bar", barProp);
            } else {
                System.clearProperty("bar");
            }
            if (blahProp != null) {
                System.setProperty("blah", blahProp);
            } else {
                System.clearProperty("blah");
            }
        }
    }

    private InputStream getTestStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }
}

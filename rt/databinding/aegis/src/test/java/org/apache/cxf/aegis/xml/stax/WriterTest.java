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
package org.apache.cxf.aegis.xml.stax;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;

import org.apache.cxf.aegis.util.jdom.StaxBuilder;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.jdom.JDOMWriter;
import org.apache.cxf.test.AbstractCXFTest;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.DOMOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Nov 4, 2004
 */
public class WriterTest extends AbstractCXFTest {
    File output;

    @Before
    public void setUp() throws Exception {
        super.setUpBus();

        output = File.createTempFile("writetest", ".xml");
    }

    @After
    public void tearDown() {
        if (output.exists()) {
            output.delete();
        }
    }

    @Test
    public void testLiteral() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ElementWriter writer = new ElementWriter(bos, "root", "urn:test");

        write(writer);

        writer.flush();
        bos.close();

        // System.out.println(bos.toString());
        StaxBuilder builder = new StaxBuilder();
        Document doc = builder.build(new StringReader(bos.toString()));

        testWrite(doc);
    }

    @Test
    public void testJDOM() throws Exception {
        Document doc = new Document(new Element("root", "urn:test"));

        write(new JDOMWriter(doc.getRootElement()));

        testWrite(doc);
    }

    public void write(MessageWriter writer) {
        MessageWriter nons = writer.getElementWriter("nons");
        nons.writeValue("nons");
        nons.close();

        MessageWriter intval = writer.getElementWriter("int");
        intval.writeValueAsInt(new Integer(10000));
        intval.close();

        MessageWriter child1 = writer.getElementWriter("child1", "urn:child1");
        MessageWriter att1 = child1.getAttributeWriter("att1");
        att1.writeValue("att1");
        att1.close();
        MessageWriter att2 = child1.getAttributeWriter("att2", "");
        att2.writeValue("att2");
        att2.close();
        MessageWriter att3 = child1.getAttributeWriter("att3", "urn:att3");
        att3.writeValue("att3");
        att3.close();
        MessageWriter att4 = child1.getAttributeWriter("att4", null);
        att4.writeValue("att4");
        att4.close();

        child1.close();

        writer.close();
    }

    public void testWrite(Document jdoc) throws Exception {
        org.w3c.dom.Document doc = new DOMOutputter().output(jdoc);
        addNamespace("t", "urn:test");
        addNamespace("c", "urn:child1");
        addNamespace("a", "urn:att3");

        assertValid("/t:root/t:nons[text()='nons']", doc);
        assertValid("/t:root/t:int[text()='10000']", doc);
        assertValid("/t:root/c:child1", doc);
        assertValid("/t:root/c:child1[@c:att1]", doc);
        assertValid("/t:root/c:child1[@att2]", doc);
        assertValid("/t:root/c:child1[@a:att3]", doc);
        assertValid("/t:root/c:child1[@att4]", doc);
    }
}

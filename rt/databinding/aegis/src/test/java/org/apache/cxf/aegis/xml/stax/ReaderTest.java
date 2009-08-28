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

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.AbstractCXFTest;
import org.junit.Test;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Nov 4, 2004
 */
public class ReaderTest extends AbstractCXFTest {
    @Test
    public void testLiteralReader() throws Exception {
        ElementReader lr = getStreamReader("bean11.xml");
        testReading(lr);

        lr = getStreamReader("read1.xml");
        testReading2(lr);
    }

    private ElementReader getStreamReader(String resource) throws FactoryConfigurationError,
        XMLStreamException {
        /*
         * XMLInputFactory factory = XMLInputFactory.newInstance();
         * XMLStreamReader reader = factory.createXMLStreamReader(
         * getResourceAsStream(resource));
         */
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(getResourceAsStream(resource), null);

        while (reader.getEventType() != XMLStreamReader.START_ELEMENT) {
            reader.next();
        }

        return new ElementReader(reader);
    }

    public void testReading(MessageReader reader) {
        assertTrue(reader.getLocalName().equals("Envelope"));

        // make sure we can repeat this
        assertTrue(reader.hasMoreElementReaders());
        assertTrue(reader.hasMoreElementReaders());
        assertTrue(reader.hasMoreElementReaders());

        MessageReader header = reader.getNextElementReader();
        assertEquals("Header", header.getLocalName());
        assertEquals(Soap11.getInstance().getNamespace(), header.getNamespace());
        assertFalse(header.hasMoreElementReaders());

        MessageReader body = reader.getNextElementReader();
        assertEquals("Body", body.getLocalName());
        assertFalse(body.hasMoreElementReaders());
    }

    public void testReading2(MessageReader reader) throws Exception {
        assertEquals("test", reader.getLocalName());
        assertEquals("urn:test", reader.getNamespace());

        // make sure we can repeat this
        assertTrue(reader.hasMoreAttributeReaders());
        assertTrue(reader.hasMoreAttributeReaders());
        assertTrue(reader.hasMoreAttributeReaders());

        MessageReader one = reader.getNextAttributeReader();
        assertEquals("one", one.getValue());

        MessageReader two = reader.getNextAttributeReader();
        assertEquals("two", two.getValue());

        assertFalse(reader.hasMoreAttributeReaders());

        assertTrue(reader.hasMoreElementReaders());
        assertTrue(reader.hasMoreElementReaders());
        assertTrue(reader.hasMoreElementReaders());

        MessageReader child = reader.getNextElementReader();
        assertEquals("child", child.getLocalName());
        assertTrue(child.hasMoreElementReaders());
    }
}

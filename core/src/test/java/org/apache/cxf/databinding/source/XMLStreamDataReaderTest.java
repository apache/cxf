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

package org.apache.cxf.databinding.source;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class XMLStreamDataReaderTest {
    private static final byte[] DUMMY_DATA = "<ns:dummy xmlns:ns='http://www.apache.org/cxf'/>".getBytes();
    private static final byte[] DUMMY_DATA_NULL_BYTE =
        "<ns:dummy xmlns:ns='http://www.apache.org/cxf&#x00;'/>".getBytes();

    @Test
    public void testCloseOriginalInputStream() throws Exception {
        XMLStreamDataReader reader = new XMLStreamDataReader();
        Message msg = new MessageImpl();

        TestInputStream in1 = new TestInputStream(DUMMY_DATA);

        msg.setContent(InputStream.class, in1);

        reader.setProperty(Message.class.getName(), msg);

        Object obj = reader.read(new QName("http://www.apache.org/cxf", "dummy"),
                                 StaxUtils.createXMLStreamReader(in1), XMLStreamReader.class);

        assertTrue(obj instanceof XMLStreamReader);

        assertFalse(in1.isClosed());
        ((XMLStreamReader)obj).close();

        assertTrue(in1.isClosed());
    }

    // Parse some XML with a null byte and check we don't return internal Woodstox package names in the error message
    @Test
    public void testParseNullByte() throws Exception {
        XMLStreamDataReader reader = new XMLStreamDataReader();
        Message msg = new MessageImpl();

        TestInputStream in1 = new TestInputStream(DUMMY_DATA_NULL_BYTE);

        msg.setContent(InputStream.class, in1);

        reader.setProperty(Message.class.getName(), msg);

        Object obj = reader.read(new QName("http://www.apache.org/cxf", "dummy"),
                StaxUtils.createXMLStreamReader(in1), XMLStreamReader.class);

        assertTrue(obj instanceof XMLStreamReader);

        try {
            reader.read((XMLStreamReader) obj);
        } catch (Fault f) {
            assertFalse(f.getMessage().contains("com.ctc.wstx"));
        }

        ((XMLStreamReader)obj).close();
    }

    private static class TestInputStream extends ByteArrayInputStream {
        private boolean closed;

        TestInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}

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
package org.apache.cxf.binding.corba.runtime;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.types.CorbaTypeEventProducer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CorbaStreamReaderTest {

    private CorbaStreamReader reader;
    private CorbaTypeEventProducer mock;

    @Before
    public void setUp() throws Exception {
        mock = mock(CorbaTypeEventProducer.class);
        reader = new CorbaStreamReader(mock);
    }

    @Test
    public void testGetName() throws Exception {
        when(mock.getName()).thenReturn(new QName("http://foo.org", "test"));
        assertEquals("checking getName ", new QName("http://foo.org", "test"), reader.getName());
    }

    @Test
    public void testGetLocalName() throws Exception {
        when(mock.getName()).thenReturn(new QName("http://foo.org", "test"));
        assertEquals("checking localName ", "test", reader.getLocalName());
    }

    @Test
    public void testGetNamespaceURI() throws Exception {
        when(mock.getName()).thenReturn(new QName("http://foo.org", "test"));
        assertEquals("checking namespace ", "http://foo.org", reader.getNamespaceURI());
    }

    @Test
    public void testGetText() throws Exception {
        when(mock.getText()).thenReturn("abcdef");
        assertEquals("checking getText", "abcdef", reader.getText());
    }


    @Test
    public void testGetTextCharacters() throws Exception {
        when(mock.getText()).thenReturn("abcdef");
        assertEquals("checking getTextCharacters",
                    "abcdef",
                    new String(reader.getTextCharacters()));
    }
}
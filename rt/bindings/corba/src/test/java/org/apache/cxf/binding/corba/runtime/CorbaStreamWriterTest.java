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

import javax.xml.stream.XMLStreamException;

import org.apache.cxf.binding.corba.types.AbstractCorbaTypeListener;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CorbaStreamWriterTest {

    @Test
    public void writeCharactersTest() throws XMLStreamException {
        CorbaStreamWriter writer = new CorbaStreamWriter(null, null, null);
        final String[] pointer = new String[1];

        writer.currentTypeListener = new AbstractCorbaTypeListener(null) {
            @Override
            public void processCharacters(String text) {
                pointer[0] = text;
            }
        };

        writer.writeCharacters("abcdefghijklmnopqrstuvwxyz".toCharArray(), 0, 4);
        assertEquals("abcd", pointer[0]);
    }

}
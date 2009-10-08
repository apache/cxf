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
package org.apache.cxf.aegis.type.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.util.jdom.StaxBuilder;
import org.apache.cxf.aegis.util.jdom.StaxSerializer;
import org.apache.cxf.aegis.util.stax.JDOMStreamReader;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.jdom.Document;

/**
 * Reads and writes <code>org.w3c.dom.Document</code> types.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class JDOMDocumentType extends Type {
    // private static final StaxBuilder builder = new StaxBuilder();
    private static final StaxSerializer SERIALIZER = new StaxSerializer();

    public JDOMDocumentType() {
        setWriteOuter(false);
    }

    @Override
    public Object readObject(MessageReader mreader, Context context) throws DatabindingException {
        StaxBuilder builder = new StaxBuilder();
        try {
            XMLStreamReader reader = ((ElementReader)mreader).getXMLStreamReader();

            if (reader instanceof JDOMStreamReader) {
                return ((JDOMStreamReader)reader).getCurrentElement();
            }

            return builder.build(reader);
        } catch (XMLStreamException e) {
            throw new DatabindingException("Could not parse xml.", e);
        }
    }

    @Override
    public void writeObject(Object object, MessageWriter writer,
                            Context context) throws DatabindingException {
        Document doc = (Document)object;

        try {
            SERIALIZER.writeElement(doc.getRootElement(), ((ElementWriter)writer).getXMLStreamWriter());
        } catch (XMLStreamException e) {
            throw new DatabindingException("Could not write xml.", e);
        }
    }
}

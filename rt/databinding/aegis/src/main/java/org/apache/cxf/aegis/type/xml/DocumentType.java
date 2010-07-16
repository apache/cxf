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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.staxutils.FragmentStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Reads and writes <code>org.w3c.dom.Document</code> types.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class DocumentType extends Type {
    public DocumentType() {
        setWriteOuter(false);
    }

    @Override
    public Object readObject(MessageReader mreader, Context context) throws DatabindingException {
        try {
            XMLStreamReader reader = ((ElementReader)mreader).getXMLStreamReader();
            // we need to eat the surrounding element.
            reader.nextTag();
            Object tree = StaxUtils.read(null, new FragmentStreamReader(reader), true);
            reader.nextTag(); // eat the end tag.
            return tree;
        } catch (XMLStreamException e) {
            throw new DatabindingException("Could not parse xml.", e);
        }
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, 
                            Context context) throws DatabindingException {
        Document doc = (Document)object;

        try {
            Element docElement = doc.getDocumentElement();
            if (docElement == null) {
                if (isNillable()) {
                    writer.writeXsiNil();
                } else {
                    throw new DatabindingException("Could not write xml: null document element.");
                }
            } else {
                StaxUtils.writeElement(docElement, ((ElementWriter)writer).getXMLStreamWriter(), false);
            }
        } catch (XMLStreamException e) {
            throw new DatabindingException("Could not write xml.", e);
        }
    }
}

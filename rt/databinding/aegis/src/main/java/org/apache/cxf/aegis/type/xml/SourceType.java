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

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Reads and writes <code>javax.xml.transform.Source</code> types.
 * <p>
 * The XML stream is converted DOMSource and sent off.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @see javanet.staxutils.StAXSource
 * @see javax.xml.stream.XMLInputFactory
 * @see org.apache.cxf.aegis.util.STAXUtils
 */
public class SourceType extends AegisType {
    public SourceType() {
        setTypeClass(Source.class);
        setWriteOuter(false);
    }

    @Override
    public Object readObject(MessageReader mreader, Context context) throws DatabindingException {
        DocumentType dt = (DocumentType)getTypeMapping().getType(Document.class);

        return new DOMSource((Document)dt.readObject(mreader, context));
    }

    @Override
    public void writeObject(Object object, MessageWriter writer,
                            Context context) throws DatabindingException {
        try {
            if (object == null) {
                return;
            }

            write((Source)object, ((ElementWriter)writer).getXMLStreamWriter());
        } catch (XMLStreamException e) {
            throw new DatabindingException("Could not write xml.", e);
        }
    }

    protected void write(Source object, XMLStreamWriter writer) throws FactoryConfigurationError,
        XMLStreamException, DatabindingException {
        if (object == null) {
            return;
        }

        if (object instanceof DOMSource) {
            DOMSource ds = (DOMSource)object;

            Element element = null;
            if (ds.getNode() instanceof Element) {
                element = (Element)ds.getNode();
            } else if (ds.getNode() instanceof Document) {
                element = ((Document)ds.getNode()).getDocumentElement();
            } else {
                throw new DatabindingException("Node type " + ds.getNode().getClass()
                                               + " was not understood.");
            }

            StaxUtils.writeElement(element, writer, false);
        } else {
            StaxUtils.copy((Source)object, writer);
        }
    }

    protected XMLReader createXMLReader() throws SAXException {
        // In JDK 1.4, the xml reader factory does not look for META-INF
        // services
        // If the org.xml.sax.driver system property is not defined, and
        // exception will be thrown.
        // In these cases, default to xerces parser
        try {
            return XMLReaderFactory.createXMLReader();
        } catch (Exception e) {
            return XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        }
    }

}

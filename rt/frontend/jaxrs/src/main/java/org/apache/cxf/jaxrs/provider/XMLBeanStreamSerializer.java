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

package org.apache.cxf.jaxrs.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.xmlbeans.XmlObject;

/**
 * Serializes an XMLBean data object to an XML stream Note: uses an intermediate file created by
 * File.createTempFile(String, String) as I couldn't work out how to fit a normal stream into an event driven
 * XML stream.
 */
public class XMLBeanStreamSerializer {

 /**
     * Serialize the given XML data object. Writes the data object to a temporary file then reads it back in
     * with an <code>XMLStreamReader<code>. 
     *  This allows the events from the reader to drive the output to the <code>XMLStreamWriter</code>.
     *  Probably not the best way to do this.
     * 
     * @param obj
     * @param writer
     */
    public void serialize(XmlObject xObj, XMLStreamWriter writer) throws IOException, XMLStreamException {

        File tmpFile = null;

        try {

            // create tmp file
            tmpFile = File.createTempFile(Integer.toString(xObj.hashCode()), ".xml");
            // TODO may need to set some XMLOptions here
            // write to tmp file
            xObj.save(tmpFile);

            InputStream tmpIn = new FileInputStream(tmpFile);
            XMLStreamReader rdr = XMLInputFactory.newInstance().createXMLStreamReader(tmpIn);

            while (rdr.hasNext()) {

                int event = rdr.next();

                switch (event) {

                case XMLStreamConstants.START_DOCUMENT:
                    writer.writeStartDocument();
                    break;

                case XMLStreamConstants.END_DOCUMENT:
                    writer.writeEndDocument();
                    break;

                case XMLStreamConstants.START_ELEMENT:
                    String name = rdr.getLocalName();
                    writer.writeStartElement(name);

                    // handle attributes
                    int attrCount = rdr.getAttributeCount();
                    for (int i = 0; i < attrCount; i++) {
                        String attrName = rdr.getAttributeLocalName(i);
                        String attrNS = rdr.getAttributeNamespace(i);
                        String attrVal = rdr.getAttributeValue(i);
                        if (attrNS == null) {

                            writer.writeAttribute(attrName, attrVal);

                        } else {

                            writer.writeAttribute(attrNS, attrName, attrVal);
                        }

                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    break;

                case XMLStreamConstants.ATTRIBUTE:
                    // do nothing
                    break;

                case XMLStreamConstants.CHARACTERS:
                    String txt = rdr.getText();
                    writer.writeCharacters(txt);
                    break;

                default:
                    // ignore
                    break;
                }
            }

        } finally {

            if (tmpFile != null && tmpFile.exists() && tmpFile.canWrite()) {

                tmpFile.delete();
            }
        }
    }
}

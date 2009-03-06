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

package org.apache.cxf.wstx_msv_validation;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.XmlSchemaSerializer.XmlSchemaSerializerException;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.validation.XMLValidationSchema;

/**
 * This class touches stax2 API, so it is kept separate to allow graceful
 * fallback.
 */
class Stax2ValidationUtils {
    
    /** {@inheritDoc}
     * @throws XMLStreamException */
    public void setupValidation(XMLStreamReader reader, 
                                XmlSchemaCollection schemas) throws XMLStreamException {
        XMLStreamReader2 reader2 = (XMLStreamReader2)reader;
        XMLValidationSchema vs = getValidator(schemas);
        reader2.validateAgainst(vs);


    }
    
    private Reader getSchemaAsStream(DOMSource source) {
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        try {
            TransformerFactory.newInstance().newTransformer().transform(source, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new StringReader(writer.toString());
    }

    /**
     * Create woodstox validator for a schema set.
     * @param schemas
     * @return
     * @throws XMLStreamException
     */
    private XMLValidationSchema getValidator(XmlSchemaCollection schemas) throws XMLStreamException {
        List<InputSource> sources = new ArrayList<InputSource>();
        XmlSchemaSerializer serializer = new XmlSchemaSerializer();
        for (XmlSchema sch : schemas.getXmlSchemas()) {
            Document[] serialized;
            try {
                serialized = serializer.serializeSchema(sch, false);
            } catch (XmlSchemaSerializerException e) {
                throw new RuntimeException(e);
            }
            DOMSource domSource = new DOMSource(serialized[0]);
            Reader schemaReader = getSchemaAsStream(domSource);
            InputSource inputSource = new InputSource(schemaReader);
            inputSource.setSystemId(sch.getSourceURI());
            sources.add(inputSource);
        }
        
        W3CMultiSchemaFactory factory = new W3CMultiSchemaFactory();
        XMLValidationSchema vs;
        vs = factory.loadSchemas(sources.toArray(new InputSource[sources.size()]));
        return vs;
    }

}

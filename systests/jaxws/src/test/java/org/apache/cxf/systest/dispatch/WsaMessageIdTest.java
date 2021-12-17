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

package org.apache.cxf.systest.dispatch;


import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.ws.addressing.AttributedURIType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class WsaMessageIdTest {

    /*
     * 3.1.4 White Space Normalization during Validation preserve No normalization is done, the value is the
     * ·normalized value· replace All occurrences of #x9 (tab), #xA (line feed) and #xD (carriage return) are
     * replaced > with #x20 (space). collapse Subsequent to the replacements specified above under replace,
     * contiguous sequences of #x20s are collapsed to a single #x20, and initial and/or final #x20s are
     * deleted.
     */
    @Test
    public void testCollapseNeeded() throws JAXBException, SAXException {
        for (String[] value : new String[][] {
            {
                "expected", " expected"
            }, {
                "expected", " expected "
            }, {
                "expected", " expected  "
            }, {
                "expected", "  expected"
            }, {
                "expected", "  expected  "
            }, {
                "expected expected", "  expected         expected"
            },
        }) {
            String expected = value[0];
            String actual = value[1];
            assertEquals("|" + expected + "|", "|" + unmarshalMessageIDValue(actual) + "|");
        }
    }   

    @Test
    public void testNoCollapseNeeded() throws JAXBException, SAXException {
        for (String value : Arrays.asList("", "http://cxf.com", "mailto:info@kimholland.nl",
                                          "../%C3%A9dition.html", "../édition.html",
                                          "http://corona.com/prod.html#shirt", "../prod.html#shirt",
                                          "urn:example:org")) {
            assertEquals(value, unmarshalMessageIDValue(value));
        }
    }

    @Test
    public void testSchemaValidationEnabled() throws JAXBException, SAXException {
        try {
            unmarshalMessageIDValue("##");
            fail();
        } catch (UnmarshalException cause) {
            assertTrue(cause.getLinkedException().getMessage()
                .contains("'##' is not a valid value for 'anyURI'"));
        }
    }

    private String unmarshalMessageIDValue(String messageIDValue) throws JAXBException, SAXException {
        String xmlString = "<MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">" + messageIDValue
                           + "</MessageID>";

        // Create unmarshaller
        JAXBContext jc = JAXBContext.newInstance("org.apache.cxf.ws.addressing");
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL wsAddrXsd = getClass().getResource("/schemas/wsdl/ws-addr.xsd");
        assertNotNull(wsAddrXsd);

        // Enable schema validation
        Schema schema = sf.newSchema(wsAddrXsd);
        unmarshaller.setSchema(schema);
        
        //unmarshaller.setAdapter(new StringTrimAdapter());
        // Unmarshal the MessageID XML-fragment
        JAXBElement je = (JAXBElement)unmarshaller.unmarshal(new StringReader(xmlString));
        assertEquals(AttributedURIType.class, je.getDeclaredType());
        assertTrue(je.getValue() instanceof AttributedURIType);
        AttributedURIType attributedURIType = (AttributedURIType)je.getValue();

        // Return unmarshalled value
        return attributedURIType.getValue();
    }

    
}

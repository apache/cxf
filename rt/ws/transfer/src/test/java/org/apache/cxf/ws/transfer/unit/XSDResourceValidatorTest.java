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

package org.apache.cxf.ws.transfer.unit;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.validationtransformation.XSDResourceValidator;

import org.junit.Assert;
import org.junit.Test;

public class XSDResourceValidatorTest {


    private Representation loadRepresentation(InputStream input) throws XMLStreamException {
        Document doc = StaxUtils.read(input);
        Representation representation = new Representation();
        representation.setAny(doc.getDocumentElement());
        return representation;
    }

    @Test
    public void validRepresentationTest() throws XMLStreamException {
        XSDResourceValidator validator = new XSDResourceValidator(
                new StreamSource(getClass().getResourceAsStream("/xml/xsdresourcevalidator/schema.xsd")));
        boolean result = validator.validate(loadRepresentation(
                getClass().getResourceAsStream("/xml/xsdresourcevalidator/validRepresentation.xml")), null);
        Assert.assertTrue(result);
    }

    @Test
    public void invalidRepresentationTest() throws XMLStreamException {
        XSDResourceValidator validator = new XSDResourceValidator(
                new StreamSource(getClass().getResourceAsStream("/xml/xsdresourcevalidator/schema.xsd")));
        boolean result = validator.validate(loadRepresentation(
                getClass().getResourceAsStream("/xml/xsdresourcevalidator/invalidRepresentation.xml")), null);
        Assert.assertFalse(result);
    }

}

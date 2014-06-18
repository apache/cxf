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

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.validationtransformation.ValidatorResult;
import org.apache.cxf.ws.transfer.validationtransformation.XSDResourceValidator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author erich
 */
public class XSDResourceValidatorTest {
    
    private static DocumentBuilderFactory documentBuilderFactory;
    
    private static DocumentBuilder documentBuilder;
    
    @BeforeClass
    public static void beforeClass() throws ParserConfigurationException {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
    }
    
    @AfterClass
    public static void afterClass() {
        documentBuilderFactory = null;
        documentBuilder = null;
    }
    
    private Representation loadRepresentation(InputStream input) throws SAXException, IOException {
        Document doc = documentBuilder.parse(input);
        Representation representation = new Representation();
        representation.setAny(doc.getDocumentElement());
        return representation;
    }
    
    @Test
    public void validRepresentationTest() throws SAXException, IOException {
        XSDResourceValidator validator = new XSDResourceValidator(
                new StreamSource(getClass().getResourceAsStream("/xml/xsdresourcevalidator/schema.xsd")));
        ValidatorResult result = validator.validate(loadRepresentation(
                getClass().getResourceAsStream("/xml/xsdresourcevalidator/validRepresentation.xml")));
        Assert.assertTrue(result.isValidate());
    }
    
    @Test
    public void invalidRepresentationTest() throws SAXException, IOException {
        XSDResourceValidator validator = new XSDResourceValidator(
                new StreamSource(getClass().getResourceAsStream("/xml/xsdresourcevalidator/schema.xsd")));
        ValidatorResult result = validator.validate(loadRepresentation(
                getClass().getResourceAsStream("/xml/xsdresourcevalidator/invalidRepresentation.xml")));
        Assert.assertFalse(result.isValidate());
    }
    
}

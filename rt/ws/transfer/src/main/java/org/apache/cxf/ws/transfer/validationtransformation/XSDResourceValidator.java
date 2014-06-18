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

package org.apache.cxf.ws.transfer.validationtransformation;

import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.apache.cxf.ws.transfer.Representation;

/**
 *
 * @author erich
 */
public class XSDResourceValidator implements ResourceValidator {

    protected ResourceTransformer resourceTransformer;
    
    protected Validator validator;
    
    public XSDResourceValidator(Source xsd, ResourceTransformer resourceTransformer) {
        try {
            this.resourceTransformer = resourceTransformer;
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(xsd);
            this.validator = schema.newValidator();
        } catch (SAXException ex) {
            throw new IllegalArgumentException("Error occured during creating the Validator.", ex);
        }
    }
    
    public XSDResourceValidator(Source xsd) {
        this(xsd, null);
    }
    
    @Override
    public ValidatorResult validate(Representation representation) {
        try {
            validator.validate(new DOMSource((Node) representation.getAny()));
        } catch (SAXException ex) {
            return new ValidatorResult(false, resourceTransformer);
        } catch (IOException ex) {
            throw new RuntimeException("Error occured during reading the XML representation.", ex);
        }
        return new ValidatorResult(true, resourceTransformer);
    }
    
}

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
package org.apache.cxf.aegis.type.streams;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.xml.XMLStreamReaderType;
import org.apache.cxf.common.util.SOAPConstants;
import org.junit.Before;
import org.junit.Test;

public class XMLStreamReaderMappingTest extends AbstractAegisTest {
    protected DefaultTypeMapping mapping;
    private AegisContext context;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        addNamespace("b", "urn:beanz");
        addNamespace("xsi", SOAPConstants.XSI_NS);

        context = new AegisContext();
        // create a different mapping than the context creates.
        TypeMapping baseMapping = DefaultTypeMapping.createSoap11TypeMapping(true, false);
        mapping = new DefaultTypeMapping(SOAPConstants.XSD, baseMapping);
        mapping.register(XMLStreamReader.class, 
                         new QName("urn:Bean", "SimpleBean"), new XMLStreamReaderType());
        mapping.setTypeCreator(context.createTypeCreator());
        context.setTypeMapping(mapping);
        context.initialize();
    }
    
    @Test
    public void testReadStream() throws Exception {
        // Test reading
        InputStream is = getResourceAsStream("bean1.xml");
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader inputReader = inputFactory.createXMLStreamReader(is);
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        Object what = reader.read(inputReader);
        assertTrue(what instanceof XMLStreamReader);
        XMLStreamReader beanReader = (XMLStreamReader) what;
        beanReader.nextTag();
        assertEquals("bleh", beanReader.getLocalName());
    }
    
    protected Context getContext() {
        AegisContext globalContext = new AegisContext();
        globalContext.initialize();
        globalContext.setTypeMapping(mapping);
        return new Context(globalContext);
    }

    
    
}

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
package org.apache.cxf.aegis.type.encoded;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.jdom.JDOMWriter;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.common.util.SOAPConstants;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Before;

public abstract class AbstractEncodedTest extends AbstractAegisTest {
    protected DefaultTypeMapping mapping;
    protected TrailingBlocks trailingBlocks;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        addNamespace("b", "urn:Bean");
        addNamespace("a", "urn:anotherns");
        addNamespace("xsi", SOAPConstants.XSI_NS);
        addNamespace("soapenc", Soap11.getInstance().getSoapEncodingStyle());

        AegisContext context = new AegisContext();
        // create a different mapping than the context creates.
        TypeMapping baseMapping = DefaultTypeMapping.createSoap11TypeMapping(true, false);
        mapping = new DefaultTypeMapping(SOAPConstants.XSD, baseMapping);
        mapping.setTypeCreator(context.createTypeCreator());
        context.setTypeMapping(mapping);
        context.initialize();
        // serialization root type
        trailingBlocks = new TrailingBlocks();
    }
    
    protected Context getContext() {
        AegisContext globalContext = new AegisContext();
        globalContext.initialize();
        globalContext.setTypeMapping(mapping);
        return new Context(globalContext);
    }

    public <T> T readWriteReadRef(String file, Class<T> typeClass) throws XMLStreamException {
        Context context = getContext();
        
        Type type = mapping.getType(typeClass);
        assertNotNull("no type found for " + typeClass.getName());

        // read file
        ElementReader reader = new ElementReader(getClass().getResourceAsStream(file));
        T value = typeClass.cast(type.readObject(reader, context));
        reader.getXMLStreamReader().close();

        // write value to element
        Element element = writeRef(value);

        // reread value from element
        value = typeClass.cast(readRef(element));

        return value;
    }

    public Object readRef(String file) throws XMLStreamException {
        ElementReader root = new ElementReader(getClass().getResourceAsStream(file));
        return readRef(root);
    }

    public Object readRef(Element element) throws XMLStreamException {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        String xml = xmlOutputter.outputString(element);
        ElementReader root = new ElementReader(new ByteArrayInputStream(xml.getBytes()));
        return readRef(root);
    }

    public Object readRef(ElementReader root) throws XMLStreamException {
        Context context = getContext();

        // get Type based on the element qname
        MessageReader reader = root.getNextElementReader();
        Type type = this.mapping.getType(reader.getName());
        assertNotNull("type is null", type);

        // read ref
        SoapRefType soapRefType = new SoapRefType(type);
        SoapRef ref = (SoapRef) soapRefType.readObject(reader, context);
        reader.readToEnd();

        // read the trailing blocks (referenced objects)
        List<Object> roots = trailingBlocks.readBlocks(root, context);
        assertNotNull(roots);

        // close the input stream
        root.getXMLStreamReader().close();

        // return the ref
        return ref.get();
    }

    public Element writeRef(Object instance) {
        Type type = mapping.getType(instance.getClass());
        assertNotNull("no type found for " + instance.getClass().getName());

        // create the document
        Element element = new Element("root", "b", "urn:Bean");
        for (Map.Entry<String, String> entry : getNamespaces().entrySet()) {
            element.addNamespaceDeclaration(Namespace.getNamespace(entry.getKey(), entry.getValue()));
        }
        new Document(element);
        JDOMWriter rootWriter = new JDOMWriter(element);
        Context context = getContext();

        // get Type based on the object instance
        assertNotNull("type is null", type);

        // write the ref
        SoapRefType soapRefType = new SoapRefType(type);
        MessageWriter cwriter = rootWriter.getElementWriter(soapRefType.getSchemaType());
        soapRefType.writeObject(instance, cwriter, context);
        cwriter.close();

        // write the trailing blocks (referenced objects)
        trailingBlocks.writeBlocks(rootWriter, context);

        // log xml for debugging
        // XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        // System.out.println(xmlOutputter.outputString(element)) ;

        return element;
    }

    public void verifyInvalid(String resourceName, Class<?> expectedType) throws XMLStreamException {
        Type type = mapping.getType(expectedType);
        assertNotNull("type is null", type);

        Context context = getContext();

        ElementReader reader = new ElementReader(getClass().getResourceAsStream(resourceName));
        try {
            type.readObject(reader, context);
            fail("expected DatabindingException");
        } catch (DatabindingException expected) {
            // expected
        } finally {
            reader.getXMLStreamReader().close();
        }
    }

    public static void validateShippingAddress(Address address) {
        assertNotNull(address);
        assertEquals("1234 Riverside Drive", address.getStreet());
        assertEquals("Gainesville", address.getCity());
        assertEquals("FL", address.getState());
        assertEquals("30506", address.getZip());
    }

    public static void validateBillingAddress(Address billing) {
        assertNotNull("billing is null", billing);
        assertEquals("1234 Fake Street", billing.getStreet());
        assertEquals("Las Vegas", billing.getCity());
        assertEquals("NV", billing.getState());
        assertEquals("89102", billing.getZip());
    }
}

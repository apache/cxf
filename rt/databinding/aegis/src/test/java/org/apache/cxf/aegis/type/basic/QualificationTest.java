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
package org.apache.cxf.aegis.type.basic;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.services.AttributeBean;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.xml.jdom.JDOMWriter;
import org.apache.cxf.common.util.SOAPConstants;
import org.jdom.Document;
import org.jdom.Element;
import org.junit.Test;

public class QualificationTest extends AbstractAegisTest {

    public void setUp() throws Exception {
        super.setUp();
    
        addNamespace("b", "urn:Bean");
        addNamespace("xyzzy", "urn:xyzzy");
        addNamespace("xsi", SOAPConstants.XSI_NS);
    }
    
    @Test
    public void testDefaultUnqualifiedAttribute() throws Exception {
        AegisContext context = new AegisContext();
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        
        Type type = mapping.getTypeCreator().createType(AttributeBean.class);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Context messageContext = new Context(context);
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        AttributeBean bean = new AttributeBean();
        type.writeObject(bean, new JDOMWriter(element), messageContext);
        assertValid("/b:root[@xyzzy:attrExplicitString]", element);
        assertXPathEquals("/b:root/@xyzzy:attrExplicitString", "attrExplicit", element);
        assertValid("/b:root[@attrPlainString]", element);
        assertXPathEquals("/b:root/@attrPlainString", "attrPlain", element);
    }
    
    @Test
    public void testDefaultQualifiedAttribute() throws Exception {
        AegisContext context = new AegisContext();
        TypeCreationOptions typeCreationOptions = 
            new TypeCreationOptions();
        typeCreationOptions.setQualifyAttributes(true);
        context.setTypeCreationOptions(typeCreationOptions);
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        
        Type type = mapping.getTypeCreator().createType(AttributeBean.class);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Context messageContext = new Context(context);
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        AttributeBean bean = new AttributeBean();
        type.writeObject(bean, new JDOMWriter(element), messageContext);
        assertValid("/b:root[@xyzzy:attrExplicitString]", element);
        assertXPathEquals("/b:root/@xyzzy:attrExplicitString", "attrExplicit", element);
        assertValid("/b:root[@attrPlainString]", element);
        assertXPathEquals("/b:root/@attrPlainString", "attrPlain", element);
    }
}

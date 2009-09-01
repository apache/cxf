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

import org.w3c.dom.Element;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.services.AttributeBean;
import org.apache.cxf.aegis.services.XmlMappedAttributeBean;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.common.util.SOAPConstants;

import org.junit.Test;

public class QualificationTest extends AbstractAegisTest {

    public void setUp() throws Exception {
        super.setUp();
    
        addNamespace("b", "urn:Bean");
        addNamespace("xyzzy", "urn:xyzzy");
        addNamespace("pkg", "http://services.aegis.cxf.apache.org");
        addNamespace("xsi", SOAPConstants.XSI_NS);
    }
    
    @Test
    public void testAnnotatedDefaultUnqualifiedAttribute() throws Exception {
        AegisContext context = new AegisContext();
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        
        AegisType type = mapping.getTypeCreator().createType(AttributeBean.class);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Context messageContext = new Context(context);
        AttributeBean bean = new AttributeBean();
        Element element = writeObjectToElement(type, bean, messageContext);
        assertValid("/b:root[@xyzzy:attrExplicitString]", element);
        assertXPathEquals("/b:root/@xyzzy:attrExplicitString", "attrExplicit", element);
        assertValid("/b:root[@attrPlainString]", element);
        assertXPathEquals("/b:root/@attrPlainString", "attrPlain", element);
    }
    
    @Test
    public void testAnnotatedDefaultQualifiedAttribute() throws Exception {
        AegisContext context = new AegisContext();
        TypeCreationOptions typeCreationOptions = 
            new TypeCreationOptions();
        typeCreationOptions.setQualifyAttributes(true);
        context.setTypeCreationOptions(typeCreationOptions);
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        
        AegisType type = mapping.getTypeCreator().createType(AttributeBean.class);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Context messageContext = new Context(context);
        AttributeBean bean = new AttributeBean();
        Element element = writeObjectToElement(type, bean, messageContext);
        assertValid("/b:root[@xyzzy:attrExplicitString]", element);
        assertXPathEquals("/b:root/@xyzzy:attrExplicitString", "attrExplicit", element);
        assertValid("/b:root[@pkg:attrPlainString]", element);
        assertXPathEquals("/b:root/@pkg:attrPlainString", "attrPlain", element);
    }
    @Test
    public void testXmlDefaultUnqualifiedAttribute() throws Exception {
        AegisContext context = new AegisContext();
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        
        AegisType type = mapping.getTypeCreator().createType(XmlMappedAttributeBean.class);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Context messageContext = new Context(context);
        XmlMappedAttributeBean bean = new XmlMappedAttributeBean();
        
        Element element = writeObjectToElement(type, bean, messageContext);
        assertValid("/b:root[@attrXmlString]", element);
        assertXPathEquals("/b:root/@attrXmlString", "attrXml", element);
    }
    
    @Test
    public void testXmlDefaultQualifiedAttribute() throws Exception {
        AegisContext context = new AegisContext();
        TypeCreationOptions typeCreationOptions = 
            new TypeCreationOptions();
        typeCreationOptions.setQualifyAttributes(true);
        context.setTypeCreationOptions(typeCreationOptions);
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        
        AegisType type = mapping.getTypeCreator().createType(XmlMappedAttributeBean.class);
        type.setSchemaType(new QName("urn:Bean", "bean"));

        Context messageContext = new Context(context);
        XmlMappedAttributeBean bean = new XmlMappedAttributeBean();

        Element element = writeObjectToElement(type, bean, messageContext);
        assertValid("/b:root[@pkg:attrXmlString]", element);
        assertXPathEquals("/b:root/@pkg:attrXmlString", "attrXml", element);
    }


}

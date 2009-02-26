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
package org.apache.cxf.aegis.xmlconfig;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.test.TestUtilities;

import org.junit.Before;
import org.junit.Test;

public class TestDateMapping {
    private AegisContext context;
    private TestUtilities testUtilities;
    private XMLOutputFactory xmlOutputFactory;
    
    @Before
    public void before() {
        testUtilities = new TestUtilities(getClass());
        testUtilities.addNamespace("test", "urn:test");
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }
    
    @Test
    public void testWriteSqlDateAsDate() throws Exception {
        context = new AegisContext();
        Set<Class<?>> rootClasses = new HashSet<Class<?>>();
        rootClasses.add(BeanWithDate.class);
        context.setRootClasses(rootClasses);
        context.initialize();
        BeanWithDate bean = new BeanWithDate();
        java.sql.Date date = new java.sql.Date(0);
        bean.setFig(date);
        Type sbType = context.getTypeMapping().getType(bean.getClass());
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
        writer.write(bean, new QName("urn:test", "beanWithDate"),
                          false, xmlWriter, sbType);
        xmlWriter.close();
        // an absence of exception is success here.
    }
    
    @Test
    public void testWriteCustomTypeSchemaType() throws Exception {
        context = new AegisContext();
        Set<Class<?>> rootClasses = new HashSet<Class<?>>();
        rootClasses.add(BeanWithDate.class);
        context.setRootClasses(rootClasses);
        context.initialize();
        BeanWithDate bean = new BeanWithDate();
        java.sql.Date date = new java.sql.Date(0);
        bean.setFig(date);
        Type sbType = context.getTypeMapping().getType(bean.getClass());
     /* will explode if the type object created for the custom mapping isn't fully initialized.
      */
        sbType.writeSchema(null);
    }
}

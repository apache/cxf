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
package org.apache.cxf.aegis.type.java5;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.XMLTypeCreator;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;

import org.junit.Before;
import org.junit.Test;

public class ConfigurationTest extends AbstractAegisTest {

    DefaultTypeMapping tm;

    TypeCreationOptions config;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AegisContext context = new AegisContext();
        config = new TypeCreationOptions();
        context.setTypeCreationOptions(config);
        context.initialize();
        XMLTypeCreator creator = new XMLTypeCreator();
        creator.setConfiguration(config);
        Java5TypeCreator next = new Java5TypeCreator();
        next.setConfiguration(config);
        creator.setNextCreator(next);

        tm = (DefaultTypeMapping)context.getTypeMapping();
        tm.setTypeCreator(creator);
    }

    @Test
    public void testNillableDefaultTrue() throws Exception {
        config.setDefaultNillable(true);

        AegisType type = tm.getTypeCreator().createType(AnnotatedBean1.class);
        BeanTypeInfo info = ((BeanType)type).getTypeInfo();

        assertTrue(info.isNillable(new QName(info.getDefaultNamespace(), "bogusProperty")));
    }

    @Test
    public void testNillableDefaultFalse() throws Exception {
        config.setDefaultNillable(false);
        AegisType type = tm.getTypeCreator().createType(AnnotatedBean1.class);
        BeanTypeInfo info = ((BeanType)type).getTypeInfo();

        assertFalse(info.isNillable(new QName(info.getDefaultNamespace(), "bogusProperty")));
    }

    @Test
    public void testMinOccursDefault0() throws Exception {
        config.setDefaultMinOccurs(0);
        AegisType type = tm.getTypeCreator().createType(AnnotatedBean1.class);
        BeanTypeInfo info = ((BeanType)type).getTypeInfo();

        assertEquals(info.getMinOccurs(new QName(info.getDefaultNamespace(), "bogusProperty")), 0);
    }

    @Test
    public void testMinOccursDefault1() throws Exception {
        config.setDefaultMinOccurs(1);
        AegisType type = tm.getTypeCreator().createType(AnnotatedBean1.class);
        BeanTypeInfo info = ((BeanType)type).getTypeInfo();

        assertEquals(info.getMinOccurs(new QName(info.getDefaultNamespace(), "bogusProperty")), 1);
    }

    @Test
    public void testExtensibleDefaultTrue() throws Exception {
        config.setDefaultExtensibleElements(true);
        config.setDefaultExtensibleAttributes(true);
        AegisType type = tm.getTypeCreator().createType(AnnotatedBean1.class);
        BeanTypeInfo info = ((BeanType)type).getTypeInfo();
        assertTrue(info.isExtensibleElements());
        assertTrue(info.isExtensibleAttributes());
    }

    @Test
    public void testExtensibleDefaultFalse() throws Exception {
        config.setDefaultExtensibleElements(false);
        config.setDefaultExtensibleAttributes(false);
        AegisType type = tm.getTypeCreator().createType(AnnotatedBean1.class);
        BeanTypeInfo info = ((BeanType)type).getTypeInfo();
        assertFalse(info.isExtensibleElements());
        assertFalse(info.isExtensibleAttributes());
    }

}

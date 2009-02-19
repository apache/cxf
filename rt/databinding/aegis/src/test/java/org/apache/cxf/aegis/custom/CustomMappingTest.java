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
package org.apache.cxf.aegis.custom;

import java.util.GregorianCalendar;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.ws.commons.schema.XmlSchema;

import org.junit.Test;

/**
 * See CXF-1788. Ensure that a mapping that of a type that inherits from a type with a default mapping works.
 */
public class CustomMappingTest extends AbstractAegisTest {
    @Test
    public void testInheritedMapping() throws Exception {
        BeanTypeInfo bti = new BeanTypeInfo(GregorianCalendar.class, "http://util.java");
        BeanType beanType = new BeanType(bti);
        beanType.setSchemaType(new QName("http://util.java{GregorianCalendar}"));
        AegisContext context = new AegisContext();
        context.initialize();
        TypeMapping mapping = context.getTypeMapping();
        // we are replacing the default mapping.
        mapping.register(beanType);
        XmlSchema schema = newXmlSchema("http://util.java");
        beanType.writeSchema(schema);
        // well, test?
    }

}

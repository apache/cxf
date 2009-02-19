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
package org.apache.cxf.aegis.standalone;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.test.TestUtilities;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaSerializer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SchemaAddinsTest extends Assert {
    private TestUtilities testUtilities;
    
    @Before
    public void before() {
        testUtilities = new TestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
    }
    
    @Test
    public void testAegisTypeSchema() throws Exception {
        AegisContext context = new AegisContext();
        context.initialize();
        XmlSchemaCollection collection = new XmlSchemaCollection();
        context.addTypesSchemaDocument(collection);
        XmlSchema[] schemas = collection.getXmlSchemas();
        XmlSchema typeSchema = null;
        for (XmlSchema schema : schemas) {
            if (AegisContext.UTILITY_TYPES_SCHEMA_NS.equals(schema.getTargetNamespace())) {
                typeSchema = schema;
                break;
            }
        }
        assertNotNull(typeSchema);
        
        assertNotSame(0, typeSchema.getItems().getCount());
        XmlSchemaSerializer serializer = new XmlSchemaSerializer();
        Document[] docs = serializer.serializeSchema(typeSchema, false);
        testUtilities.assertValid("/xsd:schema/xsd:simpleType[@name='char']", docs[0]);
        
        
        
    }
}

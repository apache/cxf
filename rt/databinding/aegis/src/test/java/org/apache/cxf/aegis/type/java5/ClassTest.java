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

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.basic.StringType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ClassTest extends AbstractAegisTest {
    Server server;

    @Before
    public void startServer() throws Exception {
        AegisContext context = new AegisContext();
        context.initialize();
        context.getTypeMapping().register(new ClassAsStringType());

        ServerFactoryBean b = new ServerFactoryBean();
        b.setDataBinding(new AegisDatabinding(context));
        b.setServiceClass(GenericsService.class);
        b.setAddress("local://GenericsService");
        server = b.create();
    }
    @After
    public void stopServer() {
        server.stop();
        server.destroy();
        server = null;
    }

    @Test
    public void testType() throws Exception {
        Document doc = getWSDLDocument("GenericsService");
        assertNotNull(doc);
        this.assertValidBoolean("//xsd:simpleType[@name='class']/xsd:restriction", doc.getDocumentElement());
    }

    public static class GenericsService {

        public <T extends Serializable> T createInstance(Class<T> type) 
            throws Exception {
            return type.getDeclaredConstructor().newInstance();
        }
    }


    public static class ClassAsStringType extends AegisType {

        public static final QName CLASS_AS_STRING_TYPE_QNAME
            = new QName("http://cxf.apache.org/my/class/test", "class");

        private StringType stringType;

        public ClassAsStringType() {
            stringType = new StringType();
            super.setTypeClass(Class.class);
            super.setSchemaType(CLASS_AS_STRING_TYPE_QNAME);
        }

        public Object readObject(MessageReader reader, Context context)
            throws DatabindingException {
            String className = (String) stringType.readObject(reader, context);
            Class<?> cls = null;
            try {
                context.getClass().getClassLoader().loadClass(className);
            } catch (ClassNotFoundException x) {
                throw new DatabindingException("Unable to dynamically load class '"
                    + className + "'", x);
            }
            return cls;
        }

        public void writeObject(Object object, MessageWriter writer, Context context)
            throws DatabindingException {
            if (object == null) {
                stringType.writeObject(null, writer, context);
            } else {
                Class<?> cls = (Class<?>) object;
                stringType.writeObject(cls.getName(), writer, context);
            }
        }
        public void writeSchema(XmlSchema root) {
            XmlSchemaSimpleType xst = new XmlSchemaSimpleType(root, true);
            xst.setName("class");

            XmlSchemaSimpleTypeRestriction content = new XmlSchemaSimpleTypeRestriction();
            content.setBaseTypeName(Constants.XSD_STRING);
            xst.setContent(content);
        }

        public boolean usesUtilityTypes() {
            return true;
        }
    }

}
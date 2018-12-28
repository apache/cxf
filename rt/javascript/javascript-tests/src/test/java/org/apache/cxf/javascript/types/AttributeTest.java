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

package org.apache.cxf.javascript.types;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.javascript.BasicNameManager;
import org.apache.cxf.javascript.JavascriptTestUtilities;
import org.apache.cxf.javascript.NameManager;
import org.apache.cxf.javascript.NamespacePrefixAccumulator;
import org.apache.cxf.javascript.fortest.AttributeTestBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.test.AbstractCXFSpringTest;
import org.springframework.context.support.GenericApplicationContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AttributeTest extends AbstractCXFSpringTest {
    private JavascriptTestUtilities testUtilities;
    private XMLOutputFactory xmlOutputFactory;
    private Client client;
    private List<ServiceInfo> serviceInfos;
    private Collection<SchemaInfo> schemata;
    private NameManager nameManager;
    private JaxWsProxyFactoryBean clientProxyFactory;

    public AttributeTest() throws Exception {
        testUtilities = new JavascriptTestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
        xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:attributeTestBeans.xml"};
    }

    @Test
    public void testDeserialization() throws Exception {
        setupClientAndRhino("attribute-test-proxy-factory");
        testUtilities.readResourceIntoRhino("/attributeTests.js");
        DataBinding dataBinding = new JAXBDataBinding(AttributeTestBean.class);
        assertNotNull(dataBinding);
        AttributeTestBean bean = new AttributeTestBean();
        bean.element1 = "e1";
        bean.element2 = "e2";
        bean.attribute1 = "a1";
        bean.attribute2 = "a2";

        String serialized = serializeObject(dataBinding, bean);
        testUtilities.rhinoCallInContext("deserializeAttributeTestBean", serialized);
    }

    private String serializeObject(DataBinding dataBinding,
                                   AttributeTestBean bean) throws XMLStreamException {
        DataWriter<XMLStreamWriter> writer = dataBinding.createWriter(XMLStreamWriter.class);
        StringWriter stringWriter = new StringWriter();
        XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(stringWriter);
        writer.write(bean, xmlStreamWriter);
        xmlStreamWriter.flush();
        xmlStreamWriter.close();
        return stringWriter.toString();
    }

    private void setupClientAndRhino(String clientProxyFactoryBeanId) throws IOException {
        testUtilities.setBus(getBean(Bus.class, "cxf"));

        testUtilities.initializeRhino();
        testUtilities.readResourceIntoRhino("/org/apache/cxf/javascript/cxf-utils.js");

        clientProxyFactory = getBean(JaxWsProxyFactoryBean.class, clientProxyFactoryBeanId);
        client = clientProxyFactory.getClientFactoryBean().create();
        serviceInfos = client.getEndpoint().getService().getServiceInfos();
        // there can only be one.
        assertEquals(1, serviceInfos.size());
        ServiceInfo serviceInfo = serviceInfos.get(0);
        schemata = serviceInfo.getSchemas();
        nameManager = BasicNameManager.newNameManager(serviceInfo);
        NamespacePrefixAccumulator prefixAccumulator =
            new NamespacePrefixAccumulator(serviceInfo.getXmlSchemaCollection());
        for (SchemaInfo schema : schemata) {
            SchemaJavascriptBuilder builder = new SchemaJavascriptBuilder(serviceInfo
                .getXmlSchemaCollection(), prefixAccumulator, nameManager);
            String allThatJavascript = builder.generateCodeForSchema(schema.getSchema());
            assertNotNull(allThatJavascript);
            testUtilities.readStringIntoRhino(allThatJavascript, schema.toString() + ".js");
        }
    }

    @Override
    protected void additionalSpringConfiguration(GenericApplicationContext context) throws Exception {
    }
}
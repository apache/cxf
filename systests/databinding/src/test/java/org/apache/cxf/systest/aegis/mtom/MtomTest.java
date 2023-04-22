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

package org.apache.cxf.systest.aegis.mtom;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.activation.DataHandler;
import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.mtom.AbstractXOPType;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.aegis.mtom.fortest.DataHandlerBean;
import org.apache.cxf.systest.aegis.mtom.fortest.MtomTestImpl;
import org.apache.cxf.systest.aegis.mtom.fortest.MtomTestService;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.ws.commons.schema.constants.Constants;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import org.junit.Assert;
import org.junit.Test;


/**
 *
 */
@ContextConfiguration(locations = { "classpath:mtomTestBeans.xml" })
public class MtomTest extends AbstractJUnit4SpringContextTests {
    static final String PORT = TestUtil.getPortNumber(MtomTest.class);

    private MtomTestImpl impl;
    private MtomTestService client;
    private MtomTestService jaxwsClient;
    private TestUtilities testUtilities;

    public MtomTest() {
        testUtilities = new TestUtilities(getClass());
    }

    private void setupForTest(boolean enableClientMTOM) throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        aegisBinding.setMtomEnabled(enableClientMTOM);
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setDataBinding(aegisBinding);
        proxyFac.setAddress("http://localhost:" + PORT + "/mtom");

        JaxWsProxyFactoryBean jaxwsFac = new JaxWsProxyFactoryBean();
        jaxwsFac.setDataBinding(new AegisDatabinding());
        jaxwsFac.setAddress("http://localhost:" + PORT + "/jaxWsMtom");

        Map<String, Object> props = new HashMap<>();
        if (enableClientMTOM) {
            props.put("mtom-enabled", Boolean.TRUE);
        }
        proxyFac.setProperties(props);

        client = proxyFac.create(MtomTestService.class);
        jaxwsClient = jaxwsFac.create(MtomTestService.class);
        impl = (MtomTestImpl)applicationContext.getBean("mtomImpl");
    }

    @Test
    public void testMtomReply() throws Exception {
        setupForTest(true);
        DataHandlerBean dhBean = client.produceDataHandlerBean();
        Assert.assertNotNull(dhBean);
        String result = IOUtils.toString(dhBean.getDataHandler().getInputStream());
        Assert.assertEquals(MtomTestImpl.STRING_DATA, result);
    }

    //TODO: how do we see if MTOM actually happened?
    @Test
    public void testJaxWsMtomReply() throws Exception {
        setupForTest(true);
        DataHandlerBean dhBean = jaxwsClient.produceDataHandlerBean();
        Assert.assertNotNull(dhBean);
        String result = IOUtils.toString(dhBean.getDataHandler().getInputStream());
        Assert.assertEquals(MtomTestImpl.STRING_DATA, result);
    }

    @Test
    public void testAcceptDataHandler() throws Exception {
        setupForTest(true);
        DataHandlerBean dhBean = new DataHandlerBean();
        dhBean.setName("some name");
        // some day, we might need this to be higher than some threshold.
        String someData = "This is the cereal shot from guns.";
        DataHandler dataHandler = new DataHandler(someData, "text/plain;charset=utf-8");
        dhBean.setDataHandler(dataHandler);
        client.acceptDataHandler(dhBean);
        DataHandlerBean accepted = impl.getLastDhBean();
        Assert.assertNotNull(accepted);
        Object o = accepted.getDataHandler().getContent();
        String data = null;
        if (o instanceof String) {
            data = (String)o;
        } else if (o instanceof InputStream) {
            data = IOUtils.toString((InputStream)o);
        }
        Assert.assertNotNull(data);
        Assert.assertEquals("This is the cereal shot from guns.", data);
    }

    @Test
    public void testAcceptDataHandlerNoMTOM() throws Exception {
        setupForTest(false);
        DataHandlerBean dhBean = new DataHandlerBean();
        dhBean.setName("some name");
        // some day, we might need this to be longer than some threshold.
        String someData = "This is the cereal shot from guns.";
        DataHandler dataHandler = new DataHandler(someData, "text/plain;charset=utf-8");
        dhBean.setDataHandler(dataHandler);
        client.acceptDataHandler(dhBean);
        DataHandlerBean accepted = impl.getLastDhBean();
        Assert.assertNotNull(accepted);
        InputStream data = accepted.getDataHandler().getInputStream();
        Assert.assertNotNull(data);
        String dataString = IOUtils.toString(data);
        Assert.assertEquals("This is the cereal shot from guns.", dataString);
    }

    @Test
    public void testMtomSchema() throws Exception {
        testUtilities.setBus((Bus)applicationContext.getBean("cxf"));
        testUtilities.addDefaultNamespaces();
        testUtilities.addNamespace("xmime", "http://www.w3.org/2005/05/xmlmime");
        Server s = testUtilities.
            getServerForService(new QName("http://fortest.mtom.aegis.systest.cxf.apache.org/",
                                          "MtomTestService"));
        Document wsdl = testUtilities.getWSDLDocument(s);
        Assert.assertNotNull(wsdl);
        NodeList typeAttrList =
            testUtilities.assertValid("//xsd:complexType[@name='inputDhBean']/xsd:sequence/"
                                      + "xsd:element[@name='dataHandler']/"
                                      + "@type",
                                      wsdl);
        Attr typeAttr = (Attr)typeAttrList.item(0);
        String typeAttrValue = typeAttr.getValue();
        // now, this thing is a qname with a :, and we have to work out if it's correct.
        String[] pieces = typeAttrValue.split(":");
        Assert.assertEquals("base64Binary", pieces[1]);
        Node elementNode = typeAttr.getOwnerElement();
        String url = testUtilities.resolveNamespacePrefix(pieces[0], elementNode);
        Assert.assertEquals(Constants.URI_2001_SCHEMA_XSD, url);

        s = testUtilities.getServerForAddress("http://localhost:" + PORT + "/mtomXmime");
        wsdl = testUtilities.getWSDLDocument(s);
        Assert.assertNotNull(wsdl);
        typeAttrList =
            testUtilities.assertValid("//xsd:complexType[@name='inputDhBean']/xsd:sequence/"
                                      + "xsd:element[@name='dataHandler']/"
                                      + "@type",
                                      wsdl);
        typeAttr = (Attr)typeAttrList.item(0);
        typeAttrValue = typeAttr.getValue();
        // now, this thing is a qname with a :, and we have to work out if it's correct.
        pieces = typeAttrValue.split(":");
        Assert.assertEquals("base64Binary", pieces[1]);
        elementNode = typeAttr.getOwnerElement();
        url = testUtilities.resolveNamespacePrefix(pieces[0], elementNode);
        Assert.assertEquals(AbstractXOPType.XML_MIME_NS, url);

        /* when I add a test for a custom mapping.
        testUtilities.assertValid("//xsd:complexType[@name='inputDhBean']/xsd:sequence/"
                                  + "xsd:element[@name='dataHandler']/"
                                  + "@xmime:expectedContentType/text()",
                                  wsdl);
                                  */
    }
}

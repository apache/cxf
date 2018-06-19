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

package org.apache.cxf.systest.aegis;

import java.net.URL;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import org.junit.Assert;
import org.junit.Test;


/**
 *
 */
@ContextConfiguration(locations = { "classpath:aegisWSDLNSBeans.xml" })
public class AegisWSDLNSTest extends AbstractJUnit4SpringContextTests {
    static final String PORT = TestUtil.getPortNumber(AegisWSDLNSTest.class);

    private AegisJaxWsWsdlNs client;

    public AegisWSDLNSTest() {
    }

    private void setupForTest(boolean specifyWsdl) throws Exception {

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(AegisJaxWsWsdlNs.class);
        if (specifyWsdl) {
            factory.setServiceName(new QName("http://v1_1_2.rtf2pdf.doc.ws.daisy.marbes.cz",
                                             "AegisJaxWsWsdlNsImplService"));
            factory.setWsdlLocation("http://localhost:" + PORT + "/aegisJaxWsWSDLNS?wsdl");
        }
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        factory.setAddress("http://localhost:" + PORT + "/aegisJaxWsWSDLNS");
        client = (AegisJaxWsWsdlNs)factory.create();
    }

    @Test
    public void testWithInterface() throws Exception {
        setupForTest(false);
        AegisJaxWsWsdlNs.VO vo = new AegisJaxWsWsdlNs.VO();
        vo.setStr("ffang");
        client.updateVO(vo);
    }

    @Test
    public void testWithWsdl() throws Exception {
        setupForTest(true);
        AegisJaxWsWsdlNs.VO vo = new AegisJaxWsWsdlNs.VO();
        vo.setStr("ffang");
        client.updateVO(vo);
    }

    @Test
    public void testGeneratedWsdlNs() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/aegisJaxWsWSDLNS?wsdl");
        Document dom = StaxUtils.read(url.openStream());
        TestUtilities util = new TestUtilities(this.getClass());
        util.addDefaultNamespaces();
        util.assertValid(
                         "//wsdl:definitions[@targetNamespace"
                         + "='http://v1_1_2.rtf2pdf.doc.ws.daisy.marbes.cz']",
                         dom);
        //should be a targetNamespace for "http://wo.rtf2pdf.doc.ws.daisy.marbes.cz"
        //as VO type specified in the SEI
        util.assertValid("//wsdl:definitions/wsdl:types/xsd:schema[@targetNamespace"
                         + "='http://wo.rtf2pdf.doc.ws.daisy.marbes.cz']",
                         dom);
    }

    @Test
    public void testUsingCorrectMethod() throws Exception {
        setupForTest(false);
        Integer result = client.updateInteger(Integer.valueOf(20));
        Assert.assertEquals(result.intValue(), 20);
    }

}

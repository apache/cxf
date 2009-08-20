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

package org.apache.cxf.ws.policy;

import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.wsdl.Definition;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;

import org.junit.Assert;


/**
 * 
 */
public class PolicyAnnotationTest extends Assert {

    @org.junit.Test
    public void testAnnotations() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new TestImpl());
        factory.setStart(false);
        Server s = factory.create();
        Bus bus = factory.getBus();
        try {
            ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus,
                                                                s.getEndpoint().getService()
                                                                    .getServiceInfos());
            Definition def = builder.build();
            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                .getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            Element wsdl = wsdlWriter.getDocument(def).getDocumentElement();
            
            Map<String, String> ns = new HashMap<String, String>();
            ns.put("wsdl", WSDLConstants.NS_WSDL11);
            ns.put("wsu", 
                   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            ns.put("wsp", PolicyConstants.NAMESPACE_XMLSOAP_200409);
            XPathUtils xpu = new XPathUtils(ns);
            //org.apache.cxf.helpers.XMLUtils.printDOM(wsdl);
            check(xpu, wsdl, "/wsdl:definitions/wsdl:service/", "TestImplServiceServicePolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:service/wsdl:port", "TestImplPortPortPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/", "TestInterfacePortTypePolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/wsdl:operation/", "echoIntPortTypeOpPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/wsdl:operation/wsdl:input",
                  "echoIntPortTypeOpInputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/wsdl:operation/wsdl:output",
                  "echoIntPortTypeOpOutputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/", "TestImplServiceSoapBindingBindingPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/wsdl:operation/", "echoIntBindingOpPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:input",
                  "echoIntBindingOpInputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:output",
                  "echoIntBindingOpOutputPolicy");
        } finally {
            bus.shutdown(true);
        }
    }
    
    private void check(XPathUtils xpu, Element wsdl, String path, String uri) {
        assertTrue(uri + " not found",
                   xpu.isExist("/wsdl:definitions/wsp:Policy[@wsu:Id='" + uri + "']",
                              wsdl,
                              XPathConstants.NODE));
        assertTrue(uri + " reference not found",
               xpu.isExist(path + "/wsp:PolicyReference[@URI='#" + uri + "']",
                          wsdl,
                          XPathConstants.NODE));
        
    }
    
    @Policies({
        @Policy(uri = "annotationpolicies/TestInterfacePolicy.xml"),
        @Policy(uri = "annotationpolicies/TestImplPolicy.xml",
                placement = Policy.Placement.SERVICE_PORT),
        @Policy(uri = "annotationpolicies/TestPortTypePolicy.xml", 
                placement = Policy.Placement.PORT_TYPE)
    }
    )
    @WebService
    public static interface TestInterface {
        @Policies({
            @Policy(uri = "annotationpolicies/TestOperationPolicy.xml"),
            @Policy(uri = "annotationpolicies/TestOperationInputPolicy.xml", 
                    placement = Policy.Placement.BINDING_OPERATION_INPUT),
            @Policy(uri = "annotationpolicies/TestOperationOutputPolicy.xml", 
                    placement = Policy.Placement.BINDING_OPERATION_OUTPUT),
            @Policy(uri = "annotationpolicies/TestOperationPTPolicy.xml", 
                    placement = Policy.Placement.PORT_TYPE_OPERATION),
            @Policy(uri = "annotationpolicies/TestOperationPTInputPolicy.xml", 
                    placement = Policy.Placement.PORT_TYPE_OPERATION_INPUT),
            @Policy(uri = "annotationpolicies/TestOperationPTOutputPolicy.xml", 
                    placement = Policy.Placement.PORT_TYPE_OPERATION_OUTPUT)
        }
        )
        int echoInt(int i);
    }
    
    
    @Policies({
        @Policy(uri = "annotationpolicies/TestImplPolicy.xml")
    }
    )
    @WebService()
    public static class TestImpl implements TestInterface {
        public int echoInt(int i) {
            return i;
        }
    }
}

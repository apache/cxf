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

package org.apache.cxf.jaxws.ws;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.wsdl.Definition;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.neethi.Constants;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 *
 */
public class PolicyAnnotationTest {

    @org.junit.Test
    public void testAnnotations() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestImpl());
        factory.setStart(false);
        List<String> tp = Arrays.asList(
            "http://schemas.xmlsoap.org/soap/http",
            "http://schemas.xmlsoap.org/wsdl/http/",
            "http://schemas.xmlsoap.org/wsdl/soap/http",
            "http://www.w3.org/2003/05/soap/bindings/HTTP/",
            "http://cxf.apache.org/transports/http/configuration",
            "http://cxf.apache.org/bindings/xformat");

        LocalTransportFactory f = new LocalTransportFactory();
        f.getUriPrefixes().add("http");
        f.setTransportIds(tp);

        Server s = factory.create();

        try {
            ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus,
                                                                s.getEndpoint().getService()
                                                                    .getServiceInfos());
            Definition def = builder.build();
            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                .getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            Element wsdl = wsdlWriter.getDocument(def).getDocumentElement();

            Map<String, String> ns = new HashMap<>();
            ns.put("wsdl", WSDLConstants.NS_WSDL11);
            ns.put("wsu",
                   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            ns.put("wsp", Constants.URI_POLICY_13_NS);
            XPathUtils xpu = new XPathUtils(ns);
            //org.apache.cxf.helpers.XMLUtils.printDOM(wsdl);
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
            check(xpu, wsdl, "/wsdl:definitions/wsdl:service/", "TestImplServiceServicePolicy");

            assertEquals(1,
                         xpu.getValueList("/wsdl:definitions/wsdl:binding/wsdl:operation/"
                                              + "wsp:PolicyReference[@URI='#echoIntBindingOpPolicy']", wsdl)
                             .getLength());

            EndpointPolicy policy = bus.getExtension(PolicyEngine.class)
                .getServerEndpointPolicy(s.getEndpoint().getEndpointInfo(), null, null);
            assertNotNull(policy);
            assertEquals(1, policy.getChosenAlternative().size());
        } finally {
            bus.shutdown(true);
        }
    }

    @org.junit.Test
    public void testAnnotationsInterfaceAsClass() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestImpl());
        factory.setServiceClass(TestInterface.class);
        factory.setStart(false);
        List<String> tp = Arrays.asList(
            "http://schemas.xmlsoap.org/soap/http",
            "http://schemas.xmlsoap.org/wsdl/http/",
            "http://schemas.xmlsoap.org/wsdl/soap/http",
            "http://www.w3.org/2003/05/soap/bindings/HTTP/",
            "http://cxf.apache.org/transports/http/configuration",
            "http://cxf.apache.org/bindings/xformat");

        LocalTransportFactory f = new LocalTransportFactory();
        f.getUriPrefixes().add("http");
        f.setTransportIds(tp);


        Server s = factory.create();

        try {
            ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus,
                                                                s.getEndpoint().getService()
                                                                    .getServiceInfos());
            Definition def = builder.build();
            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class)
                .getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            Element wsdl = wsdlWriter.getDocument(def).getDocumentElement();

            Map<String, String> ns = new HashMap<>();
            ns.put("wsdl", WSDLConstants.NS_WSDL11);
            ns.put("wsu",
                   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            ns.put("wsp", Constants.URI_POLICY_13_NS);
            XPathUtils xpu = new XPathUtils(ns);
            //org.apache.cxf.helpers.XMLUtils.printDOM(wsdl);
            check(xpu, wsdl, "/wsdl:definitions/wsdl:service/wsdl:port", "TestInterfacePortPortPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/", "TestInterfacePortTypePolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/wsdl:operation/", "echoIntPortTypeOpPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/wsdl:operation/wsdl:input",
                  "echoIntPortTypeOpInputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:portType/wsdl:operation/wsdl:output",
                  "echoIntPortTypeOpOutputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/",
                  "TestInterfaceServiceSoapBindingBindingPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/wsdl:operation/", "echoIntBindingOpPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:input",
                  "echoIntBindingOpInputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:binding/wsdl:operation/wsdl:output",
                  "echoIntBindingOpOutputPolicy");
            check(xpu, wsdl, "/wsdl:definitions/wsdl:service/", "TestInterfaceServiceServicePolicy");

            assertEquals(1,
                         xpu.getValueList("/wsdl:definitions/wsdl:binding/wsdl:operation/"
                                              + "wsp:PolicyReference[@URI='#echoIntBindingOpPolicy']", wsdl)
                             .getLength());

        } finally {
            bus.shutdown(true);
        }
    }

    @org.junit.Test
    public void testAnnotationImplNoInterface() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestImplNoInterface());
        factory.setStart(false);
        List<String> tp = Arrays.asList("http://schemas.xmlsoap.org/soap/http", "http://schemas.xmlsoap.org/wsdl/http/",
                "http://schemas.xmlsoap.org/wsdl/soap/http", "http://www.w3.org/2003/05/soap/bindings/HTTP/",
                "http://cxf.apache.org/transports/http/configuration", "http://cxf.apache.org/bindings/xformat");

        LocalTransportFactory f = new LocalTransportFactory();
        f.getUriPrefixes().add("http");
        f.setTransportIds(tp);

        Server s = factory.create();

        try {
            ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, s.getEndpoint().getService().getServiceInfos());
            Definition def = builder.build();
            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            Element wsdl = wsdlWriter.getDocument(def).getDocumentElement();

            Map<String, String> ns = new HashMap<>();
            ns.put("wsdl", WSDLConstants.NS_WSDL11);
            ns.put("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            ns.put("wsp", Constants.URI_POLICY_13_NS);
            XPathUtils xpu = new XPathUtils(ns);

            // org.apache.cxf.helpers.XMLUtils.printDOM(wsdl);
            assertEquals(1,
                    xpu.getValueList("/wsdl:definitions/wsdl:binding/"
                        + "wsp:PolicyReference[@URI='#TestImplNoInterfaceServiceSoapBindingBindingPolicy']", wsdl)
                        .getLength());
            final EndpointPolicy policy = bus.getExtension(PolicyEngine.class)
                    .getServerEndpointPolicy(s.getEndpoint().getEndpointInfo(), null, null);
            assertNotNull(policy);
        } finally {
            bus.shutdown(true);
        }
    }

    @org.junit.Test
    public void testAnnotationImplNoInterfacePolicies() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceBean(new TestImplWithPoliciesNoInterface());
        factory.setStart(false);
        List<String> tp = Arrays.asList("http://schemas.xmlsoap.org/soap/http", "http://schemas.xmlsoap.org/wsdl/http/",
                "http://schemas.xmlsoap.org/wsdl/soap/http", "http://www.w3.org/2003/05/soap/bindings/HTTP/",
                "http://cxf.apache.org/transports/http/configuration", "http://cxf.apache.org/bindings/xformat");

        LocalTransportFactory f = new LocalTransportFactory();
        f.getUriPrefixes().add("http");
        f.setTransportIds(tp);

        Server s = factory.create();

        try {
            ServiceWSDLBuilder builder = new ServiceWSDLBuilder(bus, s.getEndpoint().getService().getServiceInfos());
            Definition def = builder.build();
            WSDLWriter wsdlWriter = bus.getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter();
            def.setExtensionRegistry(bus.getExtension(WSDLManager.class).getExtensionRegistry());
            Element wsdl = wsdlWriter.getDocument(def).getDocumentElement();

            Map<String, String> ns = new HashMap<>();
            ns.put("wsdl", WSDLConstants.NS_WSDL11);
            ns.put("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
            ns.put("wsp", Constants.URI_POLICY_13_NS);
            XPathUtils xpu = new XPathUtils(ns);

            // org.apache.cxf.helpers.XMLUtils.printDOM(wsdl);
            assertEquals(1,
                    xpu.getValueList("/wsdl:definitions/wsdl:binding/"
                    + "wsp:PolicyReference[@URI='#TestImplWithPoliciesNoInterfaceServiceSoapBindingBindingPolicy']",
                            wsdl).getLength());
            final EndpointPolicy policy = bus.getExtension(PolicyEngine.class)
                    .getServerEndpointPolicy(s.getEndpoint().getEndpointInfo(), null, null);
            assertNotNull(policy);
        } finally {
            bus.shutdown(true);
        }
    }

    private void check(XPathUtils xpu, Element wsdl, String path, String uri) {
        assertTrue(uri + " not found",
                   xpu.isExist("/wsdl:definitions/wsp:Policy[@wsu:Id='" + uri + "']",
                              wsdl,
                              XPathConstants.NODE));
        assertEquals(1, xpu.getValueList("/wsdl:definitions/wsp:Policy[@wsu:Id='" + uri + "']",
                         wsdl).getLength());
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
    public interface TestInterface {
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
    @WebService(endpointInterface = "org.apache.cxf.jaxws.ws.PolicyAnnotationTest$TestInterface")
    public static class TestImpl implements TestInterface {
        public int echoInt(int i) {
            return i;
        }
    }

    @WebService()
    @Policy(placement = Policy.Placement.BINDING, uri = "annotationpolicies/TestImplPolicy.xml")
    public static class TestImplNoInterface {
        @WebMethod
        public int echoInt(int i) {
            return i;
        }
    }

    @WebService()
    @Policies({
        @Policy(placement = Policy.Placement.BINDING, uri = "annotationpolicies/TestImplPolicy.xml")
     }
    )
    public static class TestImplWithPoliciesNoInterface {
        @WebMethod
        public int echoInt(int i) {
            return i;
        }
    }
}
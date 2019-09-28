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

package org.apache.cxf.systest.jaxws;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.anonymous_complex_type.AnonymousComplexType;
import org.apache.cxf.anonymous_complex_type.AnonymousComplexTypeService;
import org.apache.cxf.anonymous_complex_type.RefSplitName;
import org.apache.cxf.anonymous_complex_type.RefSplitNameResponse;
import org.apache.cxf.anonymous_complex_type.SplitName;
import org.apache.cxf.anonymous_complex_type.SplitNameResponse.Names;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.xml.XMLBinding;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxb_element_test.JaxbElementTest;
import org.apache.cxf.jaxb_element_test.JaxbElementTest_Service;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.ordered_param_holder.ComplexStruct;
import org.apache.cxf.ordered_param_holder.OrderedParamHolder;
import org.apache.cxf.ordered_param_holder.OrderedParamHolder_Service;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.CXF2411Result;
import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.CXF2411SubClass;
import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.Foo;
import org.apache.cxf.systest.jaxws.cxf5064.HeaderObj;
import org.apache.cxf.systest.jaxws.cxf5064.SOAPHeaderSEI;
import org.apache.cxf.tests.inherit.Inherit;
import org.apache.cxf.tests.inherit.InheritService;
import org.apache.cxf.tests.inherit.objects.SubTypeA;
import org.apache.cxf.tests.inherit.objects.SubTypeB;
import org.apache.cxf.tests.inherit.types.ObjectInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.wsdl.WSDLConstants;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClientServerMiscTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ServerMisc.class);


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(ServerMisc.class, true));
    }

    @Test
    public void testWSDLDocs() throws Exception {
        Map<String, String> ns = new HashMap<>();
        ns.put("wsdl", WSDLConstants.NS_WSDL11);
        XPathUtils xpu = new XPathUtils(ns);
        Document wsdl = StaxUtils.read(this.getHttpConnection(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl")
                                          .getInputStream());
        //XMLUtils.printDOM(wsdl.getDocumentElement());
        assertEquals("DocLitWrappedCodeFirstService impl",
                     xpu.getValue("/wsdl:definitions/wsdl:service/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("DocLitWrappedCodeFirstService interface",
                     xpu.getValue("/wsdl:definitions/wsdl:portType/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("DocLitWrappedCodeFirstService top level doc",
                     xpu.getValue("/wsdl:definitions/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("DocLitWrappedCodeFirstService binding doc",
                     xpu.getValue("/wsdl:definitions/wsdl:binding/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("DocLitWrappedCodeFirstService service/port doc",
                     xpu.getValue("/wsdl:definitions/wsdl:service/wsdl:port/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut doc",
                     xpu.getValue("/wsdl:definitions/wsdl:portType/wsdl:operation[@name='multiInOut']"
                                  + "/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut Input doc",
                     xpu.getValue("/wsdl:definitions/wsdl:portType/wsdl:operation[@name='multiInOut']"
                                  + "/wsdl:input/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut Output doc",
                     xpu.getValue("/wsdl:definitions/wsdl:portType/wsdl:operation[@name='multiInOut']"
                                  + "/wsdl:output/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut InputMessage doc",
                     xpu.getValue("/wsdl:definitions/wsdl:message[@name='multiInOut']"
                                  + "/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut OutputMessage doc",
                     xpu.getValue("/wsdl:definitions/wsdl:message[@name='multiInOutResponse']"
                                  + "/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut binding doc",
                     xpu.getValue("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='multiInOut']"
                                  + "/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut binding Input doc",
                     xpu.getValue("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='multiInOut']"
                                  + "/wsdl:input/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("multiInOut binding Output doc",
                     xpu.getValue("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='multiInOut']"
                                  + "/wsdl:output/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("fault binding doc",
                     xpu.getValue("/wsdl:definitions/wsdl:binding/wsdl:operation[@name='throwException']"
                                  + "/wsdl:fault/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("fault porttype doc",
                     xpu.getValue("/wsdl:definitions/wsdl:portType/wsdl:operation[@name='throwException']"
                                  + "/wsdl:fault/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
        assertEquals("fault message doc",
                     xpu.getValue("/wsdl:definitions/wsdl:message[@name='CustomException']"
                                  + "/wsdl:documentation",
                                  wsdl.getDocumentElement(),
                                  XPathConstants.STRING));
    }

    @Test
    public void testDocLitBare() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitBareCodeFirstService",
            "DocLitBareCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitBareCodeFirstService",
            "DocLitBareCodeFirstService");

        //try without wsdl
        Service service = Service.create(servName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING,
                        ServerMisc.DOCLITBARE_CODEFIRST_URL);
        DocLitBareCodeFirstService port = service.getPort(portName,
                                  DocLitBareCodeFirstService.class);
        DocLitBareCodeFirstService.GreetMeRequest req =
            new DocLitBareCodeFirstService.GreetMeRequest();
        DocLitBareCodeFirstService.GreetMeResponse resp;
        BigInteger[] i;

        req.setName("Foo");
        resp = port.greetMe(req);
        assertEquals(req.getName(), resp.getName());

        i = port.sayTest(new DocLitBareCodeFirstService.SayTestRequest("Dan"));
        assertEquals(4, i.length);
        assertEquals(0, i[0].intValue());
        assertEquals(1, i[1].intValue());
        assertEquals(2, i[2].intValue());
        assertEquals(3, i[3].intValue());

        //try with wsdl
        service = Service.create(new URL(ServerMisc.DOCLITBARE_CODEFIRST_URL + "?wsdl"),
                                         servName);
        port = service.getPort(portName, DocLitBareCodeFirstService.class);
        resp = port.greetMe(req);
        assertEquals(req.getName(), resp.getName());

        //try the fault
        req.setName("fault");
        try {
            resp = port.greetMe(req);
            fail("did not get fault back");
        } catch (SOAPFaultException ex) {
            assertEquals("mr.actor", ex.getFault().getFaultActor());
            assertEquals("test", ex.getFault().getDetail().getFirstChild().getLocalName());
        }
        req.setName("emptyfault");
        try {
            resp = port.greetMe(req);
            fail("did not get fault back");
        } catch (SOAPFaultException ex) {
            assertNull(ex.getFault().getDetail());
        }

        //This test will pass/fail completely dependent on the versions of JAXB picked up and what
        //version of Xerces is picked up.  Newer versions of JAXB will generate "--11Z" (correct per XMLSchema)
        //older version will generate "--11--Z".  Xerces will only parse "--11--Z".  The parser in the JDK
        //will depend on the JDK version used.
        /*
        GMonthTest gm = new GMonthTest();
        XMLGregorianCalendar dt = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2010, 11, 16, 0);
        gm.setValue(dt);
        GMonthTest gm2 = port.echoGMonthTest(gm);
        assertEquals(gm.getValue().getMonth(), gm2.getValue().getMonth());
        */
    }


    @Test
    public void testAnonymousComplexType() throws Exception {

        AnonymousComplexTypeService actService = new AnonymousComplexTypeService();
        assertNotNull(actService);
        QName portName = new QName("http://cxf.apache.org/anonymous_complex_type/",
            "anonymous_complex_typeSOAP");
        AnonymousComplexType act = actService.getPort(portName, AnonymousComplexType.class);
        updateAddressPort(act, PORT);
        try {
            Names reply = act.splitName("Tom Li");
            assertNotNull("no response received from service", reply);
            assertEquals("Tom", reply.getFirst());
            assertEquals("Li", reply.getSecond());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testRefAnonymousComplexType() throws Exception {

        AnonymousComplexTypeService actService = new AnonymousComplexTypeService();
        assertNotNull(actService);
        QName portName = new QName("http://cxf.apache.org/anonymous_complex_type/",
            "anonymous_complex_typeSOAP");
        AnonymousComplexType act = actService.getPort(portName, AnonymousComplexType.class);
        updateAddressPort(act, PORT);

        try {
            SplitName name = new SplitName();
            name.setName("Tom Li");
            RefSplitName refName = new RefSplitName();
            refName.setSplitName(name);
            RefSplitNameResponse reply = act.refSplitName(refName);
            assertNotNull("no response received from service", reply);
            assertEquals("Tom", reply.getSplitNameResponse().getNames().getFirst());
            assertEquals("Li", reply.getSplitNameResponse().getNames().getSecond());
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testMinOccursAndNillableJAXBElement() throws Exception {

        JaxbElementTest_Service service = new JaxbElementTest_Service();
        assertNotNull(service);
        JaxbElementTest port = service.getPort(JaxbElementTest.class);
        updateAddressPort(port, PORT);

        try {

            String response = port.newOperation("hello");
            assertNotNull(response);
            assertEquals("in=hello", response);

            response = port.newOperation(null);
            assertNotNull(response);
            assertEquals("in=null", response);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testOrderedParamHolder() throws Exception {
        OrderedParamHolder_Service service = new OrderedParamHolder_Service();
        OrderedParamHolder port = service.getOrderedParamHolderSOAP();
        updateAddressPort(port, PORT);

        try {
            Holder<ComplexStruct> part3 = new Holder<>();
            part3.value = new ComplexStruct();
            part3.value.setElem1("elem1");
            part3.value.setElem2("elem2");
            part3.value.setElem3(0);
            Holder<Integer> part2 = new Holder<>();
            part2.value = 0;
            Holder<String> part1 = new Holder<>();
            part1.value = "part1";

            port.orderedParamHolder(part3, part2, part1);

            assertNotNull(part3.value);
            assertEquals("check value", "return elem1", part3.value.getElem1());
            assertEquals("check value", "return elem2", part3.value.getElem2());
            assertEquals("check value", 1, part3.value.getElem3());
            assertNotNull(part2.value);
            assertEquals("check value", 1, part2.value.intValue());
            assertNotNull(part1.value);
            assertEquals("check value", "return part1", part1.value);

        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test
    public void testMissingMethods() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                "DocLitWrappedCodeFirstService");

        Service service = Service.create(new URL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl"),
                      servName);
        DocLitWrappedCodeFirstServiceMissingOps port = service.getPort(portName,
                                  DocLitWrappedCodeFirstServiceMissingOps.class);

        ((BindingProvider)port).getEndpointReference();
        int[] ret = port.echoIntArray(new int[] {1, 2});
        assertNotNull(ret);
        ret = port.echoIntArray(new int[] {1, 2});
        assertNotNull(ret);
        ret = port.echoIntArray(new int[] {1, 2});
        assertNotNull(ret);
        //port.arrayOutput();
    }

    @Test
    public void testDocLitWrappedCodeFirstServiceNoWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                   "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                   "DocLitWrappedCodeFirstService");

        Service service = Service.create(servName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.DOCLIT_CODEFIRST_URL);
        DocLitWrappedCodeFirstService port = service.getPort(portName,
                                                             DocLitWrappedCodeFirstService.class);
        runDocLitTest(port);
    }

    @Test
    public void testDocLitWrappedCodeFirstServiceWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                   "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                   "DocLitWrappedCodeFirstService");

        Service service = Service.create(new URL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl"),
                                         servName);
        DocLitWrappedCodeFirstService port = service.getPort(portName,
                                                             DocLitWrappedCodeFirstService.class);
        runDocLitTest(port);
    }

    @Test
    public void testWrappedHolderOutNull() throws Exception {
        // this test to verify CXF-3836 works
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                   "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                   "DocLitWrappedCodeFirstService");

        Service service = Service.create(new URL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl"), servName);
        DocLitWrappedCodeFirstService port = service.getPort(portName, DocLitWrappedCodeFirstService.class);
        Holder<Boolean> created = new Holder<>();
        port.singleInOut(created);
        assertFalse(created.value);
    }

    private void setASM(boolean b) throws Exception {
        Field f = ASMHelper.class.getDeclaredField("badASM");
        ReflectionUtil.setAccessible(f);
        f.set(null, !b);
    }

    @Test
    public void testDocLitWrappedCodeFirstServiceNoWsdlNoASM() throws Exception {
        try {
            setASM(false);
            QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                       "DocLitWrappedCodeFirstServicePort");
            QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                       "DocLitWrappedCodeFirstService");

            Service service = Service.create(servName);
            service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.DOCLIT_CODEFIRST_URL);
            DocLitWrappedCodeFirstService port = service.getPort(portName,
                                                                 DocLitWrappedCodeFirstService.class);
            runDocLitTest(port);
        } finally {
            setASM(true);
        }
    }

    @Test
    public void testDocLitWrappedCodeFirstServiceWsdlNoASM() throws Exception {
        try {
            setASM(false);
            QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                       "DocLitWrappedCodeFirstServicePort");
            QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
                                       "DocLitWrappedCodeFirstService");

            Service service = Service.create(new URL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl"),
                                             servName);
            DocLitWrappedCodeFirstService port = service.getPort(portName,
                                                                 DocLitWrappedCodeFirstService.class);
            runDocLitTest(port);
        } finally {
            setASM(true);
        }
    }



    @Test
    public void testSimpleClientWithWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
            "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
            "DocLitWrappedCodeFirstService");

        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setWsdlURL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl");
        factory.setServiceName(servName);
        factory.setServiceClass(DocLitWrappedCodeFirstService.class);
        factory.setEndpointName(portName);

        DocLitWrappedCodeFirstService port = (DocLitWrappedCodeFirstService) factory.create();
        assertNotNull(port);

        String echoMsg = port.echo("Hello");
        assertEquals("Hello", echoMsg);
    }

    @Test
    public void testSimpleClientWithWsdlAndBindingId() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
            "DocLitWrappedCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService",
            "DocLitWrappedCodeFirstService");

        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setBindingId("http://cxf.apache.org/bindings/xformat");
        factory.setWsdlURL(ServerMisc.DOCLIT_CODEFIRST_URL_XMLBINDING + "?wsdl");
        factory.setServiceName(servName);
        factory.setServiceClass(DocLitWrappedCodeFirstService.class);
        factory.setEndpointName(portName);
        factory.setAddress(ServerMisc.DOCLIT_CODEFIRST_URL_XMLBINDING);
        DocLitWrappedCodeFirstService port = (DocLitWrappedCodeFirstService) factory.create();
        assertNotNull(port);
        assertEquals(factory.getBindingId(), "http://cxf.apache.org/bindings/xformat");
        assertTrue(ClientProxy.getClient(port).getEndpoint().getBinding() instanceof XMLBinding);

        String echoMsg = port.echo("Hello");
        assertEquals("Hello", echoMsg);
    }

    private void runDocLitTest(DocLitWrappedCodeFirstService port) throws Exception {
        assertEquals("snarf", port.doBug2692("snarf"));
        CXF2411Result<CXF2411SubClass> o = port.doCXF2411();
        assertNotNull(o);
        assertNotNull(o.getContent());
        Object[] ar = o.getContent();
        assertTrue(ar[0] instanceof CXF2411SubClass);
        Foo foo = new Foo();
        foo.setName("blah");
        assertEquals("blah", port.modifyFoo(foo).getName());

        assertEquals("hello", port.outOnly(new Holder<String>(), new Holder<String>()));

        long start = System.currentTimeMillis();
        port.doOneWay();
        assertTrue((System.currentTimeMillis() - start) < 500);

        assertEquals("Hello", port.echoStringNotReallyAsync("Hello"));

        Set<Foo> fooSet = port.getFooSet();
        assertEquals(2, fooSet.size());
        assertEquals("size: 2", port.doFooList(new ArrayList<>(fooSet)));

        assertEquals(24, port.echoIntDifferentWrapperName(24));

        String echoMsg = port.echo("Hello");
        assertEquals("Hello", echoMsg);

        List<String> rev = new ArrayList<>(Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA));
        Collections.reverse(rev);

        String s;

        String[] arrayOut = port.arrayOutput();
        assertNotNull(arrayOut);
        assertEquals(3, arrayOut.length);
        for (int x = 0; x < 3; x++) {
            assertEquals(DocLitWrappedCodeFirstServiceImpl.DATA[x], arrayOut[x]);
        }

        List<String> listOut = port.listOutput();
        assertNotNull(listOut);
        assertEquals(3, listOut.size());
        for (int x = 0; x < 3; x++) {
            assertEquals(DocLitWrappedCodeFirstServiceImpl.DATA[x], listOut.get(x));
        }

        s = port.arrayInput(DocLitWrappedCodeFirstServiceImpl.DATA);
        assertEquals("string1string2string3", s);
        s = port.listInput(java.util.Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA));
        assertEquals("string1string2string3", s);

        s = port.multiListInput(Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA),
                                rev,
                                "Hello", 24);
        assertEquals("string1string2string3string3string2string1Hello24", s);


        s = port.listInput(new ArrayList<>());
        assertEquals("", s);

        s = port.listInput(null);
        assertEquals("", s);

        s = port.multiListInput(Arrays.asList(DocLitWrappedCodeFirstServiceImpl.DATA),
                                        rev,
                                        null, 24);
        assertEquals("string1string2string3string3string2string1<null>24", s);

        Holder<String> a = new Holder<>();
        Holder<String> b = new Holder<>("Hello");
        Holder<String> c = new Holder<>();
        Holder<String> d = new Holder<>(" ");
        Holder<String> e = new Holder<>("world!");
        Holder<String> f = new Holder<>();
        Holder<String> g = new Holder<>();
        s = port.multiInOut(a, b, c, d, e, f, g);
        assertEquals("Hello world!", s);
        assertEquals("a", a.value);
        assertEquals("b", b.value);
        assertEquals("c", c.value);
        assertEquals("d", d.value);
        assertEquals("e", e.value);
        assertEquals("f", f.value);
        assertEquals("g", g.value);

        List<Foo> foos = port.listObjectOutput();
        assertEquals(2, foos.size());
        assertEquals("a", foos.get(0).getName());
        assertEquals("b", foos.get(1).getName());

        List<Foo[]> foos2 = port.listObjectArrayOutput();
        assertNotNull(foos2);
        assertEquals(2, foos2.size());
        assertEquals(2, foos2.get(0).length);
        assertEquals(2, foos2.get(1).length);

        int[] ints = port.echoIntArray(new int[] {1, 2, 3}, null);
        assertEquals(3, ints.length);
        assertEquals(1, ints[0]);

        if (new ASMHelper().createClassWriter() != null) {
            //doing the type adapter things and such really
            //requires the ASM generated helper classes
            assertEquals("Val", port.createBar("Val").getName());
        }
        testExceptionCases(port);
    }

    private void testExceptionCases(DocLitWrappedCodeFirstService port) throws Exception {
        /*   CXF-926 test case */
        try {
            port.throwException(10);
            fail("Expected exception not found");
        } catch (ServiceTestFault ex) {
            assertEquals(10L, ex.getFaultInfo().getId());
        }
        // CXF-1131 testcase
        try {
            port.throwException(-1);
            fail("Expected exception not found");
        } catch (ServiceTestFault ex) {
            assertNull(ex.getFaultInfo());
        }
        // CXF-1136 testcase
        try {
            port.throwException(-2);
            fail("Expected exception not found");
        } catch (CustomException ex) {
            assertEquals("CE: -2", ex.getMessage());
            assertEquals("A Value", ex.getA());
            assertEquals("B Value", ex.getB());
        }
        // CXF-1407
        try {
            port.throwException(-3);
            fail("Expected exception not found");
        } catch (ComplexException ex) {
            assertEquals("Throw user fault -3", ex.getMessage());
            assertEquals(3, ex.getInts().length);
        }
        try {
            port.throwException(-3);
            fail("Expected exception not found");
        } catch (ComplexException ex) {
            assertEquals("Throw user fault -3", ex.getMessage());
        }

        try {
            port.throwException(-4);
            fail("Expected exception not found");
        } catch (WebServiceException ex) {
            assertEquals("RuntimeException!!", ex.getMessage());
        }
        try {
            Foo foo = new Foo();
            foo.setNameIgnore("DoNoName");
            port.modifyFoo(foo);
            fail("Expected exception not found");
        } catch (SOAPFaultException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("NoName is not a valid name"));
        }
        try {
            Foo foo = new Foo();
            foo.setNameIgnore("NoName");
            port.modifyFoo(foo);
            fail("Expected exception not found");
        } catch (SOAPFaultException ex) {
            assertTrue(ex.getMessage().contains("NoName is not a valid name"));
        }


    }


    @Test
    public void testRpcLitNoWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService",
                                   "RpcLitCodimlpementor6eFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService",
                                   "RpcLitCodeFirstService");

        Service service = Service.create(servName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.RPCLIT_CODEFIRST_URL);
        RpcLitCodeFirstService port = service.getPort(portName,
                                                      RpcLitCodeFirstService.class);
        runRpcLitTest(port);
    }


    @Test
    public void testRpcLitWsdl() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService",
            "RpcLitCodeFirstServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService",
            "RpcLitCodeFirstService");

        Service service = Service.create(new URL(ServerMisc.RPCLIT_CODEFIRST_URL + "?wsdl"),
                                         servName);
        RpcLitCodeFirstService port = service.getPort(portName,
                                                      RpcLitCodeFirstService.class);
        runRpcLitTest(port);
    }

    private void runRpcLitTest(RpcLitCodeFirstService port) throws Exception {

        String[] ret = port.convertToString(new int[] {1, 2, 3});
        assertEquals(3, ret.length);

        List<String> rev = new ArrayList<>(Arrays.asList(RpcLitCodeFirstServiceImpl.DATA));
        Collections.reverse(rev);

        String s;

        String[] arrayOut = port.arrayOutput();
        assertNotNull(arrayOut);
        assertEquals(3, arrayOut.length);
        for (int x = 0; x < 3; x++) {
            assertEquals(RpcLitCodeFirstServiceImpl.DATA[x], arrayOut[x]);
        }

        List<String> listOut = port.listOutput();
        assertNotNull(listOut);
        assertEquals(3, listOut.size());
        for (int x = 0; x < 3; x++) {
            assertEquals(RpcLitCodeFirstServiceImpl.DATA[x], listOut.get(x));
        }

        s = port.arrayInput(RpcLitCodeFirstServiceImpl.DATA);
        assertEquals("string1string2string3", s);
        s = port.listInput(java.util.Arrays.asList(RpcLitCodeFirstServiceImpl.DATA));
        assertEquals("string1string2string3", s);

        s = port.multiListInput(Arrays.asList(RpcLitCodeFirstServiceImpl.DATA),
                                rev,
                                "Hello", 24);
        assertEquals("string1string2string3string3string2string1Hello24", s);


        s = port.listInput(new ArrayList<>());
        assertEquals("", s);

        try {
            s = port.listInput(null);
            fail("RPC/Lit parts cannot be null");
        } catch (SOAPFaultException ex) {
            //ignore, expected
        }

        try {
            s = port.multiListInput(Arrays.asList(RpcLitCodeFirstServiceImpl.DATA),
                                            rev,
                                            null, 24);
            fail("RPC/Lit parts cannot be null");
        } catch (SOAPFaultException ex) {
            //ignore, expected
        }

        Holder<String> a = new Holder<>();
        Holder<String> b = new Holder<>("Hello");
        Holder<String> c = new Holder<>();
        Holder<String> d = new Holder<>(" ");
        Holder<String> e = new Holder<>("world!");
        Holder<String> f = new Holder<>();
        Holder<String> g = new Holder<>();
        s = port.multiInOut(a, b, c, d, e, f, g);
        assertEquals("Hello world!", s);
        assertEquals("a", a.value);
        assertEquals("b", b.value);
        assertEquals("c", c.value);
        assertEquals("d", d.value);
        assertEquals("e", e.value);
        assertEquals("f", f.value);
        assertEquals("g", g.value);

        a = new Holder<>();
        b = new Holder<>("Hello");
        c = new Holder<>();
        d = new Holder<>(" ");
        e = new Holder<>("world!");
        f = new Holder<>();
        g = new Holder<>();
        s = port.multiHeaderInOut(a, b, c, d, e, f, g);
        assertEquals("Hello world!", s);
        assertEquals("a", a.value);
        assertEquals("b", b.value);
        assertEquals("c", c.value);
        assertEquals("d", d.value);
        assertEquals("e", e.value);
        assertEquals("f", f.value);
        assertEquals("g", g.value);

        List<org.apache.cxf.systest.jaxws.RpcLitCodeFirstService.Foo> foos = port.listObjectOutput();
        assertEquals(2, foos.size());
        assertEquals("a", foos.get(0).getName());
        assertEquals("b", foos.get(1).getName());

        List<org.apache.cxf.systest.jaxws.RpcLitCodeFirstService.Foo[]> foos2 = port.listObjectArrayOutput();
        assertNotNull(foos2);
        assertEquals(2, foos2.size());
        assertEquals(2, foos2.get(0).length);
        assertEquals(2, foos2.get(1).length);

    }

    @Test
    public void testInheritedTypesInOtherPackage() throws Exception {
        InheritService serv = new InheritService();
        Inherit port = serv.getInheritPort();
        updateAddressPort(port, PORT);
        ObjectInfo obj = port.getObject(0);
        assertNotNull(obj);
        assertNotNull(obj.getBaseObject());
        assertEquals("A", obj.getBaseObject().getName());
        assertTrue(obj.getBaseObject() instanceof SubTypeA);

        obj = port.getObject(1);
        assertNotNull(obj);
        assertNotNull(obj.getBaseObject());
        assertEquals("B", obj.getBaseObject().getName());
        assertTrue(obj.getBaseObject() instanceof SubTypeB);

    }

    @Test
    public void testInterfaceExtension() throws Exception {
        QName portName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstBaseService",
            "DocLitWrappedCodeFirstBaseServicePort");
        QName servName = new QName("http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstBaseService",
            "DocLitWrappedCodeFirstBaseService");

        //try without wsdl
        Service service = Service.create(servName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.DOCLIT_CODEFIRST_BASE_URL);
        DocLitWrappedCodeFirstBaseService port = service.getPort(portName,
                                  DocLitWrappedCodeFirstBaseService.class);
        assertEquals(1, port.operationInBase(1));
        assertEquals(2, port.operationInSub1(2));
        assertEquals(3, port.operationInSub2(3));

        //try with wsdl
        service = Service.create(new URL(ServerMisc.DOCLIT_CODEFIRST_BASE_URL + "?wsdl"),
                                         servName);
        port = service.getPort(portName, DocLitWrappedCodeFirstBaseService.class);
        assertEquals(1, port.operationInBase(1));
        assertEquals(2, port.operationInSub1(2));
        assertEquals(3, port.operationInSub2(3));
    }



    @Test
    public void testAnonymousMinOccursConfig() throws Exception {
        HttpURLConnection httpConnection =
            getHttpConnection(ServerMisc.DOCLIT_CODEFIRST_SETTINGS_URL + "?wsdl");
        httpConnection.connect();

        assertEquals(200, httpConnection.getResponseCode());
        assertEquals("OK", httpConnection.getResponseMessage());
        InputStream in = httpConnection.getInputStream();
        assertNotNull(in);

        Document doc = StaxUtils.read(in);
        assertNotNull(doc);


        Map<String, String> ns = new HashMap<>();
        ns.put("soap", Soap11.SOAP_NAMESPACE);
        ns.put("tns", "http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService");
        ns.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        ns.put("xs", "http://www.w3.org/2001/XMLSchema");


        XPathUtils xu = new XPathUtils(ns);

        //make sure the wrapper types are anonymous types
        Node ct = (Node) xu.getValue("//wsdl:definitions/wsdl:types/xs:schema"
                                     + "/xs:element[@name='getFooSetResponse']/xs:complexType/xs:sequence",
                                     doc, XPathConstants.NODE);
        assertNotNull(ct);

        //make sure the params are nillable, not minOccurs=0
        ct = (Node) xu.getValue("//wsdl:definitions/wsdl:types/xs:schema"
                                + "/xs:element[@name='multiInOut']/xs:complexType/xs:sequence"
                                + "/xs:element[@nillable='true']",
                                doc, XPathConstants.NODE);
        assertNotNull(ct);
    }

    @Test
    public void testDynamicClientExceptions() throws Exception {
        JaxWsDynamicClientFactory dcf =
            JaxWsDynamicClientFactory.newInstance();
        URL wsdlURL = new URL(ServerMisc.DOCLIT_CODEFIRST_URL + "?wsdl");
        Client client = dcf.createClient(wsdlURL);
        try {
            client.invoke("throwException", -2);
        } catch (Exception ex) {
            Object o = ex.getClass().getMethod("getFaultInfo").invoke(ex);
            assertNotNull(o);
        }

        try {
            client.getRequestContext().put("disable-fault-mapping", true);
            client.invoke("throwException", -2);
        } catch (SoapFault ex) {
            //expected
        }

    }


    @Test
    public void testCXF5064() throws Exception {
        URL url = new URL(ServerMisc.CXF_5064_URL + "?wsdl");
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.getResponseCode();
        String str = IOUtils.readStringFromStream(con.getInputStream());

        //Check to make sure SESSIONID is a string
        try (BufferedReader reader = new BufferedReader(new StringReader(str))) {
            String s = reader.readLine();
            while (s != null) {
                if (s.contains("SESSIONID") && s.contains("element ")) {
                    assertTrue(s.contains("string"));
                    assertFalse(s.contains("headerObj"));
                }
                s = reader.readLine();
            }
        }
        //wsdl is correct, now make sure we can actually invoke it
        QName name = new QName("http://cxf.apache.org/cxf5064",
            "SOAPHeaderServiceImplService");
        Service service = Service.create(name);
        service.addPort(name, SOAPBinding.SOAP11HTTP_BINDING, ServerMisc.CXF_5064_URL);
        SOAPHeaderSEI port = service.getPort(name, SOAPHeaderSEI.class);
        assertEquals("a-b", port.test(new HeaderObj("a-b")));


        service = Service.create(url, name);
        port = service.getPort(SOAPHeaderSEI.class);
        assertEquals("a-b", port.test(new HeaderObj("a-b")));
    }
}

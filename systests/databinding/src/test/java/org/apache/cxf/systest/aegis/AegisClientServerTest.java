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

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.authservice.AuthService;
import org.apache.cxf.authservice.Authenticate;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.aegis.SportsService.Pair;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AegisClientServerTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(AegisServer.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(AegisServer.class, true));
    }

    @Test
    public void testAegisClient() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(AuthService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/service");
        AuthService service = (AuthService) proxyFactory.create();
        assertTrue(service.authenticate("Joe", "Joe", "123"));
        assertFalse(service.authenticate("Joe1", "Joe", "fang"));
        assertTrue(service.authenticate("Joe", null, "123"));
        List<String> list = service.getRoles("Joe");
        assertEquals(3, list.size());
        assertEquals("Joe", list.get(0));
        assertEquals("Joe-1", list.get(1));
        assertEquals("Joe-2", list.get(2));
        String[] roles = service.getRolesAsArray("Joe");
        assertEquals(2, roles.length);
        assertEquals("Joe", roles[0]);
        assertEquals("Joe-1", roles[1]);

        assertEquals("get Joe", service.getAuthentication("Joe"));
        Authenticate au = new Authenticate();
        au.setSid("ffang");
        au.setUid("ffang");
        assertTrue(service.authenticate(au));
        au.setUid("ffang1");
        assertFalse(service.authenticate(au));
    }

    @Test
    public void testJaxWsAegisClient() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(AuthService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegis");
        AuthService service = (AuthService) proxyFactory.create();
        assertTrue(service.authenticate("Joe", "Joe", "123"));
        assertFalse(service.authenticate("Joe1", "Joe", "fang"));
        assertTrue(service.authenticate("Joe", null, "123"));
        List<String> list = service.getRoles("Joe");
        assertEquals(3, list.size());
        assertEquals("Joe", list.get(0));
        assertEquals("Joe-1", list.get(1));
        assertEquals("Joe-2", list.get(2));
        String[] roles = service.getRolesAsArray("Joe");
        assertEquals(2, roles.length);
        assertEquals("Joe", roles[0]);
        assertEquals("Joe-1", roles[1]);

        roles = service.getRolesAsArray("null");
        assertNull(roles);

        roles = service.getRolesAsArray("0");
        assertEquals(0, roles.length);

        assertEquals("get Joe", service.getAuthentication("Joe"));
        Authenticate au = new Authenticate();
        au.setSid("ffang");
        au.setUid("ffang");
        assertTrue(service.authenticate(au));
        au.setUid("ffang1");
        assertFalse(service.authenticate(au));
    }

    @Test
    public void testWSDL() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/jaxwsAndAegis?wsdl");
        Document dom = StaxUtils.read(url.openStream());
        TestUtilities util = new TestUtilities(this.getClass());
        util.addDefaultNamespaces();
        util.assertInvalid("//wsdl:definitions/wsdl:types/xsd:schema/"
                           + "xsd:complexType[@name='getRolesAsArrayResponse']/"
                           + "xsd:sequence/xsd:element[@maxOccurs]",
                           dom);
        util.assertValid("//wsdl:definitions/wsdl:types/xsd:schema/"
                           + "xsd:complexType[@name='getRolesAsArrayResponse']/"
                           + "xsd:sequence/xsd:element[@nillable='true']",
                           dom);

        url = new URL("http://localhost:" + PORT + "/serviceWithCustomNS?wsdl");
        dom = StaxUtils.read(url.openStream());
        util.assertValid("//wsdl:definitions[@targetNamespace='http://foo.bar.com']",
                         dom);
    }

    @Test
    public void testCollection() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setWsdlLocation("http://localhost:" + PORT + "/jaxwsAndAegisSports?wsdl");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();

        Collection<Team> teams = service.getTeams();
        assertEquals(1, teams.size());
        assertEquals("Patriots", teams.iterator().next().getName());

        //CXF-1251
        String s = service.testForMinOccurs0("A", null, "b");
        assertEquals("Anullb", s);
    }

    @Test
    public void testComplexMapResult() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegisSports");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();
        Map<String, Map<Integer, Integer>> result = service.testComplexMapResult();
        assertEquals(result.size(), 1);
        assertTrue(result.containsKey("key1"));
        assertNotNull(result.get("key1"));
        assertEquals(result.toString(), "{key1={1=3}}");
    }

    @Test
    public void testGenericCollection() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegisSports");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();
        List<String> list = new ArrayList<>();
        list.add("ffang");
        String ret = service.getGeneric(list);
        assertEquals(ret, "ffang");
    }

    @Test
    public void testGenericPair() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegisSports");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();
        Pair<String, Integer> ret = service.getReturnGenericPair("ffang", 111);
        assertEquals("ffang", ret.getFirst());
        assertEquals(Integer.valueOf(111), ret.getSecond());
    }

    @Test
    public void testReturnQualifiedPair() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegisSports");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();
        Pair<Integer, String> ret = service.getReturnQualifiedPair(111, "ffang");
        assertEquals(Integer.valueOf(111), ret.getFirst());
        assertEquals("ffang", ret.getSecond());
    }


    @Test
    public void testReturnGenericPair() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegisSports");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();
        int ret = service.getGenericPair(new Pair<Integer, String>(111, "String"));
        assertEquals(111, ret);
    }

    @Test
    public void testQualifiedPair() throws Exception {
        AegisDatabinding aegisBinding = new AegisDatabinding();
        JaxWsProxyFactoryBean proxyFactory = new JaxWsProxyFactoryBean();
        proxyFactory.setDataBinding(aegisBinding);
        proxyFactory.setServiceClass(SportsService.class);
        proxyFactory.setAddress("http://localhost:" + PORT + "/jaxwsAndAegisSports");
        proxyFactory.getInInterceptors().add(new LoggingInInterceptor());
        proxyFactory.getOutInterceptors().add(new LoggingOutInterceptor());
        SportsService service = (SportsService) proxyFactory.create();
        int ret = service.getQualifiedPair(new Pair<Integer, String>(111, "ffang"));
        assertEquals(111, ret);
    }


    @Test
    public void testDynamicClient() throws Exception {
        DynamicClientFactory dcf = DynamicClientFactory.newInstance();
        Client client = dcf.createClient("http://localhost:" + PORT + "/jaxwsAndAegisSports?wsdl&dynamic");

        Object r = client.invoke("getAttributeBean")[0];
        Method getAddrPlainString = r.getClass().getMethod("getAttrPlainString");
        String s = (String)getAddrPlainString.invoke(r);

        assertEquals("attrPlain", s);
    }
}

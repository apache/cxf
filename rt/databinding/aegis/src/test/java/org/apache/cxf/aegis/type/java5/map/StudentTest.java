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
package org.apache.cxf.aegis.type.java5.map;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StudentTest extends AbstractAegisTest {

    @Test
    public void testWSDL() throws Exception {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(StudentService.class);
        sf.setServiceBean(new StudentServiceImpl());
        sf.setAddress("local://StudentService");
        setupAegis(sf);
        Server server = sf.create();
        Document wsdl = getWSDLDocument(server);

        assertValid("//*[@name='string2stringMap']", wsdl);
    }

    @Test
    public void testReturnMap() throws Exception {

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(StudentService.class);
        sf.setServiceBean(new StudentServiceImpl());
        sf.setAddress("local://StudentService");
        setupAegis(sf);
        Server server = sf.create();
        server.start();

        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setAddress("local://StudentService");
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());

        StudentService clientInterface = proxyFac.create(StudentService.class);
        Map<Long, Student> fullMap = clientInterface.getStudentsMap();
        assertNotNull(fullMap);
        Student one = fullMap.get(Long.valueOf(1));
        assertNotNull(one);
        assertEquals("Student1", one.getName());

        Map<String, ?> wildMap = clientInterface.getWildcardMap();
        assertEquals("valuestring", wildMap.get("keystring"));
    }

    @Test
    public void testMapMap() throws Exception {

        ServerFactoryBean sf = new ServerFactoryBean();
        sf.setServiceClass(StudentServiceDocLiteral.class);
        sf.setServiceBean(new StudentServiceDocLiteralImpl());
        sf.setAddress("local://StudentServiceDocLiteral");
        setupAegis(sf);
        Server server = sf.create();
        server.start();

        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setAddress("local://StudentServiceDocLiteral");
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());
        //CHECKSTYLE:OFF
        HashMap<String, Student> mss = new HashMap<>();
        mss.put("Alice", new Student());
        HashMap<String, HashMap<String, Student>> mmss = new HashMap<>();
        mmss.put("Bob", mss);

        StudentServiceDocLiteral clientInterface = proxyFac.create(StudentServiceDocLiteral.class);
        clientInterface.takeMapMap(mmss);
        //CHECKSTYLE:ON
    }

    @Test
    public void testReturnMapDocLiteral() throws Exception {

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(StudentServiceDocLiteral.class);
        sf.setServiceBean(new StudentServiceDocLiteralImpl());
        sf.setAddress("local://StudentServiceDocLiteral");
        setupAegis(sf);
        Server server = sf.create();
        server.start();

        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setAddress("local://StudentServiceDocLiteral");
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());

        StudentServiceDocLiteral clientInterface = proxyFac.create(StudentServiceDocLiteral.class);
        Map<Long, Student> fullMap = clientInterface.getStudentsMap();
        assertNotNull(fullMap);
        Student one = fullMap.get(Long.valueOf(1));
        assertNotNull(one);
        assertEquals("Student1", one.getName());

    }
}
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

package org.apache.cxf.systest.ws.transfer;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class GetTest {

    static final String PORT = TestUtil.getPortNumber(CreateStudentTest.class);
    static final String PORT2 = TestUtil.getPortNumber(CreateStudentTest.class, 2);

    private static EndpointReferenceType studentRef;

    private static EndpointReferenceType teacherRef;

    @BeforeClass
    public static void beforeClass() throws XMLStreamException {
        TestUtils.createStudentsServers(PORT, PORT2);
        TestUtils.createTeachersServers(PORT2);

        ResourceFactory rf = TestUtils.createResourceFactoryClient(PORT);

        Document createStudentXML = StaxUtils.read(
                GetTest.class.getResourceAsStream("/xml/createStudent.xml"));
        Create studentRequest = new Create();
        studentRequest.setRepresentation(new Representation());
        studentRequest.getRepresentation().setAny(createStudentXML.getDocumentElement());
        studentRef = rf.create(studentRequest).getResourceCreated();

        Document createTeacherXML = StaxUtils.read(
                GetTest.class.getResourceAsStream("/xml/createTeacher.xml"));
        Create teacherRequest = new Create();
        teacherRequest.setRepresentation(new Representation());
        teacherRequest.getRepresentation().setAny(createTeacherXML.getDocumentElement());
        teacherRef = rf.create(teacherRequest).getResourceCreated();
    }

    @AfterClass
    public static void afterClass() {
        TestUtils.destroyStudentsServers();
        TestUtils.destroyTeachersServers();
    }

    @Test
    public void getStudentTest() {
        Resource client = TestUtils.createResourceClient(studentRef);
        GetResponse response = client.get(new Get());

        Element representation = (Element) response.getRepresentation().getAny();
        NodeList children = representation.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if ("name".equals(child.getLocalName())) {
                Assert.assertEquals("John", child.getTextContent());
            } else if ("surname".equals(child.getLocalName())) {
                Assert.assertEquals("Smith", child.getTextContent());
            } else if ("address".equals(child.getLocalName())) {
                Assert.assertEquals("Street 21", child.getTextContent());
            }
        }
    }

    @Test
    public void getTeacherTest() {
        Resource client = TestUtils.createResourceClient(teacherRef);
        GetResponse response = client.get(new Get());

        Element representation = (Element) response.getRepresentation().getAny();
        NodeList children = representation.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            if ("name".equals(child.getLocalName())) {
                Assert.assertEquals("Bob", child.getTextContent());
            } else if ("surname".equals(child.getLocalName())) {
                Assert.assertEquals("Stuart", child.getTextContent());
            } else if ("address".equals(child.getLocalName())) {
                Assert.assertEquals("Street 526", child.getTextContent());
            }
        }
    }

}

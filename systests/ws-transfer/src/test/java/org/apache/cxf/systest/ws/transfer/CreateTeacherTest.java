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

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CreateTeacherTest {

    static final String PORT = TestUtil.getPortNumber(CreateStudentTest.class);
    static final String PORT2 = TestUtil.getPortNumber(CreateStudentTest.class, 2);

    static final String RESOURCE_TEACHERS_URL = "http://localhost:" + PORT2 + "/ResourceTeachers";

    @BeforeClass
    public static void beforeClass() {
        TestUtils.createStudentsServers(PORT, PORT2);
        TestUtils.createTeachersServers(PORT2);
    }

    @AfterClass
    public static void afterClass() {
        TestUtils.destroyStudentsServers();
        TestUtils.destroyTeachersServers();
    }

    @Test
    public void createTeacherTest() throws XMLStreamException {
        Document createTeacherXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createTeacher.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherXML.getDocumentElement());

        ResourceFactory rf = TestUtils.createResourceFactoryClient(PORT);
        CreateResponse response = rf.create(request);

        Assert.assertEquals(RESOURCE_TEACHERS_URL,
            response.getResourceCreated().getAddress().getValue());
    }

    @Test
    public void createTeacherPartialTest() throws XMLStreamException {
        Document createTeacherPartialXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createTeacherPartial.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherPartialXML.getDocumentElement());

        ResourceFactory rf = TestUtils.createResourceFactoryClient(PORT);
        CreateResponse response = rf.create(request);

        Assert.assertEquals(RESOURCE_TEACHERS_URL,
            response.getResourceCreated().getAddress().getValue());
    }

    @Test(expected = SOAPFaultException.class)
    public void createTeacherWrongTest() throws XMLStreamException {
        Document createTeacherWrongXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createTeacherWrong.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherWrongXML.getDocumentElement());

        ResourceFactory rf = TestUtils.createResourceFactoryClient(PORT);
        rf.create(request);
    }

}

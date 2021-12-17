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
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PutTest {

    static final String PORT = TestUtil.getPortNumber(CreateStudentTest.class);
    static final String PORT2 = TestUtil.getPortNumber(CreateStudentTest.class, 2);

    @Before
    public void before() {
        TestUtils.createStudentsServers(PORT, PORT2);
        TestUtils.createTeachersServers(PORT2);
    }

    @After
    public void after() {
        TestUtils.destroyStudentsServers();
        TestUtils.destroyTeachersServers();
    }

    @Test
    public void rightStudentPutTest() throws XMLStreamException {
        CreateResponse createResponse = createStudent();

        Document putStudentXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/putStudent.xml"));
        Put request = new Put();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(putStudentXML.getDocumentElement());

        Resource client = TestUtils.createResourceClient(createResponse.getResourceCreated());
        client.put(request);
    }

    @Test(expected = SOAPFaultException.class)
    public void wrongStudentPutTest() throws XMLStreamException {
        createStudent();
        CreateResponse createResponse = createStudent();

        Document putStudentXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/putStudent.xml"));
        Put request = new Put();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(putStudentXML.getDocumentElement());

        Resource client = TestUtils.createResourceClient(createResponse.getResourceCreated());
        client.put(request);
    }

    @Test
    public void rightTeacherPutTest() throws XMLStreamException {
        CreateResponse createResponse = createTeacher();

        Document putStudentXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/putTeacher.xml"));
        Put request = new Put();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(putStudentXML.getDocumentElement());

        Resource client = TestUtils.createResourceClient(createResponse.getResourceCreated());
        client.put(request);
    }

    @Test(expected = SOAPFaultException.class)
    public void wrongTeacherPutTest() throws XMLStreamException {
        createStudent();
        CreateResponse createResponse = createTeacher();

        Document putStudentXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/putTeacher.xml"));
        Put request = new Put();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(putStudentXML.getDocumentElement());

        Resource client = TestUtils.createResourceClient(createResponse.getResourceCreated());
        client.put(request);
    }

    private CreateResponse createStudent() throws XMLStreamException {
        Document createStudentXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createStudent.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createStudentXML.getDocumentElement());

        ResourceFactory rf = TestUtils.createResourceFactoryClient(PORT);
        return rf.create(request);
    }

    private CreateResponse createTeacher() throws XMLStreamException {
        Document createTeacherXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createTeacher.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherXML.getDocumentElement());

        ResourceFactory rf = TestUtils.createResourceFactoryClient(PORT);
        return rf.create(request);
    }
}

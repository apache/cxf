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
import javax.xml.ws.soap.SOAPFaultException;
import org.w3c.dom.Document;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CreateStudentTest {
    
    
    @BeforeClass
    public static void beforeClass() {
        TestUtils.createStudentsServers();
        TestUtils.createTeachersServers();
    }
    
    @AfterClass
    public static void afterClass() {
        TestUtils.destroyStudentsServers();
        TestUtils.destroyTeachersServers();
    }
    
    @Test
    public void createStudentTest() throws XMLStreamException {
        Document createStudentXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createStudent.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createStudentXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        CreateResponse response = rf.create(request);
        
        Assert.assertEquals(TestUtils.RESOURCE_STUDENTS_URL,
            response.getResourceCreated().getAddress().getValue());
    }

    @Test
    public void createStudentPartialTest() throws XMLStreamException {
        Document createStudentPartialXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createStudentPartial.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createStudentPartialXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        CreateResponse response = rf.create(request);
        
        Assert.assertEquals(TestUtils.RESOURCE_STUDENTS_URL,
            response.getResourceCreated().getAddress().getValue());
    }
    
    @Test(expected = SOAPFaultException.class)
    public void createStudentWrongTest() throws XMLStreamException {
        Document createStudentWrongXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/createStudentWrong.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createStudentWrongXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        rf.create(request);
    }
    
    @Test(expected = SOAPFaultException.class)
    public void createRandomTest() throws XMLStreamException {
        Document randomXML = StaxUtils.read(
                getClass().getResourceAsStream("/xml/random.xml"));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(randomXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        rf.create(request);
    }
}

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

import javax.xml.ws.soap.SOAPFaultException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author erich
 */
public class DeleteTest {
    
    @Before
    public void before() {
        TestUtils.createStudentsServers();
        TestUtils.createTeachersServers();
    }
    
    @After
    public void after() {
        TestUtils.destroyStudentsServers();
        TestUtils.destroyTeachersServers();
    }
    
    @Test
    public void deleteStudent() {
        CreateResponse response = createStudent();
        Resource client = TestUtils.createResourceClient(response.getResourceCreated());
        client.delete(new Delete());
    }
    
    @Test(expected = SOAPFaultException.class)
    public void getDeletedStudent() {
        CreateResponse response = createStudent();
        Resource client = TestUtils.createResourceClient(response.getResourceCreated());
        client.delete(new Delete());
        client.get(new Get());
    }
    
    @Test(expected = SOAPFaultException.class)
    public void deleteDeletedStudent() {
        CreateResponse response = createStudent();
        Resource client = TestUtils.createResourceClient(response.getResourceCreated());
        client.delete(new Delete());
        client.delete(new Delete());
    }
    
    @Test
    public void deleteTeacher() {
        CreateResponse response = createTeacher();
        Resource client = TestUtils.createResourceClient(response.getResourceCreated());
        client.delete(new Delete());
    }
    
    @Test(expected = SOAPFaultException.class)
    public void getDeletedTeacher() {
        CreateResponse response = createTeacher();
        Resource client = TestUtils.createResourceClient(response.getResourceCreated());
        client.delete(new Delete());
        client.get(new Get());
    }
    
    @Test(expected = SOAPFaultException.class)
    public void deleteDeletedTeacher() {
        CreateResponse response = createTeacher();
        Resource client = TestUtils.createResourceClient(response.getResourceCreated());
        client.delete(new Delete());
        client.delete(new Delete());
    }
    
    private CreateResponse createStudent() {
        Document createStudentXML = TransferTools.parse(
            new InputSource(getClass().getResourceAsStream("/xml/createStudent.xml")));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createStudentXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        return rf.create(request);
    }
    
    private CreateResponse createTeacher() {
        Document createTeacherXML = TransferTools.parse(
            new InputSource(getClass().getResourceAsStream("/xml/createTeacher.xml")));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        return rf.create(request);
    }
}

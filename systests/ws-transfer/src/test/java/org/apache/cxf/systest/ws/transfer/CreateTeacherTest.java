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
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author erich
 */
public class CreateTeacherTest {
    
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
    public void createTeacherTest() {
        Document createTeacherXML = TransferTools.parse(
            new InputSource(getClass().getResourceAsStream("/xml/createTeacher.xml")));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        CreateResponse response = rf.create(request);
        
        Assert.assertEquals(TestUtils.RESOURCE_TEACHERS_URL,
            response.getResourceCreated().getAddress().getValue());
    }

    @Test
    public void createTeacherPartialTest() {
        Document createTeacherPartialXML = TransferTools.parse(
            new InputSource(getClass().getResourceAsStream("/xml/createTeacherPartial.xml")));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherPartialXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        CreateResponse response = rf.create(request);
        
        Assert.assertEquals(TestUtils.RESOURCE_TEACHERS_URL,
            response.getResourceCreated().getAddress().getValue());
    }
    
    @Test(expected = SOAPFaultException.class)
    public void createTeacherWrongTest() {
        Document createTeacherWrongXML = TransferTools.parse(
            new InputSource(getClass().getResourceAsStream("/xml/createTeacherWrong.xml")));
        Create request = new Create();
        request.setRepresentation(new Representation());
        request.getRepresentation().setAny(createTeacherWrongXML.getDocumentElement());
        
        ResourceFactory rf = TestUtils.createResourceFactoryClient();
        rf.create(request);
    }
    
}

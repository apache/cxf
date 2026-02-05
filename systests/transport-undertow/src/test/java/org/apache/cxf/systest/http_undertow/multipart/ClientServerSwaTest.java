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
package org.apache.cxf.systest.http_undertow.multipart;

import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.swa.SwAService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.util.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
public class ClientServerSwaTest extends AbstractBusClientServerTestBase {
    static String serverPort = TestUtil.getPortNumber(Server.class);
    static String serverPortInvalid = TestUtil.getPortNumber(Server.class, 1);
    
    static class TestParam {
        private final String port;
        private final String exMessage;
        TestParam(String port, String ex) {
            this.port = port;
            this.exMessage = ex;          
        }
        String getPort() {
            return port;
        }
        String getExceptionMessage() {
            return exMessage;
        }     
    }
    
    final TestParam test;
    public ClientServerSwaTest(TestParam test) {
        this.test = test;
    }

    @Parameterized.Parameters
    public static Collection<TestParam> data() {
        List<TestParam> parameters = new ArrayList<>();
        parameters.add(new TestParam(serverPort, null));
        parameters.add(new TestParam(serverPortInvalid, "java.net.ConnectException"));
        parameters.add(new TestParam(serverPort + "/INVALID", "404: Not Found"));
        return parameters;
    }
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    private String getFullStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
    
    
    @Test
    public void testSwaTypesWithDispatchAPI() throws Exception {
        URL url1 = this.getClass().getResource("resources/attach.text");
        URL url2 = this.getClass().getResource("resources/attach.html");
        URL url3 = this.getClass().getResource("resources/attach.xml");
        URL url4 = this.getClass().getResource("resources/attach.jpeg1");
        URL url5 = this.getClass().getResource("resources/attach.jpeg2");


        int copyCount = 10000;
        byte[] bytes = IOUtils.readBytesFromStream(url1.openStream());
        byte[] bigBytes = new byte[bytes.length * copyCount];
        for (int x = 0; x < copyCount; x++) {
            System.arraycopy(bytes, 0, bigBytes, x * bytes.length, bytes.length);
        }

        DataHandler dh1 = new DataHandler(new ByteArrayDataSource(bigBytes, "text/plain"));
        DataHandler dh2 = new DataHandler(url2);
        DataHandler dh3 = new DataHandler(url3);
        DataHandler dh4 = new DataHandler(url4);
        DataHandler dh5 = new DataHandler(url5);

        SwAService service = new SwAService();

        Dispatch<SOAPMessage> disp = service
            .createDispatch(SwAService.SwAServiceHttpPort,
                            SOAPMessage.class,
                            Service.Mode.MESSAGE);
        setAddress(disp, "http://localhost:" + test.getPort() + "/swa");


        SOAPMessage msg = MessageFactory.newInstance().createMessage();
        SOAPBody body = msg.getSOAPPart().getEnvelope().getBody();
        body.addBodyElement(new QName("http://cxf.apache.org/swa/types",
                                      "VoidRequest"));

        AttachmentPart att = msg.createAttachmentPart(dh1);
        att.setContentId("<attach1=c187f5da-fa5d-4ab9-81db-33c2bb855201@apache.org>");
        msg.addAttachmentPart(att);

        att = msg.createAttachmentPart(dh2);
        att.setContentId("<attach2=c187f5da-fa5d-4ab9-81db-33c2bb855202@apache.org>");
        msg.addAttachmentPart(att);

        att = msg.createAttachmentPart(dh3);
        att.setContentId("<attach3=c187f5da-fa5d-4ab9-81db-33c2bb855203@apache.org>");
        msg.addAttachmentPart(att);

        att = msg.createAttachmentPart(dh4);
        att.setContentId("<attach4=c187f5da-fa5d-4ab9-81db-33c2bb855204@apache.org>");
        msg.addAttachmentPart(att);

        att = msg.createAttachmentPart(dh5);
        att.setContentId("<attach5=c187f5da-fa5d-4ab9-81db-33c2bb855205@apache.org>");
        msg.addAttachmentPart(att);

        //Test for CXF-
        try {
            msg = disp.invoke(msg);
            assertEquals(5, msg.countAttachments());
        } catch  (Exception ex) {
            if (test.getExceptionMessage() != null) {
                Assert.hasText(getFullStackTrace(ex), test.getExceptionMessage());
                return;
            }
            throw ex;
        }            

    }
}

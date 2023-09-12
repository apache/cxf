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
package org.apache.cxf.systest.swa;

import java.awt.Image;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.swa.SwAService;
import org.apache.cxf.swa.SwAServiceInterface;
import org.apache.cxf.swa.types.DataStruct;
import org.apache.cxf.swa.types.OutputResponseAll;
import org.apache.cxf.swa.types.VoidRequest;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.util.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void testSwaNoMimeCodeGen() throws Exception {
        org.apache.cxf.swa_nomime.SwAService service = new org.apache.cxf.swa_nomime.SwAService();

        org.apache.cxf.swa_nomime.SwAServiceInterface port = service.getSwAServiceHttpPort();
        setAddress(port, "http://localhost:" + test.getPort() + "/swa-nomime");

        Holder<String> textHolder = new Holder<>("Hi");
        Holder<byte[]> data = new Holder<>("foobar".getBytes());

        try {
            port.echoData(textHolder, data);
            String string = IOUtils.newStringFromBytes(data.value);
            assertEquals("testfoobar", string);
            assertEquals("Hi", textHolder.value);
    
            URL url1 = this.getClass().getResource("resources/attach.text");
            URL url2 = this.getClass().getResource("resources/attach.html");
            URL url3 = this.getClass().getResource("resources/attach.xml");
            URL url4 = this.getClass().getResource("resources/attach.jpeg1");
            URL url5 = this.getClass().getResource("resources/attach.jpeg2");
    
            Holder<String> attach1 = new Holder<>(IOUtils.toString(url1.openStream()));
            Holder<String> attach2 = new Holder<>(IOUtils.toString(url2.openStream()));
            Holder<String> attach3 = new Holder<>(IOUtils.toString(url3.openStream()));
            Holder<byte[]> attach4 = new Holder<>(IOUtils.readBytesFromStream(url4.openStream()));
            Holder<byte[]> attach5 = new Holder<>(IOUtils.readBytesFromStream(url5.openStream()));
            org.apache.cxf.swa_nomime.types.VoidRequest request
                = new org.apache.cxf.swa_nomime.types.VoidRequest();
        
            org.apache.cxf.swa_nomime.types.OutputResponseAll response
                = port.echoAllAttachmentTypes(request,
                                              attach1,
                                              attach2,
                                              attach3,
                                              attach4,
                                              attach5);
            assertNotNull(response);
        } catch  (Exception ex) {
            if (test.getExceptionMessage() != null) {
                Assert.hasText(getFullStackTrace(ex), test.getExceptionMessage());
                return;
            }
            throw ex;
        }
    }

    @Test
    public void testSwa() throws Exception {
        SwAService service = new SwAService();

        SwAServiceInterface port = service.getSwAServiceHttpPort();
        setAddress(port, "http://localhost:" + test.getPort() + "/swa");

        Holder<String> textHolder = new Holder<>();
        Holder<DataHandler> data = new Holder<>();

        ByteArrayDataSource source = new ByteArrayDataSource("foobar".getBytes(), "application/octet-stream");
        DataHandler handler = new DataHandler(source);

        data.value = handler;

        textHolder.value = "Hi";

        try {
            port.echoData(textHolder, data);
            InputStream bis = null;
            bis = data.value.getDataSource().getInputStream();
            byte[] b = new byte[10];
            bis.read(b, 0, 10);
            String string = IOUtils.newStringFromBytes(b);
            assertEquals("testfoobar", string);
            assertEquals("Hi", textHolder.value);
        } catch  (Exception ex) {
            if (test.getExceptionMessage() != null) {
                Assert.hasText(getFullStackTrace(ex), test.getExceptionMessage());
                return;
            }
            throw ex;
        }
    }

    @Test
    public void testSwaWithHeaders() throws Exception {
        SwAService service = new SwAService();

        SwAServiceInterface port = service.getSwAServiceHttpPort();
        setAddress(port, "http://localhost:" + test.getPort() + "/swa");

        Holder<String> textHolder = new Holder<>();
        Holder<String> headerHolder = new Holder<>();
        Holder<DataHandler> data = new Holder<>();

        ByteArrayDataSource source = new ByteArrayDataSource("foobar".getBytes(), "application/octet-stream");
        DataHandler handler = new DataHandler(source);

        data.value = handler;

        textHolder.value = "Hi";
        headerHolder.value = "Header";

        try {
            port.echoDataWithHeader(textHolder, data, headerHolder);
            InputStream bis = null;
            bis = data.value.getDataSource().getInputStream();
            byte[] b = new byte[10];
            bis.read(b, 0, 10);
            String string = IOUtils.newStringFromBytes(b);
            assertEquals("testfoobar", string);
            assertEquals("Hi", textHolder.value);
            assertEquals("Header", headerHolder.value);
        } catch  (Exception ex) {
            if (test.getExceptionMessage() != null) {
                Assert.hasText(getFullStackTrace(ex), test.getExceptionMessage());
                return;
            }
            throw ex;
        }
    }

    @Test
    public void testSwaDataStruct() throws Exception {
        SwAService service = new SwAService();

        SwAServiceInterface port = service.getSwAServiceHttpPort();
        setAddress(port, "http://localhost:" + test.getPort() + "/swa");

        Holder<DataStruct> structHolder = new Holder<>();

        ByteArrayDataSource source = new ByteArrayDataSource("foobar".getBytes(), "application/octet-stream");
        DataHandler handler = new DataHandler(source);

        DataStruct struct = new DataStruct();
        struct.setDataRef(handler);
        structHolder.value = struct;

        try {
            port.echoDataRef(structHolder);
    
            handler = structHolder.value.getDataRef();
            InputStream bis = handler.getDataSource().getInputStream();
            byte[] b = new byte[10];
            bis.read(b, 0, 10);
            String string = IOUtils.newStringFromBytes(b);
            assertEquals("testfoobar", string);
            bis.close();
        } catch  (Exception ex) {
            if (test.getExceptionMessage() != null) {
                Assert.hasText(getFullStackTrace(ex), test.getExceptionMessage());
                return;
            }
            throw ex;
        }
    }

    @Test
    public void testSwaTypes() throws Exception {
        SwAService service = new SwAService();

        SwAServiceInterface port = service.getSwAServiceHttpPort();
        setAddress(port, "http://localhost:" + test.getPort() + "/swa");

        URL url1 = this.getClass().getResource("resources/attach.text");
        URL url2 = this.getClass().getResource("resources/attach.html");
        URL url3 = this.getClass().getResource("resources/attach.xml");
        URL url4 = this.getClass().getResource("resources/attach.jpeg1");
        URL url5 = this.getClass().getResource("resources/attach.gif");

        DataHandler dh1 = new DataHandler(url1);
        DataHandler dh2 = new DataHandler(url2);
        DataHandler dh3 = new DataHandler(url3);
        //DataHandler dh4 = new DataHandler(url4);
        //DataHandler dh5 = new DataHandler(url5);
        Holder<DataHandler> attach1 = new Holder<>();
        attach1.value = dh1;
        Holder<DataHandler> attach2 = new Holder<>();
        attach2.value = dh2;
        Holder<Source> attach3 = new Holder<>();
        attach3.value = new StreamSource(dh3.getInputStream());
        Holder<Image> attach4 = new Holder<>();
        Holder<Image> attach5 = new Holder<>();
        attach4.value = ImageIO.read(url4);
        attach5.value = ImageIO.read(url5);
        
        try {
            VoidRequest request = new VoidRequest();
            OutputResponseAll response = port.echoAllAttachmentTypes(request, attach1, attach2, attach3, attach4,
                                                                     attach5);
    
            assertNotNull(response);
            Map<?, ?> map = CastUtils.cast((Map<?, ?>)((BindingProvider)port).getResponseContext()
                                               .get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS));
            assertNotNull(map);
            assertEquals(5, map.size());
        } catch  (Exception ex) {
            if (test.getExceptionMessage() != null) {
                Assert.hasText(getFullStackTrace(ex), test.getExceptionMessage());
                return;
            }
            throw ex;
        }
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

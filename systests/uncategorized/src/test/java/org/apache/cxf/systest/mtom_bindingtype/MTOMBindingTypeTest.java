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

package org.apache.cxf.systest.mtom_bindingtype;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.systest.mtom_feature.Hello;
import org.apache.cxf.systest.mtom_feature.HelloService;
import org.apache.cxf.systest.mtom_feature.ImageHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MTOMBindingTypeTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

    private final QName serviceName = new QName("http://apache.org/cxf/systest/mtom_feature",
                                                "HelloService");
    //private Hello port = getPort();

    @Before
    public void setUp() throws Exception {
        createBus();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    protected ByteArrayOutputStream setupInLogging() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bos, true);
        LoggingInInterceptor in = new LoggingInInterceptor(writer);
        this.bus.getInInterceptors().add(in);
        return bos;
    }

    protected ByteArrayOutputStream setupOutLogging() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bos, true);

        LoggingOutInterceptor out = new LoggingOutInterceptor(writer);
        this.bus.getOutInterceptors().add(out);

        return bos;
    }

    @Test
    public void testDetail() throws Exception {
        if (Boolean.getBoolean("java.awt.headless")) {
            System.out.println("Running headless. Skipping test as Images may not work.");
            return;
        }        
        ByteArrayOutputStream input = setupInLogging();
        ByteArrayOutputStream output = setupOutLogging();

        Holder<byte[]> photo = new Holder<byte[]>("CXF".getBytes());
        Holder<Image> image = new Holder<Image>(getImage("/java.jpg"));

        Hello port = getPort();

        SOAPBinding binding = (SOAPBinding) ((BindingProvider)port).getBinding();
        binding.setMTOMEnabled(true);


        port.detail(photo, image);

        String expected = "<xop:Include xmlns:xop=";
        assertTrue(output.toString().indexOf(expected) != -1);
        assertTrue(input.toString().indexOf(expected) != -1);

        assertEquals("CXF", new String(photo.value));
        assertNotNull(image.value);
    }
    
    @Test
    @org.junit.Ignore
    public void testEcho() throws Exception {
        if (Boolean.getBoolean("java.awt.headless")) {
            System.out.println("Running headless. Skipping test as Images may not work.");
            return;
        }        
        byte[] bytes = ImageHelper.getImageBytes(getImage("/java.jpg"), "image/jpeg");
        Holder<byte[]> image = new Holder<byte[]>(bytes);

        Hello port = getPort();
        SOAPBinding binding = (SOAPBinding) ((BindingProvider)port).getBinding();
        binding.setMTOMEnabled(true);

        port.echoData(image);
        assertNotNull(image);
    }

    private Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }

    private Hello getPort() {
        URL wsdl = getClass().getResource("/wsdl_systest/mtom.wsdl");
        assertNotNull("WSDL is null", wsdl);

        HelloService service = new HelloService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        Hello hello = service.getHelloPort();
        try {
            updateAddressPort(hello, PORT);
        } catch (Exception ex) {
            //ignore
        }
        
        return hello;
        
    }
}

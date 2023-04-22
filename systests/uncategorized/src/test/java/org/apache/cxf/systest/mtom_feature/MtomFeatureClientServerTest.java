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

package org.apache.cxf.systest.mtom_feature;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.MTOMFeature;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.ext.logging.event.PrintWriterEventSender;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.local.LocalConduit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MtomFeatureClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;
    private final QName serviceName = new QName("http://apache.org/cxf/systest/mtom_feature",
                                                "HelloService");
    private Hello port = getPort();

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Before
    public void setUp() throws Exception {
        this.createBus();
    }

    @Test
    public void testDetail() throws Exception {
        Holder<byte[]> photo = new Holder<>("CXF".getBytes());
        Holder<Image> image = new Holder<>(getImage("/java.jpg"));
        port.detail(photo, image);
        assertEquals("CXF", new String(photo.value));
        assertNotNull(image.value);
    }

    @Test
    public void testEcho() throws Exception {
        byte[] bytes = ImageHelper.getImageBytes(getImage("/java.jpg"), "image/jpeg");
        Holder<byte[]> image = new Holder<>(bytes);
        port.echoData(image);
        assertNotNull(image);
    }

    @Test
    public void testWithLocalTransport() throws Exception {
        Object implementor = new HelloImpl();
        String address = "local://Hello";
        Endpoint.publish(address, implementor);
        QName portName = new QName("http://apache.org/cxf/systest/mtom_feature",
                                   "HelloPort");

        Service service = Service.create(serviceName);
        service.addPort(portName,
                        "http://schemas.xmlsoap.org/soap/",
                        "local://Hello");
        port = service.getPort(portName,
                               Hello.class,
                               new MTOMFeature());



        ((BindingProvider)port).getRequestContext()
            .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);

        ((BindingProvider)port).getRequestContext()
            .put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        Holder<byte[]> photo = new Holder<>("CXF".getBytes());
        Holder<Image> image = new Holder<>(getImage("/java.jpg"));
        port.detail(photo, image);
        assertEquals("CXF", new String(photo.value));
        assertNotNull(image.value);


        ((BindingProvider)port).getRequestContext()
            .put(LocalConduit.DIRECT_DISPATCH, Boolean.FALSE);
        photo = new Holder<>("CXF".getBytes());
        image = new Holder<>(getImage("/java.jpg"));
        port.detail(photo, image);
        assertEquals("CXF", new String(photo.value));
        assertNotNull(image.value);
    }

    @Test
    public void testEchoWithLowThreshold() throws Exception {
        ByteArrayOutputStream bout = this.setupOutLogging();
        byte[] bytes = ImageHelper.getImageBytes(getImage("/java.jpg"), "image/jpeg");
        Holder<byte[]> image = new Holder<>(bytes);
        Hello hello = this.getPort(500);
        hello.echoData(image);
        assertTrue("MTOM should be enabled", bout.toString().indexOf("<xop:Include") > -1);
    }

    @Test
    public void testEchoWithHighThreshold() throws Exception {
        ByteArrayOutputStream bout = this.setupOutLogging();
        byte[] bytes = ImageHelper.getImageBytes(getImage("/java.jpg"), "image/jpeg");
        Holder<byte[]> image = new Holder<>(bytes);
        Hello hello = this.getPort(2000);
        hello.echoData(image);
        assertTrue("MTOM should not be enabled", bout.toString().indexOf("<xop:Include") == -1);
    }

    private ByteArrayOutputStream setupOutLogging() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(bos, true);

        LoggingOutInterceptor out = new LoggingOutInterceptor(new PrintWriterEventSender(writer));
        this.bus.getOutInterceptors().add(out);

        return bos;
    }

    private Image getImage(String name) throws Exception {
        return ImageIO.read(getClass().getResource(name));
    }

    private Hello getPort() {
        return getPort(0);
    }
    private Hello getPort(int threshold) {
        URL wsdl = getClass().getResource("/wsdl_systest/mtom.wsdl");
        assertNotNull("WSDL is null", wsdl);

        HelloService service = new HelloService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        //return service.getHelloPort();
        MTOMFeature mtomFeature = new MTOMFeature();
        if (threshold > 0) {
            mtomFeature = new MTOMFeature(true, threshold);
        }
        Hello hello = service.getHelloPort(mtomFeature);

        try {
            updateAddressPort(hello, PORT);
        } catch (Exception e) {
            //ignore
        }
        return hello;
    }
}

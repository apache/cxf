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
package org.apache.cxf.aegis.client;

import java.lang.reflect.Method;

import javax.xml.ws.Holder;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HeaderTest extends AbstractAegisTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ReflectionServiceFactoryBean factory = new ReflectionServiceFactoryBean() {
            public boolean isHeader(Method method, int j) {
                return true;
            }

            protected boolean isInParam(Method method, int j) {
                return j == 0;
            }

            protected boolean isOutParam(Method method, int j) {
                return j == -1 || j == 1;
            }

        };

        factory.setInvoker(new BeanInvoker(new EchoImpl()));

        ServerFactoryBean svrFac = new ServerFactoryBean();
        svrFac.setAddress("Echo");
        setupAegis(svrFac);
        svrFac.setServiceFactory(factory);
        svrFac.setServiceClass(Echo.class);
        svrFac.setBus(getBus());
        svrFac.create();
    }

    @Test
    @Ignore
    public void testHeaders() throws Exception {
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setAddress("Echo");
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setBus(getBus());
        setupAegis(proxyFac.getClientFactoryBean());

        Echo echo = (Echo)proxyFac.create();

        Holder<String> h = new Holder<String>();
        assertEquals("hi", echo.echo("hi", h));
        assertEquals("header2", h.value);

        Document wsdl = getWSDLDocument("Echo");

        addNamespace("wsdlsoap", SOAPConstants.WSDL11_NS);
        assertValid("//wsdl:input/wsdlsoap:header[@message='tns:echoRequestHeaders'][@part='in0']", wsdl);
        assertValid("//wsdl:output/wsdlsoap:header[@message='tns:echoResponseHeaders'][@part='out']", wsdl);
        assertValid("//wsdl:output/wsdlsoap:header[@message='tns:echoResponseHeaders'][@part='out0']", wsdl);
    }
}

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
package org.apache.cxf.jaxws.implicitheader;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A client created with a WSDL location but no service/endpoint name must use the binding defined in
 * the WSDL, rather than silently generating a default one which drops the soap:header bindings.
 */
public class ImplicitHeaderTest extends AbstractJaxWsTest {

    private static final String NS = "urn:cxf:implicitheader";
    private static final String ADDRESS = "local://implicitheader";

    @Test
    public void testHeaderSentWithoutServiceName() throws Exception {
        startServer();

        JaxWsProxyFactoryBean factory = createClientFactory();
        EchoPortType port = factory.create(EchoPortType.class);

        Client client = ClientProxy.getClient(port);
        assertEquals(new QName(NS, "ImplicitHeaderService"),
                     client.getEndpoint().getService().getName());
        assertEquals(new QName(NS, "ImplicitHeaderPort"),
                     client.getEndpoint().getEndpointInfo().getName());
        assertEquals("hello:secret", port.echo("hello", "secret"));
    }

    @Test
    public void testHeaderSentWithServiceName() throws Exception {
        startServer();

        JaxWsProxyFactoryBean factory = createClientFactory();
        factory.setServiceName(new QName(NS, "ImplicitHeaderService"));
        factory.setEndpointName(new QName(NS, "ImplicitHeaderPort"));
        EchoPortType port = factory.create(EchoPortType.class);

        assertEquals("hello:secret", port.echo("hello", "secret"));
    }

    @Test
    public void testWarningWhenWsdlBindingIgnored() throws Exception {
        Logger logger = LogUtils.getL7dLogger(WSDLServiceFactory.class);
        final StringBuilder warnings = new StringBuilder();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.WARNING) {
                    warnings.append(record.getMessage()).append(' ');
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            // an endpoint name that isn't in the WSDL, so the WSDL service can't be used
            JaxWsProxyFactoryBean factory = createClientFactory();
            factory.setEndpointName(new QName(NS, "UnknownPort"));
            factory.create(EchoPortType.class);
        } catch (RuntimeException ex) {
            // expected, the port can't be found either
        } finally {
            logger.removeHandler(handler);
        }
        assertTrue(warnings.toString(), warnings.toString().contains("PARTIAL_WSDL_BINDING_IGNORED"));
    }

    private JaxWsProxyFactoryBean createClientFactory() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(getBus());
        factory.setServiceClass(EchoPortType.class);
        factory.setWsdlURL(getClass().getResource("implicit_header.wsdl").toString());
        factory.setAddress(ADDRESS);
        return factory;
    }

    private void startServer() {
        JaxWsServerFactoryBean svr = new JaxWsServerFactoryBean();
        svr.setBus(getBus());
        svr.setServiceClass(EchoPortType.class);
        svr.setServiceBean(new EchoPortType() {
            public String echo(String in, String auth) {
                return in + ":" + auth;
            }
        });
        svr.setAddress(ADDRESS);
        svr.create();
    }
}

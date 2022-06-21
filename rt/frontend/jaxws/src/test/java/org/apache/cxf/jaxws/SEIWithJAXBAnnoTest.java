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
package org.apache.cxf.jaxws;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.BusFactory;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.service.AddNumbers;
import org.apache.cxf.jaxws.service.AddNumbersImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SEIWithJAXBAnnoTest extends AbstractJaxWsTest {
    String address = "local://localhost:9000/Hello";


    @Test
    public void testXMLList() throws Exception {

        AddNumbersImpl serviceImpl = new AddNumbersImpl();
        Endpoint.publish("local://localhost:9000/Hello", serviceImpl);

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(BusFactory.getDefaultBus());
        factory.setServiceClass(AddNumbers.class);

        factory.setAddress(address);
        AddNumbers proxy = (AddNumbers)factory.create();
        StringWriter strWriter = new StringWriter();
        LoggingOutInterceptor log = new LoggingOutInterceptor(new PrintWriter(strWriter));
        ClientProxy.getClient(proxy).getOutInterceptors().add(log);

        List<String> args = new ArrayList<>();
        args.add("str1");
        args.add("str2");
        args.add("str3");
        List<Integer> result = proxy.addNumbers(args);
        String expected = "<arg0>str1 str2 str3</arg0>";
        assertTrue("Client does not use the generated wrapper class to marshal request parameters",
                     strWriter.toString().indexOf(expected) > -1);
        assertEquals("Get the wrong result", 100, (int)result.get(0));

    }
}


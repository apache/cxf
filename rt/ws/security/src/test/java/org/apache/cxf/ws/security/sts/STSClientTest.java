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
package org.apache.cxf.ws.security.sts;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxb.JAXBContextCache;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.security.trust.STSClient;
import org.junit.Assert;
import org.junit.Test;

public class STSClientTest extends Assert {

    @Test
    public void testConfigureViaEPR() throws Exception {

        final Set<Class<?>> addressingClasses = new HashSet<Class<?>>();
        addressingClasses.add(org.apache.cxf.ws.addressing.wsdl.ObjectFactory.class);
        addressingClasses.add(org.apache.cxf.ws.addressing.ObjectFactory.class);

        JAXBContext ctx = JAXBContextCache.getCachedContextAndSchemas(addressingClasses, null, null, null,
                                                                      true).getContext();
        Unmarshaller um = ctx.createUnmarshaller();
        InputStream inStream = getClass().getResourceAsStream("epr.xml");
        JAXBElement el = (JAXBElement)um.unmarshal(inStream);
        EndpointReferenceType ref = (EndpointReferenceType)el.getValue();

        Bus bus = BusFactory.getThreadDefaultBus();
        STSClient client = new STSClient(bus);
        client.configureViaEPR(ref);

        assertEquals("http://localhost:8080/jaxws-samples-wsse-policy-trust-sts/SecurityTokenService?wsdl",
                     client.getWsdlLocation());
        assertEquals(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "SecurityTokenService"),
                     client.getServiceQName());
        assertEquals(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/", "UT_Port"),
                     client.getEndpointQName());
    }
}

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
package org.apache.cxf.systest.type_test.soap;

import java.util.Collections;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.systest.type_test.AbstractTypeTestClient5;
import org.apache.type_test.types1.FixedArray;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SOAPDocLitClientTypeTest extends AbstractTypeTestClient5 {
    protected static final String WSDL_PATH = "/wsdl/type_test/type_test_doclit_soap.wsdl";
    protected static final QName SERVICE_NAME = new QName("http://apache.org/type_test/doc", "SOAPService");
    protected static final QName PORT_NAME = new QName("http://apache.org/type_test/doc", "SOAPPort");
    static final String PORT = SOAPDocLitServerImpl.PORT;


    @Before
    public void updatePort() throws Exception {
        updateAddressPort(docClient, PORT);
    }

    @BeforeClass
    public static void startServers() throws Exception {
        boolean ok = launchServer(SOAPDocLitServerImpl.class, true);
        assertTrue("failed to launch server", ok);
        initClient(SERVICE_NAME, PORT_NAME, WSDL_PATH);
    }

    @Test
    public void testValidationFailureOnServerOut() throws Exception {
        FixedArray x = new FixedArray();
        FixedArray yOrig = new FixedArray();

        Collections.addAll(x.getItem(), 24, 42, 2008);
        Collections.addAll(yOrig.getItem(), 24, 0, 1);

        Holder<FixedArray> y = new Holder<>(yOrig);
        Holder<FixedArray> z = new Holder<>();
        try {
            docClient.testFixedArray(x, y, z);
            fail("should have thrown exception");
        } catch (SOAPFaultException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("Marshalling"));
        }
    }





}

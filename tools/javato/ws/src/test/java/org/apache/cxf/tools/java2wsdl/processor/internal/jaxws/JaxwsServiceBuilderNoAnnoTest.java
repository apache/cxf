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

package org.apache.cxf.tools.java2wsdl.processor.internal.jaxws;

import java.io.File;
import java.net.URI;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxwsServiceBuilder;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.WSDL11Generator;
import org.junit.Before;
import org.junit.Test;

public class JaxwsServiceBuilderNoAnnoTest extends ProcessorTestBase {
    JaxwsServiceBuilder builder = new JaxwsServiceBuilder();
    WSDL11Generator generator = new WSDL11Generator();


    @Before
    public void setUp() throws Exception {
        super.setUp();
        JAXBDataBinding.clearCaches();
        builder.setBus(BusFactory.getDefaultBus());
        generator.setBus(builder.getBus());
    }


    @Test
    public void testGeneratedWithElementryClass() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.classnoanno.docbare.Stock.class);
        builder.setAddress("http://localhost");
        ServiceInfo service =  builder.createService();
        generator.setServiceModel(service);
        File output = getOutputFile("stock_noanno_bare.wsdl");
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = getClass().getResource("expected/stock_noanno_bare.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }


    @Test
    public void testGeneratedWithDocWrappedClass() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.classnoanno.docwrapped.Stock.class);
        builder.setAddress("http://localhost");
        ServiceInfo service =  builder.createService();
        generator.setServiceModel(service);
        File output = getOutputFile("stock_noanno_wrapped.wsdl");
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = getClass().getResource("expected/stock_noanno_wrapped.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }


    // * Missing wsdl:types
    // * input message of binding operation "getPrice" MUST specify a value for the "namespace" attribute
    // * output message of binding operation "getPrice" MUST specify a value for the "namespace" attribute
    // CXF-527
    @Test
    public void testGeneratedWithRPCClass() throws Exception {
        builder.setServiceClass(org.apache.cxf.tools.fortest.classnoanno.rpc.Stock.class);
        builder.setAddress("http://localhost");
        ServiceInfo service =  builder.createService();
        generator.setServiceModel(service);
        File output = getOutputFile("stock_noanno_rpc.wsdl");
        generator.generate(output);
        assertTrue(output.exists());

        URI expectedFile = getClass().getResource("expected/stock_noanno_rpc.wsdl").toURI();
        assertWsdlEquals(new File(expectedFile), output);
    }

    private File getOutputFile(String fileName) {
        return new File(output, fileName);
    }
}

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
package org.apache.cxf.systest.servlet;

import com.meterware.servletunit.ServletRunner;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.testsupport.AbstractServletTest;

import org.junit.Before;
import org.junit.Test;

public class ErrorContextSerletTest extends AbstractServletTest {
    @Override
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-spring-error.xml";
    }

    @Override
    protected Bus createBus() throws BusException {
        // don't set up the bus, let the servlet do it
        return null;
    }
    
    @Before
    public void setUp() throws Exception {
        // do nothing here
             
    } 
    
    @Test
    public void testInvoke() {
        try {
            sr = new ServletRunner(getResourceAsStream(getConfiguration()), CONTEXT);
            sr.newClient().getResponse(CONTEXT_URL + "/services");
            // there expect a spring bean exception
            fail("we expect a spring bean Exception here");
        } catch (Exception ex) {
            // supprot spring 2.0.x and sping 2.5
            assertTrue("we expect a Bean Exception here",
                      ex instanceof org.springframework.beans.FatalBeanException);
        } 
    }


}

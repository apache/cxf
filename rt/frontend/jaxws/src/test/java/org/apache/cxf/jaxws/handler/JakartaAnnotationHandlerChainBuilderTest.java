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

package org.apache.cxf.jaxws.handler;

import java.util.List;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.handler.Handler;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class JakartaAnnotationHandlerChainBuilderTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testFindJakartaEmptyHandlerChainAnnotation() {
        HandlerTestImpl handlerTestImpl = new HandlerTestImpl();
        AnnotationHandlerChainBuilder chainBuilder = new AnnotationHandlerChainBuilder();
        @SuppressWarnings("rawtypes")
        List<Handler> handlers = chainBuilder
            .buildHandlerChainFromClass(handlerTestImpl.getClass(),
                                        null,
                                        null,
                                        null);
        assertNotNull(handlers);
        assertEquals(0, handlers.size());
    }

    @WebService()
    @HandlerChain(file = "./jakarta-handlers.xml", name = "TestHandlerChain")
    public class HandlerTestImpl {

    }

}

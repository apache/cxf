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

package org.apache.cxf.common.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.ibm.xml.mock.marshaller.MockMarshaller;
import com.sun.xml.bind.marshaller.MinimumEscapeHandler;
import com.sun.xml.bind.v2.runtime.JAXBContextImpl;
import com.sun.xml.bind.v2.runtime.MarshallerImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JAXBUtilsTest {

    @Test
    public void testCanSetEscapeHandlerWithStandardJaxbImpl() throws Exception {
        Marshaller m = new MarshallerImpl((JAXBContextImpl) JAXBContext.newInstance(this.getClass()), null);
        final Object mockHandler = MinimumEscapeHandler.theInstance;
        JAXBUtils.setEscapeHandler(m, mockHandler);
        assertEquals(mockHandler, m.getProperty("com.sun.xml.bind.characterEscapeHandler"));
    }

    @Test
    public void testCanSetEscapeHandlerWithIbmJaxbImpl() throws Exception {
        Marshaller m = new MockMarshaller();
        final Object mockHandler = new Object();
        JAXBUtils.setEscapeHandler(m, mockHandler);
        assertEquals(mockHandler, m.getProperty("com.sun.xml.bind.characterEscapeHandler"));
    }
}
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

package org.apache.cxf.jaxrs.model;

import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class OperationResourceInfoTest extends Assert {
    
    @ProduceMime("text/xml")
    @ConsumeMime("application/xml")
    static class TestClass {
        @ProduceMime("text/plain")
        public void doIt() {
            // empty
        };
        @ConsumeMime("application/atom+xml")
        public void doThat() {
            // empty
        };
        
    }
    
    @Test
    public void testConsumeTypes() throws Exception {
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                 TestClass.class.getMethod("doIt", new Class[]{}), 
                                 new ClassResourceInfo(TestClass.class));
        
        List<MediaType> ctypes = ori1.getConsumeTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Class resource consume type should be used", 
                   "application/xml", ctypes.get(0).toString());
        
        OperationResourceInfo ori2 = new OperationResourceInfo(
                                 TestClass.class.getMethod("doThat", new Class[]{}), 
                                 new ClassResourceInfo(TestClass.class));
        ctypes = ori2.getConsumeTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Method consume type should be used", 
                   "application/atom+xml", ctypes.get(0).toString());
    }
    
    @Test
    public void testProduceTypes() throws Exception {
        
        OperationResourceInfo ori1 = new OperationResourceInfo(
                                       TestClass.class.getMethod("doIt", new Class[]{}), 
                                       new ClassResourceInfo(TestClass.class));
        
        List<MediaType> ctypes = ori1.getProduceTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Method produce type should be used", 
                   "text/plain", ctypes.get(0).toString());
        
        OperationResourceInfo ori2 = new OperationResourceInfo(
                                 TestClass.class.getMethod("doThat", new Class[]{}), 
                                 new ClassResourceInfo(TestClass.class));
        ctypes = ori2.getProduceTypes();
        assertEquals("Single media type expected", 1, ctypes.size());
        assertEquals("Class resource produce type should be used", 
                     "text/xml", ctypes.get(0).toString());
    }

}

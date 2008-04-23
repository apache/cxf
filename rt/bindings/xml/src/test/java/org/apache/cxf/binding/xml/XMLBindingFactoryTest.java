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

package org.apache.cxf.binding.xml;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.model.BindingInfo;
import org.junit.Assert;
import org.junit.Test;

public class XMLBindingFactoryTest extends Assert {
    
    @Test
    public void testContainsInAttachmentInterceptor() {
        XMLBindingFactory xbf = new XMLBindingFactory();
        Binding b = xbf.createBinding(new BindingInfo(null, null));
        
        boolean found = false;
        for (Interceptor interseptor : b.getInInterceptors()) {
            if (interseptor instanceof AttachmentInInterceptor) {
                found = true;
            }
        }
        
        assertTrue("No in attachment interceptor found", found);
    }

}

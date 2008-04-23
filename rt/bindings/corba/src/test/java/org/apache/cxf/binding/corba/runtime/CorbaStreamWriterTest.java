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
package org.apache.cxf.binding.corba.runtime;

import javax.xml.namespace.NamespaceContext;


import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CorbaStreamWriterTest extends Assert {
    
    private CorbaStreamWriter writer;
    private NamespaceContext mock;
    
    @Before
    public void setUp() throws Exception {
        mock = EasyMock.createMock(NamespaceContext.class);
        writer = new CorbaStreamWriter(null, null, null);
    }
    
    @Test
    public void testSetNamespaceContext() throws Exception {
        writer.setNamespaceContext(mock);
        assertSame("checking namespace context. ", mock, writer.getNamespaceContext());
        
    }
    
    
}

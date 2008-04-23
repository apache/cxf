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

package org.apache.cxf.tools.common.model;

import org.junit.Assert;
import org.junit.Test;

public class JavaParameterTest extends Assert {

    @Test
    public void testGetHolderDefaultTypeValue() throws Exception {
        JavaParameter holderParameter = new JavaParameter("i", "java.lang.String", null);
        holderParameter.setHolder(true);
        holderParameter.setHolderName("javax.xml.ws.Holder");
        assertEquals("\"\"", 
                     holderParameter.getDefaultTypeValue());
        
        holderParameter = new JavaParameter("org.apache.cxf.tools.common.model.JavaParameter",
                                            "org.apache.cxf.tools.common.model.JavaParameter", null);
        holderParameter.setHolder(true);
        holderParameter.setHolderName("javax.xml.ws.Holder");
        String defaultTypeValue = holderParameter.getDefaultTypeValue();
        assertEquals("new org.apache.cxf.tools.common.model.JavaParameter()", 
                     defaultTypeValue);
    }
}

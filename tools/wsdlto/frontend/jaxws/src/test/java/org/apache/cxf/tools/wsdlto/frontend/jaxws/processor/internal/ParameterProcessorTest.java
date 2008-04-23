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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaType;
import org.junit.Assert;
import org.junit.Test;

public class ParameterProcessorTest extends Assert {
    @Test
    public void testAddParameter() throws Exception {
        ParameterProcessor processor = new ParameterProcessor(new ToolContext());

        JavaMethod method = new JavaMethod();
        JavaParameter p1 = new JavaParameter("request", String.class.getName(), null);
        p1.setStyle(JavaType.Style.IN);
        processor.addParameter(method, p1);

        JavaParameter p2 = new JavaParameter("request", String.class.getName(), null);
        p2.setStyle(JavaType.Style.OUT);
        processor.addParameter(method, p2);

        assertEquals(1, method.getParameters().size());
        assertEquals(JavaType.Style.INOUT, method.getParameters().get(0).getStyle());
    }
}

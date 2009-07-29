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
package org.apache.cxf.tools.wsdlto.jaxb;

import java.io.File;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.wsdlto.AbstractCodeGenTest;
import org.junit.Test;

public class JAXBCodeGenOptionTest extends AbstractCodeGenTest {
   
    @Test
    public void testJaxbNpa() throws Exception {
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/echo_date.wsdl"));
        env.put(ToolConstants.CFG_XJC_ARGS, "-npa"); 
        processor.setContext(env);
        processor.execute();

        assertNotNull(output);
        
        File piFile = 
            new File(output, "org/apache/cxf/tools/fortest/date/package-info.java");
        assertFalse(piFile.getAbsolutePath(), piFile.exists());
    }


}

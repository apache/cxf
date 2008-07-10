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
package org.apache.cxf.tools.wsdlto.validator;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.wsdlto.WSDLToJavaContainer;
import org.apache.cxf.tools.wsdlto.core.DataBindingProfile;
import org.apache.cxf.tools.wsdlto.core.FrontEndProfile;
import org.apache.cxf.tools.wsdlto.core.PluginLoader;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.JAXWSContainer;
import org.junit.Before;
import org.junit.Test;

    
public class ValidatorTest extends ProcessorTestBase {
    private WSDLToJavaContainer processor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        env.put(ToolConstants.CFG_VERBOSE, ToolConstants.CFG_VERBOSE);
        env.put(FrontEndProfile.class, PluginLoader.getInstance().getFrontEndProfile("jaxws"));
        env.put(DataBindingProfile.class, PluginLoader.getInstance().getDataBindingProfile("jaxb"));
        env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());        
    }
    
    @Test
    public void testXMLFormat() throws Exception {
        processor = new JAXWSContainer(null);
        env.put(ToolConstants.CFG_WSDLURL, getLocation("/wsdl2java_wsdl/xml_format_root.wsdl"));
        processor.setContext(env);

        try {
            processor.execute();
            fail("xml_format_root.wsdl is not a valid wsdl, should throws exception here");
        } catch (Exception e) {
            String expected = "Binding(Greeter_XMLBinding):BindingOperation" 
                + "({http://apache.org/xml_http_bare}sayHi)-input: empty value of rootNode attribute, "
                + "the value should be {http://apache.org/xml_http_bare}sayHi";
            assertEquals(expected, e.getMessage().trim());
        }
    }
}

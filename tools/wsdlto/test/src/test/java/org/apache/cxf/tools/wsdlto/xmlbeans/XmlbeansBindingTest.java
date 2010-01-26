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

package org.apache.cxf.tools.wsdlto.xmlbeans;

import java.io.File;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.wsdlto.WSDLToJava;
import org.junit.Test;

public class XmlbeansBindingTest extends ProcessorTestBase {
    
    @Test
    public void testEmbeddedSchema() throws Exception {
        
        try {
            String[] args = new String[] {"-classdir",
                                          output.getCanonicalPath() + "/classes", "-d",
                                          output.getCanonicalPath(), "-db", "xmlbeans",
                                          getLocation("/wsdl2java_wsdl/xmlbeanstest.wsdl")};
            WSDLToJava.main(args);       
            
            File file = new File(output, "classes/schemaorg_apache_xmlbeans/system/");
            file = new File(file.getAbsoluteFile(), file.list()[0]);
            assertTrue(file.exists());
            File stringListXSB = new File(file, "stringlisttype428ftype.xsb");          
            String contents = FileUtils.getStringFromFile(stringListXSB);
            //assertTrue(contents.indexOf("URI_SHA_1_EA71EF943F0B49ADA611FB92B3BB95A7D57BE89B" 
            //                            + "/xmlbeanstest.wsdl") != -1);
            assertTrue(contents.indexOf("/xmlbeanstest.wsdl") != -1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}

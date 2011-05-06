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

package org.apache.cxf.tools.wadlto.jaxrs;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;

import org.junit.Test;

public class JAXRSContainerTest extends ProcessorTestBase {

    @Test    
    public void testCodeGen() {
        try {
            JAXRSContainer container = new JAXRSContainer(null);
            ToolContext context = new ToolContext();

            context.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());

            context.put(ToolConstants.CFG_WSDLURL, getLocation("/wadl/bookstore.xml"));


            container.setContext(context);
            container.execute();

            assertNotNull(output.list());
            
            verifyFiles("java");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private void verifyFiles(String ext) {
        List<File> files = FileUtils.getFilesRecurse(output, ".+\\." + ext + "$");
        assertEquals(7, files.size());
        assertTrue(checkContains(files, "superbooks.Book." + ext));
        assertTrue(checkContains(files, "superbooks.Book2." + ext));
        assertTrue(checkContains(files, "superbooks.Chapter." + ext));
        assertTrue(checkContains(files, "superbooks.ObjectFactory." + ext));
        assertTrue(checkContains(files, "superbooks.package-info." + ext));
        assertTrue(checkContains(files, "org.apache.cxf.jaxrs.model.wadl.FormInterface." + ext));
        assertTrue(checkContains(files, "org.apache.cxf.jaxrs.model.wadl.BookStore." + ext));
    }
    
    private boolean checkContains(List<File> clsFiles, String name) {
        for (File f : clsFiles) {
            if (f.getAbsolutePath().replace(File.separatorChar, '.').endsWith(name)) {
                return true;
            }
        }
        return false;
    }
    
    protected String getLocation(String wsdlFile) throws URISyntaxException {
        return getClass().getResource(wsdlFile).toString();
    }
}

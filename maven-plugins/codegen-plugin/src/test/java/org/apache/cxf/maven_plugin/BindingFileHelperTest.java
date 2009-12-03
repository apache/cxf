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
package org.apache.cxf.maven_plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.maven.plugin.MojoExecutionException;

public class BindingFileHelperTest extends TestCase {
    private static final File OUTFILE = new File("target/test-data/testbinding.xml");
    private static final String TEST_WSDL_URL = "http://testwsdl";

    public void testBindingWithWsdlLocation() throws Exception {
        try {
            OUTFILE.delete();
        } catch (Exception e) {
            // Do not fail if delete fails
        }
        InputStream is = this.getClass().getResourceAsStream("/bindingWithWsdlLocation.xml");
        boolean wasSet = BindingFileHelper.setWsdlLocationAndWrite(is, new URI(TEST_WSDL_URL), OUTFILE);
        Assert.assertFalse("This binding file should not be changed", wasSet);
        Assert.assertFalse(OUTFILE.exists());
    }

    public void testBindingWithoutWsdlLocation() throws Exception {
        try {
            OUTFILE.delete();
        } catch (Exception e) {
            // Do not fail if delete fails
        }
        InputStream is = this.getClass().getResourceAsStream("/bindingWithoutWsdlLocation.xml");
        BindingFileHelper.setWsdlLocationAndWrite(is, new URI(TEST_WSDL_URL), OUTFILE);

        Document doc = BindingFileHelper.readDocument(new FileInputStream(OUTFILE));
        Element bindings = doc.getDocumentElement();
        String location = bindings.getAttribute(BindingFileHelper.LOCATION_ATTR_NAME);
        Assert.assertEquals(TEST_WSDL_URL, location);
    }

    public void testSetBindingForWsdlOption() throws MojoExecutionException {
        WsdlOption o = new WsdlOption();
        o.setWsdl("test.wsdl");
        File baseDir = new File(".");
        File tempDir = new File(baseDir, "target" + File.separator + "tempbindings");
        File bindingFile = new File(baseDir, "src/test/resources/bindingWithoutWsdlLocation.xml");
        o.setBindingFiles(new String[] {
            bindingFile.getAbsolutePath()
        });

        BindingFileHelper.setWsdlLocationInBindingsIfNotSet(baseDir, tempDir, o, null);
        String bindingFilePath = o.getBindingFiles()[0];
        File expectedBindingFile = new File(tempDir, "0-bindingWithoutWsdlLocation.xml");
        Assert.assertEquals("Binding file should be the temp file", expectedBindingFile.getAbsolutePath(),
                            bindingFilePath);
    }
}

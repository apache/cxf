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

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.junit.Assert;
import org.junit.Test;

public class Stax2DOMTest extends Assert {

    @Test
    public void testGetDocument() throws Exception {
        File wsdlFile = new File(getClass().getResource(
                "/validator_wsdl/jms_test.wsdl").toURI());
        Document doc = new Stax2DOM().getDocument(wsdlFile);
        assertDocumentContent(doc);
    }

    public void assertDocumentContent(Document doc) {
        String content = XMLUtils.toString(doc);
        assertTrue(
                content,
                content.indexOf("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"") != -1);
        assertTrue(
                content,
                content.indexOf("xmlns:x1=\"http://cxf.apache.org/hello_world_jms/types\"") != -1);
    }

    @Test
    public void testGetDocumentURL() throws Exception {
        File tempFile = createTempWsdlFile();
        Document doc = new Stax2DOM().getDocument(tempFile.toURI().toURL());
        assertDocumentContent(doc);
        assertDelete(tempFile);
    }

    @Test
    public void testGetDocumentString() throws Exception {
        File tempFile = createTempWsdlFile();
        Document doc = new Stax2DOM().getDocument(tempFile.toURI().toString());
        assertDocumentContent(doc);
        assertDelete(tempFile);
    }

    public void assertDelete(File tempFile) {
        boolean deleted = tempFile.delete();
        if (!deleted) {
            tempFile.deleteOnExit();
        }
        Assert.assertTrue(
                "Stax2DOM left the input stream open, file cannot be deleted: "
                        + tempFile, deleted);
    }

    public File createTempWsdlFile() throws URISyntaxException, IOException {
        File wsdlFile = new File(getClass().getResource(
                "/validator_wsdl/jms_test.wsdl").toURI());
        File tempFile = File.createTempFile("Stax2DOMTest", ".wsdl");
        FileOutputStream output = new FileOutputStream(tempFile);
        IOUtils.copyAndCloseInput(new FileInputStream(wsdlFile), output);
        output.close();
        return tempFile;
    }

    @Test
    public void testGetDocumentFile() throws Exception {
        File tempFile = createTempWsdlFile();
        Document doc = new Stax2DOM().getDocument(tempFile);
        assertDocumentContent(doc);
        assertDelete(tempFile);
    }
}

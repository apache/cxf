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

package org.apache.cxf.tools.wsdlto.javascript;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class WSDLToJavaScriptTest extends ProcessorTestBase {

    public int countChar(String text, char symbol) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == symbol) {
                count++;
            }
        }
        return count;
    }

    // just run with a minimum of fuss.
    @Test
    public void testGeneration() throws Exception {
        JavaScriptContainer container = new JavaScriptContainer(null);

        ToolContext context = new ToolContext();
        context.put(ToolConstants.CFG_WSDLURL, getLocation("hello_world.wsdl"));
        context.put(ToolConstants.CFG_OUTPUTDIR, output.toString());
        String[] prefixes = new String[1];
        prefixes[0] = "http://apache.org/hello_world_soap_http=murble";
        context.put(ToolConstants.CFG_JSPACKAGEPREFIX, prefixes);
        container.setContext(context);
        container.execute();

        // now we really want to check some results.
        Path path = FileSystems.getDefault().getPath(output.getPath(), "SOAPService_Test1.js");
        assertTrue(Files.isReadable(path));
        String javascript = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(javascript.contains("xmlns:murble='http://apache.org/hello_world_soap_http'"));
        assertEquals("Number of '{' does not match number of '}' in generated JavaScript.",
                countChar(javascript, '{'),
                countChar(javascript, '}'));
    }

    @Test
    public void testCXF3891() throws Exception {
        JavaScriptContainer container = new JavaScriptContainer(null);

        ToolContext context = new ToolContext();
        context.put(ToolConstants.CFG_WSDLURL, getLocation("hello_world_ref.wsdl"));
        context.put(ToolConstants.CFG_OUTPUTDIR, output.toString());
        String[] prefixes = new String[1];
        prefixes[0] = "http://apache.org/hello_world_soap_http=murble";
        context.put(ToolConstants.CFG_JSPACKAGEPREFIX, prefixes);
        container.setContext(context);
        container.execute();

        // now we really want to check some results.
        Path path = FileSystems.getDefault().getPath(output.getPath(), "SOAPService.js");
        assertTrue(Files.isReadable(path));
        String javascript = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(javascript.contains("xmlns:murble='http://apache.org/hello_world_soap_http'"));
        assertEquals("Number of '{' does not match number of '}' in generated JavaScript.",
                countChar(javascript, '{'),
                countChar(javascript, '}'));
    }

}
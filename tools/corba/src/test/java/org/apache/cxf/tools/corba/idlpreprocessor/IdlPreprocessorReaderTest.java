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
package org.apache.cxf.tools.corba.idlpreprocessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class IdlPreprocessorReaderTest extends TestCase {

    private URL findTestResource(String spec) {
        String location = "/idlpreprocessor/" + spec;
        return getClass().getResource(location);
    }

    private class ClassPathIncludeResolver implements IncludeResolver {
        public URL findSystemInclude(String spec) {
            return findUserInclude(spec);
        }

        public URL findUserInclude(String spec) {
            return findTestResource(spec);
        }
    }

    public void testResolvedInA() throws Exception {
        final String location = "A.idl";
        final IdlPreprocessorReader includeReader = createPreprocessorReader(location);
        final String expectedResultLocation = "A-resolved.idl";
        assertExpectedPreprocessingResult(expectedResultLocation, includeReader);
    }

    public void testMultiFileResolve() throws Exception {
        final String location = "B.idl";
        final IdlPreprocessorReader includeReader = createPreprocessorReader(location);
        final String expectedResultLocation = "B-resolved.idl";
        assertExpectedPreprocessingResult(expectedResultLocation, includeReader);
    }

    public void testIfElseHandling() throws Exception {
        final String location = "C.idl";
        final IdlPreprocessorReader includeReader = createPreprocessorReader(location);
        final String expectedResultLocation = "C-resolved.idl";
        assertExpectedPreprocessingResult(expectedResultLocation, includeReader);
    }

    public void testMaximumIncludeDepthIsDetected() throws IOException {
        final String location = "MaximumIncludeDepthExceeded.idl";
        try {
            final IdlPreprocessorReader preprocessor = createPreprocessorReader(location);
            consumeReader(preprocessor);
            fail("exeeding maximum include depth is not detected");
        } catch (PreprocessingException ex) {
            String msg = ex.getMessage();
            assertTrue(Pattern.matches(".*more than .* nested #includes.*", msg));
        }
    }

    public void testUnresolvableInclude() throws IOException {
        final String location = "UnresolvableInclude.idl";
        try {
            final IdlPreprocessorReader preprocessor = createPreprocessorReader(location);
            consumeReader(preprocessor);
            fail("unresolvable include not detected");
        } catch (PreprocessingException ex) {
            assertTrue(ex.getMessage().indexOf("nirvana.idl") >= 0);
            assertEquals(1, ex.getLine());
            assertTrue(ex.getUrl().getPath().endsWith("/UnresolvableInclude.idl"));
        }
    }
    
    public void testDefaultIncludeResolver() throws Exception {
        final String location = "B.idl"; 
        // uses <> notation for include
        final URL orig = findTestResource(location);
        final File origFile = new File(orig.toURI());
        final File dir = new File(origFile.getAbsolutePath()
                                  .substring(0, origFile.getAbsolutePath().indexOf(location)));
        final DefaultIncludeResolver includeResolver = new DefaultIncludeResolver(dir);
        final DefineState defineState = new DefineState(new HashMap<String, String>());
        
        final IdlPreprocessorReader includeReader = new IdlPreprocessorReader(orig,
                                                                             location,
                                                                             includeResolver,
                                                                             defineState);
        final String expectedResultLocation = "B-resolved.idl";
        assertExpectedPreprocessingResult(expectedResultLocation, includeReader);
    }

    private IdlPreprocessorReader createPreprocessorReader(final String location) throws IOException {
        final URL orig = findTestResource(location);
        final ClassPathIncludeResolver includeResolver = new ClassPathIncludeResolver();
        final DefineState defineState = new DefineState(new HashMap<String, String>());
        final IdlPreprocessorReader preprocessor = new IdlPreprocessorReader(orig,
                                                                             location,
                                                                             includeResolver,
                                                                             defineState);
        return preprocessor;
    }

    private void assertExpectedPreprocessingResult(final String expectedResultLocation,
                                                   final IdlPreprocessorReader includeReader)
        throws UnsupportedEncodingException, IOException {
        LineNumberReader oReader = new LineNumberReader(includeReader);
        InputStream resolved = findTestResource(expectedResultLocation).openStream();
        LineNumberReader rReader = new LineNumberReader(new InputStreamReader(resolved, "ISO-8859-1"));
        try {
            boolean eof = false;
            do {
                int line = rReader.getLineNumber() + 1;
                String actualLine = oReader.readLine();
                String expectedLine = rReader.readLine();
                assertEquals("difference in line " + line, expectedLine, actualLine);
                eof = actualLine == null || expectedLine == null;
            } while (!eof);
        } finally {
            rReader.close();
        }
    }

    private void consumeReader(final Reader includeReader) throws IOException {
        LineNumberReader oReader = new LineNumberReader(includeReader);
        String line = null;
        do {
            line = oReader.readLine();
        } while (line != null);
    }
}

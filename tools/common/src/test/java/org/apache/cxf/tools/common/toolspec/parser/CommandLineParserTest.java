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

package org.apache.cxf.tools.common.toolspec.parser;

import java.util.StringTokenizer;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.toolspec.ToolSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CommandLineParserTest extends Assert {
    private CommandLineParser parser;

    @Before
    public void setUp() throws Exception {
        String tsSource = "/org/apache/cxf/tools/common/toolspec/parser/resources/testtool.xml";
        ToolSpec toolspec = new ToolSpec(getClass().getResourceAsStream(tsSource), true);

        parser = new CommandLineParser(toolspec);
    }

    @Test
    public void testValidArguments() throws Exception {
        String[] args = new String[] {"-r", "-n", "test", "arg1"};
        CommandDocument result = parser.parseArguments(args);

        assertEquals("testValidArguments Failed", "test", result.getParameter("namespace"));
    }

    @Test
    public void testInvalidArgumentValue() throws Exception {
        try {
            String[] args = new String[] {"-n", "test@", "arg1"};
            parser.parseArguments(args);
            fail("testInvalidArgumentValue failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();
            assertEquals("testInvalidArgumentValue failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];
            assertTrue("Expected InvalidArgumentValue error", error instanceof ErrorVisitor.UserError);
            ErrorVisitor.UserError userError = (ErrorVisitor.UserError)error;
            assertEquals("Invalid argument value message incorrect", "-n has invalid character!", userError
                .toString());
        }
    }

    @Test
    public void testValidArgumentEnumValue() throws Exception {
        String[] args = new String[] {"-r", "-e", "true", "arg1"};        
        CommandDocument result = parser.parseArguments(args);
        assertEquals("testValidArguments Failed", "true", result.getParameter("enum"));
    }

    @Test
    public void testInvalidArgumentEnumValue() throws Exception {
        try {
            String[] args = new String[] {"-e", "wrongvalue"};
            parser.parseArguments(args);
            fail("testInvalidArgumentEnumValue failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();
            assertEquals("testInvalidArgumentEnumValu failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];
            assertTrue("Expected InvalidArgumentEnumValu error", error instanceof ErrorVisitor.UserError);
            ErrorVisitor.UserError userError = (ErrorVisitor.UserError)error;
            assertEquals("Invalid enum argument value message incorrect", 
                         "-e wrongvalue not in the enumeration value list!", 
                         userError.toString());            
        }        
    }

    @Test
    public void testValidMixedArguments() throws Exception {
        String[] args = new String[] {"-v", "-r", "-n", "test", "arg1"};
        CommandDocument result = parser.parseArguments(args);

        assertEquals("testValidMissedArguments Failed", "test", result.getParameter("namespace"));
    }

    @Test
    public void testInvalidOption() {
        try {
            String[] args = new String[] {"-n", "-r", "arg1"};
            parser.parseArguments(args);

            fail("testInvalidOption failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();

            assertEquals("testInvalidOption failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];

            assertTrue("Expected InvalidOption error", error instanceof ErrorVisitor.InvalidOption);
            ErrorVisitor.InvalidOption option = (ErrorVisitor.InvalidOption)error;

            assertEquals("Invalid option incorrect", "-n", option.getOptionSwitch());
            assertEquals("Invalid option message incorrect",
                         "Invalid option: -n is missing its associated argument", option.toString());
        }
    }

    @Test
    public void testMissingOption() {
        try {
            String[] args = new String[] {"-n", "test", "arg1"};
            parser.parseArguments(args);
            fail("testMissingOption failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();

            assertEquals("testInvalidOption failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];

            assertTrue("Expected MissingOption error", error instanceof ErrorVisitor.MissingOption);
            ErrorVisitor.MissingOption option = (ErrorVisitor.MissingOption)error;

            assertEquals("Missing option incorrect", "r", option.getOptionSwitch());
        }
    }

    @Test
    public void testMissingArgument() {
        try {
            String[] args = new String[] {"-n", "test", "-r"};
            parser.parseArguments(args);
            fail("testMissingArgument failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();

            assertEquals("testInvalidOption failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];

            assertTrue("Expected MissingArgument error", error instanceof ErrorVisitor.MissingArgument);
            ErrorVisitor.MissingArgument arg = (ErrorVisitor.MissingArgument)error;

            assertEquals("MissingArgument incorrect", "wsdlurl", arg.getArgument());
        }
    }

    @Test
    public void testDuplicateArgument() {
        try {
            String[] args = new String[] {"-n", "test", "-r", "arg1", "arg2"};
            parser.parseArguments(args);
            fail("testUnexpectedArgument failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();
            assertEquals("testInvalidOption failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];
            assertTrue("Expected UnexpectedArgument error", error instanceof ErrorVisitor.UnexpectedArgument);
        }
    }

    @Test
    public void testUnexpectedOption() {
        try {
            String[] args = new String[] {"-n", "test", "-r", "-unknown"};
            parser.parseArguments(args);
            fail("testUnexpectedOption failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();

            assertEquals("testInvalidOption failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];

            assertTrue("Expected UnexpectedOption error", error instanceof ErrorVisitor.UnexpectedOption);
            ErrorVisitor.UnexpectedOption option = (ErrorVisitor.UnexpectedOption)error;

            assertEquals("UnexpectedOption incorrect", "-unknown", option.getOptionSwitch());
        }
    }


    @Test
    public void testInvalidPackageName() {

        try {
            String[] args = new String[]{
                "-p", "/test", "arg1"
            };
            parser.parseArguments(args);
            fail("testInvalidPackageName failed");
        } catch (BadUsageException ex) {
            Object[] errors = ex.getErrors().toArray();
            assertEquals("testInvalidPackageName failed", 1, errors.length);
            CommandLineError error = (CommandLineError)errors[0];
            assertTrue("Expected InvalidArgumentValue error", error instanceof ErrorVisitor.UserError);
            ErrorVisitor.UserError userError = (ErrorVisitor.UserError)error;
            assertEquals("Invalid argument value message incorrect",
                    "-p has invalid character!", userError.toString());
        }

    }

    @Test
    public void testvalidPackageName() throws Exception {

        String[] args = new String[]{
            "-p", "http://www.iona.com/hello_world_soap_http=com.iona", "-r", "arg1"
        };
        CommandDocument result = parser.parseArguments(args);
        assertEquals("testValidPackageName Failed",
                     "http://www.iona.com/hello_world_soap_http=com.iona",
                     result.getParameter("packagename"));

    }
    

    @Test
    public void testUsage() throws Exception {
        String usage =
            "[ -n <C++ Namespace> ] [ -impl ] [ -e <Enum Value> ] -r "
            + "[ -p <[wsdl namespace =]Package Name> ]* [ -? ] [ -v ] <wsdlurl> ";
        String pUsage = parser.getUsage();

        if (isQuolifiedVersion()) {
            assertEquals("This test failed in the xerces version above 2.7.1 or the version with JDK ",
                         usage, pUsage);
        } else {
            usage = "[ -n <C++ Namespace> ] [ -impl ] [ -e <Enum Value> ] -r "
                + "-p <[wsdl namespace =]Package Name>* [ -? ] [ -v ] <wsdlurl>";
            assertEquals("This test failed in the xerces version below 2.7.1", usage.trim(), pUsage.trim());
        }
    }

    private boolean isQuolifiedVersion() {
        try {
            Class<?> c = Class.forName("org.apache.xerces.impl.Version");
            Object o = c.newInstance();
            String v =  (String) c.getMethod("getVersion").invoke(o);
            float vn = Float.parseFloat(StringUtils.getFirstFound(v, "(\\d+.\\d+)"));
            return vn >= 2.7;
        } catch (Exception e) {
            // ignore
        }
        return true;
    }

    @Test
    public void testDetailedUsage() throws Exception {
        String specialItem  = "[ -p <[wsdl namespace =]Package Name> ]*";
        if (!isQuolifiedVersion()) {
            specialItem = "-p <[wsdl namespace =]Package Name>*";
        }
            
        String[] expected = new String[]{"[ -n <C++ Namespace> ]",
                                         "Namespace",
                                         "[ -impl ]",
                                         "impl - the impl that will be used by this tool to do "
                                         + "whatever it is this tool does.",
                                         "[ -e <Enum Value> ]",
                                         "enum",
                                         "-r",
                                         "required",
                                         specialItem,
                                         "The java package name to use for the generated code."
                                         + "Also, optionally specify the wsdl namespace mapping to "
                                         + "a particular java packagename.",
                                         "[ -? ]",
                                         "help",
                                         "[ -v ]",
                                         "version",
                                         "<wsdlurl>",
                                         "WSDL/SCHEMA URL"};
        
        int index = 0;
        String lineSeparator = System.getProperty("line.separator");
        StringTokenizer st1 = new StringTokenizer(parser.getDetailedUsage(), lineSeparator);
        while (st1.hasMoreTokens()) {
            assertEquals("Failed at line " + index, expected[index++], st1.nextToken().toString().trim());
        }
    }

    @Test
    public void testOtherMethods() throws Exception {
        String tsSource = "/org/apache/cxf/tools/common/toolspec/parser/resources/testtool.xml";
        ToolSpec toolspec = new ToolSpec(getClass().getResourceAsStream(tsSource), false);
        CommandLineParser commandLineParser = new CommandLineParser(null);
        commandLineParser.setToolSpec(toolspec);
        CommandDocument commandDocument = commandLineParser.parseArguments("-r unknown");
        assertTrue(commandDocument != null);
    }

    @Test
    public void testGetDetailedUsage() {
        assertTrue("Namespace".equals(parser.getDetailedUsage("namespace")));
    }
    

    @Test
    public void testFormattedDetailedUsage() throws Exception {
        String usage = parser.getFormattedDetailedUsage();
        assertNotNull(usage);
        StringTokenizer st1 = new StringTokenizer(usage, System.getProperty("line.separator"));
        assertEquals(14, st1.countTokens());
        
        while (st1.hasMoreTokens()) {
            String s = st1.nextToken();
            if (s.indexOf("java package") != -1) {
                s = s.trim();
                assertTrue(s.charAt(s.length() - 1) != 'o');
            } else if (s.indexOf("impl - the") != -1) {
                assertTrue(s.charAt(s.length() - 1) == 'o');
            }
        }
        
    }

}

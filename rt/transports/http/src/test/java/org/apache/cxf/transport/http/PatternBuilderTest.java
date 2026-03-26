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
package org.apache.cxf.transport.http;

import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A {@code PatternBuilderTest} is ...
 */
public class PatternBuilderTest {

    @Test
    public void buildPatternWithSimpleHostname() {
        Pattern pattern = PatternBuilder.build("localhost");

        assertTrue(pattern.matcher("localhost").matches());
        assertTrue(pattern.matcher("LOCALHOST").matches());
        assertFalse(pattern.matcher("localhost-after").matches());
        assertFalse(pattern.matcher("before-localhost").matches());
    }

    @Test
    public void buildPatternWithPipelineDelimitedHostnames() {
        Pattern pattern = PatternBuilder.build("localhost|othername");

        assertTrue(pattern.matcher("localhost").matches());
        assertTrue(pattern.matcher("LOCALHOST").matches());
        assertTrue(pattern.matcher("othername").matches());
        assertFalse(pattern.matcher("somename").matches());
    }

    @Test
    public void buildPatternWithDotsInTheHostname() {
        Pattern pattern = PatternBuilder.build("www.apache.org");

        assertTrue(pattern.matcher("www.apache.org").matches());
        assertTrue(pattern.matcher("WWW.apache.ORG").matches());
        assertFalse(pattern.matcher("www1apache1org").matches());
    }

    @Test
    public void buildPatternWithLeadingWildcardInTheHostname() {
        Pattern pattern = PatternBuilder.build("*.apache.org");

        assertTrue(pattern.matcher("www.apache.org").matches());
        assertTrue(pattern.matcher("svn.apache.org").matches());
        assertFalse(pattern.matcher("apache.org").matches());
        assertTrue(pattern.matcher(".apache.org").matches()); // not very useful ...
    }

    @Test
    public void buildPatternWithTrailingWildcardInTheHostname() {
        Pattern pattern = PatternBuilder.build("www.apache.*");

        assertTrue(pattern.matcher("www.apache.org").matches());
        assertTrue(pattern.matcher("www.apache.net").matches());
        assertFalse(pattern.matcher("www.apache").matches());
        assertTrue(pattern.matcher("www.apache.").matches()); // not very useful ...
    }

    @Test
    public void buildPatternWithUppercaseLettersInTheSimpleHostname() {
        Pattern pattern = PatternBuilder.build("APACHE.ORG");

        assertTrue(pattern.matcher("apache.org").matches());
        assertTrue(pattern.matcher("APACHE.ORG").matches());
    }

    @Test
    public void buildPatternWithUppercaseLettersInTheLeadingWildcardHostname() {
        Pattern pattern = PatternBuilder.build("*.APACHE.ORG");

        assertTrue(pattern.matcher("www.apache.org").matches());
        assertTrue(pattern.matcher("svn.apache.org").matches());
        assertFalse(pattern.matcher("apache.org").matches());
    }
}
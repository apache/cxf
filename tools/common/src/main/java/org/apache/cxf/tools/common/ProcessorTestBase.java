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

package org.apache.cxf.tools.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.tools.util.ToolsStaxUtils;
import org.apache.ws.commons.schema.constants.Constants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ProcessorTestBase extends Assert {

    public static final List<String> DEFAULT_IGNORE_ATTR = Arrays.asList(new String[]{"attributeFormDefault",
                                                                                      "elementFormDefault",
                                                                                      "form",
                                                                                      "version",
                                                                                      "part@name"});
    public static final List<String> DEFAULT_IGNORE_TAG = Arrays.asList(new String[]{"sequence"});

    //CHECKSTYLE:OFF
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder() {
        protected void before() throws Throwable {
            super.before();
            output = tmpDir.getRoot();
            env.put(ToolConstants.CFG_OUTPUTDIR, output.getCanonicalPath());
        }
    };
    //CHECKSTYLE:ON

    protected File output;
    protected ToolContext env = new ToolContext();
    protected Map<QName, Set<String>> qnameAtts = new HashMap<>();

    public ProcessorTestBase() {
        addQNameAttribute(new QName(Constants.URI_2001_SCHEMA_XSD, "element"), "type");
    }

    protected final void addQNameAttribute(QName element, String local) {
        Set<String> a = qnameAtts.get(element);
        if (a == null) {
            qnameAtts.put(element, new HashSet<>());
            a = qnameAtts.get(element);
        }
        a.add(local);
    }

    @Before
    public void setUp() throws Exception {
        if (JavaUtils.isJava9Compatible()) {
            System.setProperty("org.apache.cxf.common.util.Compiler-fork", "true");
        }
    }

    @After
    public void tearDown() {
        env = null;
    }


    protected boolean isMOXy() {
        try {
            JAXBContext c = JAXBContext.newInstance(String.class);
            return c.getClass().getName().contains(".eclipse");
        } catch (JAXBException e) {
            return false;
        }
    }

    protected String getClassPath() throws URISyntaxException, IOException {
        ClassLoader loader = getClass().getClassLoader();
        StringBuilder classPath = new StringBuilder();
        if (loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader)loader).getURLs()) {
                File file = new File(url.toURI());
                String filename = file.getAbsolutePath();
                if (filename.indexOf("junit") == -1) {
                    classPath.append(filename);
                    classPath.append(System.getProperty("path.separator"));
                }
                if (filename.indexOf("surefirebooter") != -1) {
                    //surefire 2.4 uses a MANIFEST classpath that javac doesn't like
                    try (JarFile jar = new JarFile(filename)) {
                        Attributes attr = jar.getManifest().getMainAttributes();
                        if (attr != null) {
                            String cp = attr.getValue("Class-Path");
                            while (cp != null) {
                                String fileName = cp;
                                int idx = fileName.indexOf(' ');
                                if (idx != -1) {
                                    fileName = fileName.substring(0, idx);
                                    cp = cp.substring(idx + 1).trim();
                                } else {
                                    cp = null;
                                }
                                URI uri = new URI(fileName);
                                File f2 = new File(uri);
                                if (f2.exists()) {
                                    classPath.append(f2.getAbsolutePath());
                                    classPath.append(System.getProperty("path.separator"));
                                }
                            }
                        }
                    }
                }
            }
        }
        return classPath.toString();
    }

    protected String getLocation(String wsdlFile) throws URISyntaxException {
        return getClass().getResource(wsdlFile).toURI().toString();
    }

    protected File getResource(String wsdlFile) throws URISyntaxException {
        return new File(getClass().getResource(wsdlFile).toURI());
    }


    protected void assertFileEquals(String f1, String f2) {
        assertFileEquals(new File(f1), new File(f2));
    }

    protected void assertFileEquals(File location1, File location2) {
        String str1 = FileUtils.getStringFromFile(location1);
        String str2 = FileUtils.getStringFromFile(location2);

        StringTokenizer st1 = new StringTokenizer(str1, " \t\n\r\f(),");
        StringTokenizer st2 = new StringTokenizer(str2, " \t\n\r\f(),");

        // namespace declarations and wsdl message parts can be ordered
        // differently in the generated wsdl between the ibm and sun jdks.
        // So, when we encounter a mismatch, put the unmatched token in a
        // list and check this list when matching subsequent tokens.
        // It would be much better to do a proper xml comparison.
        List<String> unmatched = new ArrayList<>();
        while (st1.hasMoreTokens()) {
            String tok1 = st1.nextToken();
            String tok2 = null;
            if (unmatched.contains(tok1)) {
                unmatched.remove(tok1);
                continue;
            }
            while (st2.hasMoreTokens()) {
                tok2 = st2.nextToken();

                if (tok1.equals(tok2)) {
                    break;
                }
                unmatched.add(tok2);
            }
            assertEquals("Compare failed " + location1.getAbsolutePath()
                         + " != " + location2.getAbsolutePath(), tok1, tok2);
        }

        assertTrue(!st1.hasMoreTokens());
        assertTrue(!st2.hasMoreTokens());
        assertTrue("Files did not match: " + unmatched, unmatched.isEmpty());
    }

    public String getStringFromFile(File location) {
        return FileUtils.getStringFromFile(location);
    }

    public boolean assertXmlEquals(final File expected, final File source) throws Exception {
        List<String> attr = Arrays.asList(new String[]{"attributeFormDefault", "elementFormDefault", "form"});
        return assertXmlEquals(expected, source, attr);
    }

    public boolean assertXmlEquals(final File expected, final File source,
                                   final List<String> ignoreAttr) throws Exception {
        List<Tag> expectedTags = ToolsStaxUtils.getTags(expected);
        List<Tag> sourceTags = ToolsStaxUtils.getTags(source);

        Iterator<Tag> iterator = sourceTags.iterator();

        for (Tag expectedTag : expectedTags) {
            Tag sourceTag = iterator.next();
            if (!expectedTag.getName().equals(sourceTag.getName())) {
                throw new ComparisonFailure("Tags not equal: ",
                                            expectedTag.getName().toString(),
                                            sourceTag.getName().toString());
            }
            for (Map.Entry<QName, String> attr : expectedTag.getAttributes().entrySet()) {
                if (ignoreAttr.contains(attr.getKey().getLocalPart())) {
                    continue;
                }

                if (sourceTag.getAttributes().containsKey(attr.getKey())) {
                    if (!sourceTag.getAttributes().get(attr.getKey()).equals(attr.getValue())) {
                        throw new ComparisonFailure("Attributes not equal: ",
                                                attr.getKey() + ":" + attr.getValue(),
                                                attr.getKey() + ":"
                                                + sourceTag.getAttributes().get(attr.getKey()));
                    }
                } else {
                    throw new AssertionError("Attribute: " + attr + " is missing in the source file.");
                }
            }

            if (!StringUtils.isEmpty(expectedTag.getText())
                && !expectedTag.getText().equals(sourceTag.getText())) {
                throw new ComparisonFailure("Text not equal: ",
                                            expectedTag.getText(),
                                            sourceTag.getText());
            }
        }
        return true;
    }

    protected void assertTagEquals(Tag expected, Tag source) {
        assertTagEquals(expected, source, DEFAULT_IGNORE_ATTR, DEFAULT_IGNORE_TAG);
    }

    protected void assertAttributesEquals(QName element,
                                          Map<QName, String> q1,
                                          Map<QName, String> q2,
                                          Collection<String> ignoreAttr) {
        for (Map.Entry<QName, String>  attr : q1.entrySet()) {
            if (ignoreAttr.contains(attr.getKey().getLocalPart())
                || ignoreAttr.contains(element.getLocalPart() + "@"
                                       + attr.getKey().getLocalPart())) {
                continue;
            }

            String found = q2.get(attr.getKey());
            if (found == null) {
                throw new AssertionError("Attribute: " + attr.getKey()
                                         + " is missing in "
                                         + element);
            }
            if (!found.equals(attr.getValue())) {
                throw new ComparisonFailure("Attribute not equal: ",
                                            attr.getKey() + ":" + attr.getValue(),
                                            attr.getKey() + ":" + found);
            }
        }
    }

    protected void assertTagEquals(Tag expected, Tag source,
                                   final List<String> ignoreAttr,
                                   final List<String> ignoreTag) {
        if (!expected.getName().equals(source.getName())) {
            throw new ComparisonFailure("Tags not equal: ",
                                        expected.getName().toString(),
                                        source.getName().toString());
        }

        assertAttributesEquals(expected.getName(),
                               expected.getAttributes(), source.getAttributes(), ignoreAttr);
        assertAttributesEquals(expected.getName(),
                               source.getAttributes(), expected.getAttributes(), ignoreAttr);

        if (!StringUtils.isEmpty(expected.getText())
                && !expected.getText().equals(source.getText())) {
            throw new ComparisonFailure("Text not equal: ",
                                        expected.getText(),
                                        source.getText());
        }

        if (!expected.getTags().isEmpty()) {
            for (Tag expectedTag : expected.getTags()) {
                if (ignoreTag.contains(expectedTag.getName().getLocalPart())
                    && expectedTag.getTags().isEmpty()) {
                    continue;
                }
                Tag sourceTag = getFromSource(source, expectedTag);
                if (sourceTag == null) {
                    throw new AssertionError("\n" + expectedTag.toString()
                                             + " is missing in the source file:"
                                             + "\n" + source.toString());
                }
                assertTagEquals(expectedTag, sourceTag, ignoreAttr, ignoreTag);
            }
        }
    }

    private Tag getFromSource(Tag sourceTag, Tag expectedTag) {
        for (Tag tag : sourceTag.getTags()) {
            if (tag.equals(expectedTag)) {
                return tag;
            }
        }
        return null;
    }

    public void assertWsdlEquals(final File expected, final File source, List<String> attr, List<String> tag)
        throws Exception {
        Tag expectedTag = ToolsStaxUtils.getTagTree(expected, attr, qnameAtts);
        Tag sourceTag = ToolsStaxUtils.getTagTree(source, attr, qnameAtts);
        assertTagEquals(expectedTag, sourceTag, attr, tag);
    }

    public void assertWsdlEquals(final File expected, final File source) throws Exception {
        assertWsdlEquals(expected, source, DEFAULT_IGNORE_ATTR, DEFAULT_IGNORE_TAG);
    }

    public void assertWsdlEquals(final InputStream expected, final InputStream source,
                                 List<String> attr, List<String> tag)
        throws Exception {
        Tag expectedTag = ToolsStaxUtils.getTagTree(expected, attr, qnameAtts);
        Tag sourceTag = ToolsStaxUtils.getTagTree(source, attr, qnameAtts);
        assertTagEquals(expectedTag, sourceTag, attr, tag);
    }

    public void assertWsdlEquals(final InputStream expected, final InputStream source) throws Exception {
        assertWsdlEquals(expected, source, DEFAULT_IGNORE_ATTR, DEFAULT_IGNORE_TAG);
    }

}

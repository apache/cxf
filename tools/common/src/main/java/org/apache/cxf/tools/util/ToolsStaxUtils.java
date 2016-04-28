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

package org.apache.cxf.tools.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.Tag;

public final class ToolsStaxUtils {
   
    private ToolsStaxUtils() {
    }

    public static List<Tag> getTags(final File source) throws Exception {
        List<Tag> tags = new ArrayList<Tag>();
        List<String> ignoreEmptyTags = Arrays.asList(new String[]{"sequence"});

        try (InputStream is = new BufferedInputStream(new FileInputStream(source))) {
            XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
            Tag newTag = null;
            int count = 0;
            QName checkingPoint = null;

            Stack<Tag> stack = new Stack<Tag>();

            while (reader.hasNext()) {
                int event = reader.next();

                if (checkingPoint != null) {
                    count++;
                }

                if (event == XMLStreamReader.START_ELEMENT) {
                    newTag = new Tag();
                    newTag.setName(reader.getName());

                    if (ignoreEmptyTags.contains(reader.getLocalName())) {
                        checkingPoint = reader.getName();
                    }

                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        newTag.getAttributes().put(reader.getAttributeName(i), 
                                reader.getAttributeValue(i));
                    }
                    stack.push(newTag);
                }
                if (event == XMLStreamReader.CHARACTERS) {
                    newTag.setText(reader.getText());
                }

                if (event == XMLStreamReader.END_ELEMENT) {
                    Tag startTag = stack.pop();

                    if (checkingPoint != null && checkingPoint.equals(reader.getName())) {
                        if (count == 1) {
                            //Tag is empty, and it's in the ignore collection, so we just skip this tag
                        } else {
                            tags.add(startTag);
                        }
                        count = 0;
                        checkingPoint = null;
                    } else {
                        tags.add(startTag);
                    }
                }
            }
            reader.close();
        }
        return tags;
    }

    public static Tag getTagTree(final File source) throws Exception {
        return getTagTree(source, new ArrayList<String>());
    }

    public static Tag getTagTree(final File source, final List<String> ignoreAttr) throws Exception {
        try (InputStream is = new BufferedInputStream(new FileInputStream(source))) {
            return getTagTree(is, ignoreAttr, null);
        }
    }
    public static Tag getTagTree(final File source,
                                 final List<String> ignoreAttr,
                                 Map<QName, Set<String>> types) throws Exception {
        try (InputStream is = new BufferedInputStream(new FileInputStream(source))) {
            return getTagTree(is, ignoreAttr, types);        
        }
    }    
    public static Tag getTagTree(final InputStream is,
                                 final List<String> ignoreAttr,
                                 Map<QName, Set<String>> types) throws Exception {
        Tag root = new Tag();
        root.setName(new QName("root", "root"));

        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        Tag newTag = null;

        Tag currentTag = root;
        
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamReader.START_ELEMENT) {
                newTag = new Tag();
                newTag.setName(reader.getName());
                if (!ignoreAttr.isEmpty()) {
                    newTag.getIgnoreAttr().addAll(ignoreAttr);
                }

                for (int i = 0; i < reader.getAttributeCount(); i++) {
                    //probably a qname to a type, pull namespace in differently
                    String tp = reader.getAttributeValue(i);
                    if (isType(types, reader.getName(), reader.getAttributeName(i))) {
                        int idx = tp.indexOf(':');
                        if (idx > 0 && tp.length() > idx && tp.substring(idx + 1).indexOf(':') == -1) {
                            String pfx = tp.substring(0, idx);
                            String ns = reader.getNamespaceURI(pfx);
                            if (ns != null) {
                                tp = "{" + ns + "}" + tp.substring(idx + 1);
                            }
                        } else { 
                            String ns = reader.getNamespaceURI("");
                            if (ns != null) {
                                tp = "{" + ns + "}" + tp.substring(idx + 1);
                            }
                        }
                    }
                    newTag.getAttributes().put(reader.getAttributeName(i), 
                                               tp);
                }

                newTag.setParent(currentTag);
                currentTag.getTags().add(newTag);
                currentTag = newTag;
            }
            if (event == XMLStreamReader.CHARACTERS) {
                newTag.setText(reader.getText());
            }

            if (event == XMLStreamReader.END_ELEMENT) {
                currentTag = currentTag.getParent();
            }
        }
        reader.close();
        return root;
    }

    private static boolean isType(Map<QName, Set<String>> types, QName name, QName attributeName) {
        if (types == null) {
            return false;
        }
        Set<String> a = types.get(name);
        return a != null && a.contains(attributeName.getLocalPart());
    }

}

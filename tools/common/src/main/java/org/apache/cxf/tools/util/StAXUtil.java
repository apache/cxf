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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.InputSource;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.Tag;
import org.apache.cxf.tools.common.ToolException;

public final class StAXUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(StAXUtil.class);
    private static final XMLInputFactory XML_INPUT_FACTORY;
    static {
        XML_INPUT_FACTORY = XMLInputFactory.newInstance();
        XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
    }

    private StAXUtil() {
    }

    public static void toStartTag(XMLStreamReader r) throws XMLStreamException {
        while (!r.isStartElement() && r.hasNext()) {
            r.next();
        }
    }

    public static XMLStreamReader createFreshXMLStreamReader(InputSource source) {
        try {
            if (source.getCharacterStream() != null) {
                return XML_INPUT_FACTORY.createXMLStreamReader(source.getSystemId(),
                                                             source.getCharacterStream());
            }
            if (source.getByteStream() != null) {
                return XML_INPUT_FACTORY.createXMLStreamReader(source.getSystemId(),
                                                             source.getByteStream());
            }
            return XML_INPUT_FACTORY.createXMLStreamReader(source.getSystemId(),
                                                         new URL(source.getSystemId()).openStream());
        } catch (Exception e) {
            Message msg = new Message("FAIL_TO_CREATE_STAX", LOG);
            throw new ToolException(msg, e);
        }
    }

    public static List<Tag> getTags(final File source) throws Exception {
        List<Tag> tags = new ArrayList<Tag>();
        List<String> ignoreEmptyTags = Arrays.asList(new String[]{"sequence"});

        InputStream is = new BufferedInputStream(new FileInputStream(source));
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
        return tags;
    }

    public static Tag getTagTree(final File source) throws Exception {
        return getTagTree(source, new ArrayList<String>());
    }

    public static Tag getTagTree(final File source, final List<String> ignoreAttr) throws Exception {
        InputStream is = new BufferedInputStream(new FileInputStream(source));
        return getTagTree(is, ignoreAttr);
    }
    public static Tag getTagTree(final InputStream is, final List<String> ignoreAttr) throws Exception {
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
                    if ("type".equals(reader.getAttributeLocalName(i))
                        && "element".equals(reader.getLocalName())) {
                        //probably a qname to a type, pull namespace in differently
                        String tp = reader.getAttributeValue(i);
                        if (tp.contains(":")) {
                            String ns = tp.substring(0, tp.indexOf(":"));
                            if ("tns".equals(ns)) {
                                tp = tp.substring(tp.indexOf(":") + 1);
                            } else {
                                ns = reader.getNamespaceURI(ns);
                                tp = "{" + ns + "}" + tp.substring(tp.indexOf(":") + 1);
                            }
                        }
                        newTag.getAttributes().put(reader.getAttributeName(i), 
                                                   tp);
                    } else {
                        newTag.getAttributes().put(reader.getAttributeName(i), 
                                                   reader.getAttributeValue(i));
                    }
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

    public Tag getLastTag(Tag tag) {
        int lastIndex = tag.getTags().size() - 1;
        return tag.getTags().get(lastIndex);
    }
}

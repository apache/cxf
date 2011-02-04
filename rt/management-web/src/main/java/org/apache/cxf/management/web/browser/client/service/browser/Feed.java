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

package org.apache.cxf.management.web.browser.client.service.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

import static com.google.gwt.xml.client.Node.ELEMENT_NODE;

public class Feed {
    public static final Feed EMPTY = new Feed();

    @Nonnull
    private final List<Entry> entries;

    @Nonnull
    private final Links links;

    private Feed() {
        entries = new ArrayList<Entry>();
        links = Links.EMPTY;
    }

    public Feed(@Nonnull final Document document) {
        entries = new ArrayList<Entry>();
        new XmlParser().parseEntries(document);

        links = new Links(document);
    }

    @Nonnull
    public List<Entry> getEntries() {
        return entries;
    }

    @Nonnull
    public Links getLinks() {
        return links;
    }

    private final class XmlParser {
        private static final String ENTRY_TAG = "entry";

        private XmlParser() {
        }

        private void parseEntries(@Nonnull final Document document) {
            NodeList entryNodes = document.getElementsByTagName(ENTRY_TAG);

            if (entryNodes != null) {
                Node entryNode;
                for (int i = 0; i < entryNodes.getLength(); i++) {
                    entryNode = entryNodes.item(i);
                    assert entryNode != null;
                    
                    if (entryNode.getNodeType() == ELEMENT_NODE) {
                        entries.add(new Entry(entryNodes.item(i)));
                    }
                }
                Collections.reverse(entries);
            }
        }
    }
}

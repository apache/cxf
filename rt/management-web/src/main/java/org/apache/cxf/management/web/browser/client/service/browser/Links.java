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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

public class Links {
    public static final Links EMPTY = new Links();

    @Nullable
    private String first;

    @Nullable
    private String previous;

    @Nullable
    private String self;

    @Nullable
    private String next;

    @Nullable
    private String last;

    private Links() {
    }

    public Links(@Nonnull final Document document) {
        new XmlParser().parseLinks(document);
    }

    public boolean firstAvailable() {
        return available(first);
    }

    public boolean previousAvailable() {
        return available(previous);
    }

    public boolean selfAvailable() {
        return available(self);
    }

    public boolean nextAvailable() {
        return available(next);
    }

    public boolean lastAvailable() {
        return available(last);
    }

    @Nullable
    public String getFirst() {
        return first;
    }

    @Nullable
    public String getPrevious() {
        return previous;
    }

    @Nullable
    public String getSelf() {
        return self;
    }

    @Nullable
    public String getNext() {
        return next;
    }

    @Nullable
    public String getLast() {
        return last;
    }

    private boolean available(@Nullable final String link) {
        return link != null && !"".equals(link);
    }

    private final class XmlParser {
        private static final String FEED_TAG = "feed";
        private static final String LINK_TAG = "link";

        private static final String TYPE_ATTRIBUTE = "rel";
        private static final String URL_ATTRIBUTE = "href";

        private static final String FIRST_LINK = "first";
        private static final String PREVIOUS_LINK = "previous";
        private static final String SELF_LINK = "self";
        private static final String NEXT_LINK = "next";
        private static final String LAST_LINK = "last";

        private XmlParser() {
        }

        private void parseLinks(@Nonnull final Document document) {
            NodeList linkNodes = document.getElementsByTagName(LINK_TAG);

            if (linkNodes != null) {
                Node linkNode;
                for (int i = 0; i < linkNodes.getLength(); i++) {
                    linkNode = linkNodes.item(i);
                    if (isLinkBelongToFeed(linkNode)) {
                        setLink(linkNode);
                    }
                }
            }
        }

        private boolean isLinkBelongToFeed(@Nonnull final Node node) {
            return node.getParentNode() != null && FEED_TAG.equals(node.getParentNode().getNodeName());
        }

        private void setLink(@Nonnull final Node node) {
            Node typeNode = node.getAttributes().getNamedItem(TYPE_ATTRIBUTE);
            Node urlNode = node.getAttributes().getNamedItem(URL_ATTRIBUTE);

            if (typeNode != null && urlNode != null) {
                String typeValue = typeNode.getNodeValue();
                String urlValue = urlNode.getNodeValue();

                if (FIRST_LINK.equals(typeValue)) {
                    first = urlValue;
                } else if (PREVIOUS_LINK.equals(typeValue)) {
                    previous = urlValue;
                } else if (SELF_LINK.equals(typeValue)) {
                    self = urlValue;
                } else if (NEXT_LINK.equals(typeValue)) {
                    next = urlValue;
                } else if (LAST_LINK.equals(typeValue)) {
                    last = urlValue;
                }
            }
        }
    }
}
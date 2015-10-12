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

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

import static com.google.gwt.xml.client.Node.ELEMENT_NODE;
import static com.google.gwt.xml.client.Node.TEXT_NODE;

/**
 * The class represents log record, which is parsed from DOM representation of {@link LogRecord}.
 * <p/>
 * Log record is single piece of information log by logger. For example:
 * <code>
 * <p/>
 * </code>
 *
 * @see FeedProxy
 */
public class Entry {

    /**
     * short message
     */
    private String title;

    /**
     * long message
     */
    private String message;

    /**
     * level: 'DEBUG', 'INFO', 'WARN', 'ERROR'
     */
    private String level;

    /**
     * error message and stack trace related with occured exception
     */
    private String throwable;

    /**
     * date of occured
     */
    private Date eventTimestamp;

    @Nonnull
    private final LazyXmlParser xmlParser;

    /**
     * Constructs a new <code>Entry</code> by convert DOM representation.
     *
     * @param entryNode XML node which represent entry (nonnull);
     */
    public Entry(@Nonnull final Node entryNode) {
        xmlParser = new LazyXmlParser(entryNode);
    }

    /**
     * Returns short message of the log record.
     *
     * @return short message (nonull)
     */
    @Nonnull
    public String getTitle() {
        if (title == null) {
            title = avoidNull(xmlParser.getTitle());
        }
        return title;
    }

    /**
     * Returns long message of the log record.
     *
     * @return long message (nonull)
     */
    @Nonnull
    public String getMessage() {
        if (message == null) {
            message = avoidNull(xmlParser.getMessage());
        }
        return message;
    }

    /**
     * Returns level of the log record. Possible values: 'DEBUG', 'INFO', 'WARN', 'ERROR'.
     *
     * @return level (nonull)
     */
    @Nonnull
    public String getLevel() {
        if (level == null) {
            level = avoidNull(xmlParser.getLevel());
        }

        return level;
    }

    /**
     * Returns error message and stack trace related with occured exception.
     *
     * @return error message and stack trace (nonull)
     */
    @Nonnull
    public String getThrowable() {
        if (throwable == null) {
            throwable = avoidNull(xmlParser.getThrowable());
        }
        return throwable;
    }

    @Nullable
    public Date getEventTimestamp() {
        if (eventTimestamp == null) {
            eventTimestamp = xmlParser.getEventTimestamp();
        }
        return eventTimestamp != null ? (Date)eventTimestamp.clone() : null;
    }

    @Nonnull
    private String avoidNull(@Nullable final String value) {
        return value != null ? value : "";
    }

    private static class LazyXmlParser {
        private static final String TITLE_TAG = "title";
        private static final String MESSAGE_TAG = "message";
        private static final String LEVEL_TAG = "level";
        private static final String THROWABLE_TAG = "throwable";
        private static final String EVENT_TIMESTAMP_TAG = "date";

        private static final DateTimeFormat DATETIME_FORMATTER =
            DateTimeFormat.getFormat("yyyy-MM-ddTHH:mm:ss'.'SSSZ");

        @Nonnull
        private final Node entryNode;

        LazyXmlParser(@Nonnull final Node entryNode) {
            assert "entry".equals(entryNode.getNodeName());
            assert entryNode.getNodeType() == ELEMENT_NODE;

            this.entryNode = entryNode;
        }

        @Nullable
        private String getTagValue(@Nonnull final String tagName) {
            Node node = getUniqueElementByTagName(tagName);

            return node != null ? getTextValue(node) : null;
        }

        @Nullable
        private Node getUniqueElementByTagName(@Nonnull final String tagName) {
            NodeList nodes = ((Element)entryNode).getElementsByTagName(tagName);
            return nodes.getLength() == 1 ? nodes.item(0) : null;
        }

        @Nullable
        private String getTextValue(@Nonnull final Node node) {
            Node child = node.getFirstChild();

            if (child != null && child.getNodeType() == TEXT_NODE) {
                return child.getNodeValue();
            }

            return null;
        }

        @Nullable
        public String getTitle() {
            return getTagValue(TITLE_TAG);
        }

        @Nullable
        public String getMessage() {
            return getTagValue(MESSAGE_TAG);
        }

        @Nullable
        public String getLevel() {
            return getTagValue(LEVEL_TAG);
        }

        @Nullable
        public String getThrowable() {
            return getTagValue(THROWABLE_TAG);
        }

        @Nullable
        public Date getEventTimestamp() {
            String value = getTagValue(EVENT_TIMESTAMP_TAG);
            return value != null ? DATETIME_FORMATTER.parse(value) : null;
        }
    }
}

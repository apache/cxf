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
package org.apache.cxf.jaxrs.ext.logging.atom.converter;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.commons.lang.Validate;
import org.apache.cxf.jaxrs.ext.atom.AbstractEntryBuilder;
import org.apache.cxf.jaxrs.ext.atom.AbstractFeedBuilder;
import org.apache.cxf.jaxrs.ext.logging.LogRecord;
import org.apache.cxf.jaxrs.ext.logging.LogRecordsList;

/**
 * Converter producing ATOM Feeds on standalone Entries with LogRecords or LogRecordsLists embedded as content
 * or extension. For configuration details see constructor documentation.
 */
public final class StandardConverter implements Converter {

    /** Conversion output */
    public enum Output {
        FEED,
        ENTRY
    }

    /** Quantities of entries in feed or logrecords in entry */
    public enum Multiplicity {
        ONE,
        MANY
    }

    /** Entity format */
    public enum Format {
        CONTENT,
        EXTENSION
    }

    private Factory factory;
    private Marshaller marsh;
    private DateFormat df;
    private Converter worker;
    private Postprocessor postprocessor;

    /**
     * Creates configured converter with default post-processing of feeds/entries mandatory properties.
     * Regardless of "format", combination of "output" and "multiplicity" flags can be interpreted as follow:
     * <ul>
     * <li>ENTRY ONE - for each log record one entry is produced, converter returns list of entries</li>
     * <li>ENTRY MANY - list of log records is packed in one entry, converter return one entry.</li>
     * <li>FEED ONE - list of log records is packed in one entry, entry is inserted to feed, converter returns
     * one feed.</li>
     * <li>FEED MANY - for each log record one entry is produced, entries are collected in one feed, converter
     * returns one feed.</li>
     * </ul>
     * 
     * @param output whether root elements if Feed or Entry (e.g. for AtomPub).
     * @param multiplicity for output==FEED it is multiplicity of entities in feed for output==ENTITY it is
     *            multiplicity of log records in entity.
     * @param format log records embedded as entry content or extension.
     */
    public StandardConverter(Output output, Multiplicity multiplicity, Format format) {
        this(output, multiplicity, format, new DefaultPostprocessor());
    }

    /**
     * Creates configured converter with feeds/entries post-processing based on data provided by feed and
     * entry builders.
     */
    public StandardConverter(Output output, Multiplicity multiplicity, Format format,
                             AbstractFeedBuilder<List<LogRecord>> feedBuilder,
                             AbstractEntryBuilder<List<LogRecord>> entryBuilder) {
        this(output, multiplicity, format, new BuilderPostprocessor(feedBuilder, entryBuilder));
    }

    private StandardConverter(Output output, Multiplicity multiplicity, Format format,
                              Postprocessor postprocessor) {
        Validate.notNull(output, "output is null");
        Validate.notNull(multiplicity, "multiplicity is null");
        Validate.notNull(format, "format is null");
        Validate.notNull(postprocessor, "interceptor is null");
        configure(output, multiplicity, format);
        this.postprocessor = postprocessor;
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        factory = Abdera.getNewFactory();
        try {
            marsh = JAXBContext.newInstance(LogRecordsList.class).createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public List<? extends Element> convert(List<LogRecord> records) {
        return worker.convert(records);
    }

    private void configure(final Output output, final Multiplicity multiplicity, final Format format) {
        if (output == Output.ENTRY && multiplicity == Multiplicity.ONE) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces many entries, each entry with one log record
                    List<Element> ret = new ArrayList<Element>();
                    for (LogRecord record : records) {
                        Entry e;
                        if (format == Format.CONTENT) {
                            e = createEntry(createContent(record));
                        } else {
                            e = createEntry(createExtension(record));
                        }
                        ret.add(e);
                        postprocessor.afterEntry(e, Collections.singletonList(record));
                    }
                    return ret;
                }
            };
        }
        if (output == Output.ENTRY && multiplicity == Multiplicity.MANY) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces one entry with list of all log records
                    Entry e;
                    if (format == Format.CONTENT) {
                        e = createEntry(createContent(records));
                    } else {
                        e = createEntry(createExtension(records));
                    }
                    postprocessor.afterEntry(e, records);
                    return Arrays.asList(e);
                }
            };
        }
        if (output == Output.FEED && multiplicity == Multiplicity.ONE) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces one feed with one entry with list of all log records
                    Entry e;
                    if (format == Format.CONTENT) {
                        e = createEntry(createContent(records));
                    } else {
                        e = createEntry(createExtension(records));
                    }
                    postprocessor.afterEntry(e, records);
                    Feed f = createFeed(e);
                    postprocessor.afterFeed(f, records);
                    return Arrays.asList(f);
                }
            };
        }
        if (output == Output.FEED && multiplicity == Multiplicity.MANY) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces one feed with many entries, each entry with one log record
                    List<Entry> entries = new ArrayList<Entry>();
                    for (LogRecord record : records) {
                        Entry e;
                        if (format == Format.CONTENT) {
                            e = createEntry(createContent(record));
                        } else {
                            e = createEntry(createExtension(record));
                        }
                        entries.add(e);
                        postprocessor.afterEntry(e, Collections.singletonList(record));
                    }
                    Feed f = createFeed(entries);
                    postprocessor.afterFeed(f, records);
                    return Arrays.asList(f);
                }
            };
        }
        if (worker == null) {
            throw new IllegalArgumentException("Unsupported configuration");
        }
    }

    private String createContent(LogRecord record) {
        StringWriter writer = new StringWriter();
        try {
            marsh.marshal(record, writer);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private String createContent(List<LogRecord> records) {
        StringWriter writer = new StringWriter();
        LogRecordsList list = new LogRecordsList();
        list.setLogRecords(records);
        try {
            marsh.marshal(list, writer);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private ExtensibleElement createExtension(LogRecord record) {
        ExtensibleElement erec = factory.newExtensionElement(qn("logRecord"));
        String date = df.format(record.getEventTimestamp());
        // timezone in date does not have semicolon as XML Date requires
        // e.g we have "2009-11-23T22:03:53.996+0100"
        // instead of "2009-11-23T22:03:53.996+01:00"
        date = date.substring(0, date.length() - 2) + ":" + date.substring(date.length() - 2);
        // forget about single line "addExtension().setText()" since
        // javac failure "org.apache.abdera.model.Element cannot be dereferenced"
        Element e = erec.addExtension(qn("eventTimestamp"));
        e.setText(date);
        e = erec.addExtension(qn("level"));
        e.setText(record.getLevel().toString());
        e = erec.addExtension(qn("loggerName"));
        e.setText(record.getLoggerName());
        e = erec.addExtension(qn("message"));
        e.setText(record.getMessage());
        e = erec.addExtension(qn("threadName"));
        e.setText(record.getThreadName());
        e = erec.addExtension(qn("throwable"));
        e.setText(record.getThrowable());
        return erec;
    }

    private QName qn(String name) {
        return new QName("http://cxf.apache.org/jaxrs/log", name, "log");
    }

    private ExtensibleElement createExtension(List<LogRecord> records) {
        ExtensibleElement list = factory.newExtensionElement(qn("logRecordsList"));
        for (LogRecord rec : records) {
            list.addExtension(createExtension(rec));
        }
        return list;
    }

    private Entry createEntry(String content) {
        Entry entry = factory.newEntry();
        entry.setContent(content, Content.Type.XML);
        return entry;
    }

    private Entry createEntry(ExtensibleElement ext) {
        Entry entry = factory.newEntry();
        entry.addExtension(ext);
        return entry;
    }

    private Feed createFeed(Entry entry) {
        Feed feed = factory.newFeed();
        feed.addEntry(entry);
        return feed;
    }

    private Feed createFeed(List<Entry> entries) {
        Feed feed = factory.newFeed();
        for (Entry entry : entries) {
            feed.addEntry(entry);
        }
        return feed;
    }

    /**
     * Post-processing for feeds/entries properties customization eg setup of dates, titles, author etc.
     */
    private interface Postprocessor {

        /** Called after entry creation for given log records. */
        void afterEntry(Entry entry, List<LogRecord> records);

        /** Called after feed creation; at this stage feed has associated entries. */
        void afterFeed(Feed feed, List<LogRecord> records);
    }

    private static class DefaultPostprocessor implements Postprocessor {
        public void afterEntry(Entry entry, List<LogRecord> records) {
            // required fields (see RFC 4287)
            entry.setId("uuid:" + UUID.randomUUID().toString());
            entry.addAuthor("CXF");
            entry.setTitle(String.format("Entry with %d log record(s)", records.size()));
            entry.setUpdated(new Date());
        }

        public void afterFeed(Feed feed, List<LogRecord> records) {
            // required fields (see RFC 4287)
            feed.setId("uuid:" + UUID.randomUUID().toString());
            feed.addAuthor("CXF");
            feed.setTitle(String.format("Feed with %d entry(ies)", feed.getEntries().size()));
            feed.setUpdated(new Date());
        }
    }

    private static class BuilderPostprocessor implements Postprocessor {
        private AbstractFeedBuilder<List<LogRecord>> feedBuilder;
        private AbstractEntryBuilder<List<LogRecord>> entryBuilder;

        public BuilderPostprocessor(AbstractFeedBuilder<List<LogRecord>> feedBuilder,
                                    AbstractEntryBuilder<List<LogRecord>> entryBuilder) {
            Validate.notNull(feedBuilder, "feedBuilder is null");
            Validate.notNull(entryBuilder, "entryBuilder is null");
            this.feedBuilder = feedBuilder;
            this.entryBuilder = entryBuilder;
        }

        public void afterEntry(Entry entry, List<LogRecord> records) {
            entry.setId(entryBuilder.getId(records));
            entry.addAuthor(entryBuilder.getAuthor(records));
            entry.setTitle(entryBuilder.getTitle(records));
            entry.setUpdated(entryBuilder.getUpdated(records));
            entry.setBaseUri(entryBuilder.getBaseUri(records));
            List<String> categories = entryBuilder.getCategories(records);
            if (categories != null) {
                for (String category : categories) {
                    entry.addCategory(category);
                }
            }
            Map<String, String> links = entryBuilder.getLinks(records);
            if (links != null) {
                for (java.util.Map.Entry<String, String> mapEntry : links.entrySet()) {
                    entry.addLink(mapEntry.getKey(), mapEntry.getValue());
                }
            }
            entry.setPublished(entryBuilder.getPublished(records));
            entry.setSummary(entryBuilder.getSummary(records));
        }

        public void afterFeed(Feed feed, List<LogRecord> records) {
            feed.setId(feedBuilder.getId(records));
            feed.addAuthor(feedBuilder.getAuthor(records));
            feed.setTitle(feedBuilder.getTitle(records));
            feed.setUpdated(feedBuilder.getUpdated(records));
            feed.setBaseUri(feedBuilder.getBaseUri(records));
            List<String> categories = feedBuilder.getCategories(records);
            if (categories != null) {
                for (String category : categories) {
                    feed.addCategory(category);
                }
            }
            Map<String, String> links = feedBuilder.getLinks(records);
            if (links != null) {
                for (java.util.Map.Entry<String, String> mapEntry : links.entrySet()) {
                    feed.addLink(mapEntry.getKey(), mapEntry.getValue());
                }
            }
        }
    }
}

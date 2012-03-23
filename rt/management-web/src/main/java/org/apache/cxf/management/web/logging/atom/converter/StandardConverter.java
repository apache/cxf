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
package org.apache.cxf.management.web.logging.atom.converter;

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
import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.commons.lang.Validate;
import org.apache.cxf.jaxrs.provider.atom.AbstractEntryBuilder;
import org.apache.cxf.jaxrs.provider.atom.AbstractFeedBuilder;
import org.apache.cxf.management.web.logging.LogRecord;
import org.apache.cxf.management.web.logging.LogRecords;

/**
 * Converter producing ATOM Feeds on standalone Entries with LogRecords or LogRecordsLists embedded as content
 * or extension. For configuration details see constructor documentation.
 */
public class StandardConverter implements Converter {

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
    private JAXBContext context;
    private DateFormat df;
    private Converter worker;
    private AbstractFeedBuilder<List<LogRecord>> feedBuilder;
    private AbstractEntryBuilder<List<LogRecord>> entryBuilder;

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
        this(output, multiplicity, format, null, null);
    }

    /**
     * Creates configured converter with feeds/entries post-processing based on data provided by feed and
     * entry builders.
     */
    public StandardConverter(Output output, Multiplicity multiplicity, Format format,
                             AbstractFeedBuilder<List<LogRecord>> feedBuilder,
                             AbstractEntryBuilder<List<LogRecord>> entryBuilder) {
        Validate.notNull(output, "output is null");
        Validate.notNull(multiplicity, "multiplicity is null");
        Validate.notNull(format, "format is null");
        this.feedBuilder = feedBuilder;
        this.entryBuilder = entryBuilder;
        
        configure(output, multiplicity, format); //NOPMD
        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        factory = Abdera.getNewFactory();
        try {
            context = JAXBContext.newInstance(LogRecords.class, LogRecord.class);
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
                public List<Entry> convert(List<LogRecord> records) {
                    return createEntries(format, records);
                }
            };
        }
        if (output == Output.ENTRY && multiplicity == Multiplicity.MANY) {
            worker = new Converter() {
                public List<Entry> convert(List<LogRecord> records) {
                    // produces one entry with list of all log records
                    return Arrays.asList(createEntryFromList(format, records));
                }
            };
        }
        if (output == Output.FEED && multiplicity == Multiplicity.ONE) {
            worker = new Converter() {
                public List<Feed> convert(List<LogRecord> records) {
                    // produces one feed with one entry with list of all log records
                    return Arrays.asList(createFeedWithSingleEntry(format, records));
                }
            };
        }
        if (output == Output.FEED && multiplicity == Multiplicity.MANY) {
            worker = new Converter() {
                public List<Feed> convert(List<LogRecord> records) {
                    // produces one feed with many entries, each entry with one log record
                    return Arrays.asList(createFeed(format, records));
                }
            };
        }
        if (worker == null) {
            throw new IllegalArgumentException("Unsupported configuration");
        }
    }

    private List<Entry> createEntries(Format format, List<LogRecord> records) {
        List<Entry> entries = new ArrayList<Entry>();
        for (int i = 0; i < records.size(); i++) {
            entries.add(createEntryFromRecord(format, records.get(i), i));
        }
        return entries;
    }
    
    private Entry createEntryFromList(Format format, List<LogRecord> records) {
        Entry e = createEntry(records, 0);
        if (format == Format.CONTENT) {
            setEntryContent(e, createContent(records));
        } else {
            setEntryContent(e, createExtension(records));
        }
        return e;
    }
    
    private Entry createEntryFromRecord(Format format, LogRecord record, int entryIndex) {
        Entry e = createEntry(Collections.singletonList(record), entryIndex);
        if (format == Format.CONTENT) {
            setEntryContent(e, createContent(record));
        } else {
            setEntryContent(e, createExtension(record));
        }
        return e;
    }
    
    private String createContent(LogRecord record) {
        StringWriter writer = new StringWriter();
        try {
            context.createMarshaller().marshal(record, writer);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private String createContent(List<LogRecord> records) {
        StringWriter writer = new StringWriter();
        LogRecords list = new LogRecords();
        list.setLogRecords(records);
        try {
            context.createMarshaller().marshal(list, writer);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private ExtensibleElement createExtension(LogRecord record) {
        ExtensibleElement erec = factory.newExtensionElement(qn("logRecord"));
        
        // forget about single line "addExtension().setText()" since
        // javac failure "org.apache.abdera.model.Element cannot be dereferenced"
        Element e = erec.addExtension(qn("eventTimestamp"));
        e.setText(toAtomDateFormat(record.getDate()));
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

    private String toAtomDateFormat(Date d) {
        String date = df.format(d);
        // timezone in date does not have semicolon as XML Date requires
        // e.g we have "2009-11-23T22:03:53.996+0100"
        // instead of "2009-11-23T22:03:53.996+01:00"
        return date.substring(0, date.length() - 2) + ":" + date.substring(date.length() - 2);
    }
    
    private QName qn(String name) {
        return new QName("http://cxf.apache.org/log", name, "log");
    }

    private ExtensibleElement createExtension(List<LogRecord> records) {
        ExtensibleElement list = factory.newExtensionElement(qn("logRecords"));
        for (LogRecord rec : records) {
            list.addExtension(createExtension(rec));
        }
        return list;
    }

    private Entry createEntry(List<LogRecord> records, int entryIndex) {
        Entry entry = factory.newEntry();
        setDefaultEntryProperties(entry, records, entryIndex);
        
        return entry;
    }
    
    private void setEntryContent(Entry e, String content) {
        e.setContent(content, Content.Type.XML);
    }

    private void setEntryContent(Entry e, ExtensibleElement ext) {
        e.addExtension(ext);
    }

    private Feed createFeedWithSingleEntry(Format format, List<LogRecord> records) {
        
        Feed feed = factory.newFeed();
        feed.addEntry(createEntryFromList(format, records));
        setDefaultFeedProperties(feed, records);
        return feed;
    }
    
    private Feed createFeed(Format format, List<LogRecord> records) {
        
        Feed feed = factory.newFeed();
        List<Entry> entries = createEntries(format, records);
        for (Entry entry : entries) {
            feed.addEntry(entry);
        }
        setDefaultFeedProperties(feed, records);
        return feed;
    }

    protected void setDefaultFeedProperties(Feed feed, List<LogRecord> records) {
        if (feedBuilder != null) {
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
        } else {
            feed.setId("uuid:" + UUID.randomUUID().toString());
            feed.addAuthor("CXF");
            feed.setTitle("CXF Service Log Entries");
            feed.setUpdated(new Date());
        }
    }
    
    protected void setDefaultEntryProperties(Entry entry, List<LogRecord> records,
                                             int entryIndex) {
        if (entryBuilder != null) {
            entry.setId(entryBuilder.getId(records));
            entry.addAuthor(entryBuilder.getAuthor(records));
            entry.setTitle(entryBuilder.getTitle(records));
            entry.setUpdated(entryBuilder.getUpdated(records));
            entry.setBaseUri(entryBuilder.getBaseUri(records));
            entry.setSummary(entryBuilder.getSummary(records));
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
        } else {    
            entry.addAuthor("CXF");
            if (records.size() != 1) {
                entry.setId("uuid:" + UUID.randomUUID().toString());
                entry.setTitle(String.format("Entry with %d log record(s)", 
                                             records.size()));
            } else {
                entry.setId(records.get(0).getId());
                entry.setTitle("Log record with level " + records.get(0).getLevel().toString());
                entry.setSummary(records.get(0).getLoggerName() + " : " + records.get(0).getMessage());
            }
            if (records.size() > 0) {
                entry.setUpdated(toAtomDateFormat(records.get(0).getDate()));
            }
        }
    }
    
}

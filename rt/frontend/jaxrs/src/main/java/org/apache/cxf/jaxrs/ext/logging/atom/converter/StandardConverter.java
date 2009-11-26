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
import java.util.List;

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
import org.apache.cxf.jaxrs.ext.logging.LogRecord;
import org.apache.cxf.jaxrs.ext.logging.LogRecordsList;

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
    private Marshaller marsh;
    private DateFormat df;
    private Converter worker;

    /**
     * Creates configured converter. Regardless of "format", combination of "output" and "multiplicity" flags
     * can be interpreted as follow:
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
        Validate.notNull(output, "output is null");
        Validate.notNull(multiplicity, "multiplicity is null");
        Validate.notNull(format, "format is null");
        configure(output, multiplicity, format);
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
                        if (format == Format.CONTENT) {
                            ret.add(createEntry(createContent(record)));
                        } else {
                            ret.add(createEntry(createExtension(record)));
                        }
                    }
                    return ret;
                }
            };
        }
        if (output == Output.ENTRY && multiplicity == Multiplicity.MANY) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces one entry with list of all log records
                    if (format == Format.CONTENT) {
                        return Arrays.asList(createEntry(createContent(records)));
                    } else {
                        return Arrays.asList(createEntry(createExtension(records)));
                    }
                }
            };
        }
        if (output == Output.FEED && multiplicity == Multiplicity.ONE) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces one feed with one entry with list of all log records
                    if (format == Format.CONTENT) {
                        return Arrays.asList(createFeed(createEntry(createContent(records))));
                    } else {
                        return Arrays.asList(createFeed(createEntry(createExtension(records))));
                    }
                }
            };
        }
        if (output == Output.FEED && multiplicity == Multiplicity.MANY) {
            worker = new Converter() {
                public List<? extends Element> convert(List<LogRecord> records) {
                    // produces one feed with many entries, each entry with one log record
                    List<Entry> entries = new ArrayList<Entry>();
                    for (LogRecord record : records) {
                        if (format == Format.CONTENT) {
                            entries.add(createEntry(createContent(record)));
                        } else {
                            entries.add(createEntry(createExtension(record)));
                        }
                    }
                    return Arrays.asList(createFeed(entries));
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
}

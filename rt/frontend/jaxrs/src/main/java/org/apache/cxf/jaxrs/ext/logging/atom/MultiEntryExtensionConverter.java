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
package org.apache.cxf.jaxrs.ext.logging.atom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.logging.LogRecord;

/**
 * Multiple entries in feed, each entry with list of log records embedded as ATOM extension.
 */
public class MultiEntryExtensionConverter implements Converter {

    private Factory factory = Abdera.getNewFactory();
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public Element convert(List<LogRecord> records) {
        Feed feed = factory.newFeed();
        for (LogRecord rec : records) {
            Entry entry = factory.newEntry();
            feed.addEntry(entry);
            ExtensibleElement erec = entry.addExtension(qn("logRecord"));
            String date = df.format(rec.getEventTimestamp());
            // timezone in date does not have semicolon as XML Date requires
            // e.g we have "2009-11-23T22:03:53.996+0100"
            // instead of "2009-11-23T22:03:53.996+01:00"
            date = date.substring(0, date.length() - 2) + ":" + date.substring(date.length() - 2);
            // forget about single line "addExtension().setText()" since
            // javac failure "org.apache.abdera.model.Element cannot be dereferenced"
            Element e = erec.addExtension(qn("eventTimestamp"));
            e.setText(date);
            e = erec.addExtension(qn("level"));
            e.setText(rec.getLevel().toString());
            e = erec.addExtension(qn("loggerName"));
            e.setText(rec.getLoggerName());
            e = erec.addExtension(qn("message"));
            e.setText(rec.getMessage());
            e = erec.addExtension(qn("threadName"));
            e.setText(rec.getThreadName());
            e = erec.addExtension(qn("throwable"));
            e.setText(rec.getThrowable());
        }
        return feed;
    }

    private QName qn(String name) {
        return new QName("http://cxf.apache.org/jaxrs/log", name, "log");
    }
}

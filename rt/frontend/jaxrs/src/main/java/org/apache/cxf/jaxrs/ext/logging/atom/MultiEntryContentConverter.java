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

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.logging.LogRecord;
import org.apache.cxf.jaxrs.ext.logging.LogRecordsList;

/**
 * Multiple entries in feed, each entry with one log record from list embedded as XML content.
 */
public class MultiEntryContentConverter implements Converter {

    private Factory factory;
    private Marshaller marsh;

    public MultiEntryContentConverter() {
        factory = Abdera.getNewFactory();
        try {
            marsh = JAXBContext.newInstance(LogRecordsList.class).createMarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public Element convert(List<LogRecord> records) {
        Feed feed = factory.newFeed();
        for (LogRecord record : records) {
            Entry entry = factory.newEntry();
            feed.addEntry(entry);
            StringWriter writer = new StringWriter();
            try {
                marsh.marshal(record, writer);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
            entry.setContent(writer.toString(), Content.Type.XML);
        }
        return feed;
    }
}

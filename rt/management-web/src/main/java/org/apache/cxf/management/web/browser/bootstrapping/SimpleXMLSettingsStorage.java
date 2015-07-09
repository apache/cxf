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

package org.apache.cxf.management.web.browser.bootstrapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.Validate;
import org.apache.cxf.staxutils.StaxUtils;

public class SimpleXMLSettingsStorage implements SettingsStorage {
    private static final String DEFAULT_FILENAME = "logbrowser-settings.xml";
    private static final Settings DEFAULT_SETTINGS = new Settings();

    private final String filename;
    private final Marshaller marshaller;

    private Entries entries;

    public SimpleXMLSettingsStorage() {
        this(DEFAULT_FILENAME);
    }

    public SimpleXMLSettingsStorage(final String filename) {
        Validate.notNull(filename, "filename is null");
        Validate.notEmpty(filename, "filename is empty");
        this.filename = filename;

        try {
            JAXBContext context = JAXBContext.newInstance(Entries.class, Entry.class,
                    Settings.class, Subscription.class);

            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            
            File file = new File(filename);
            if (file.exists()) {
                Unmarshaller unmarshaller = context.createUnmarshaller();
                XMLStreamReader reader = StaxUtils.createXMLStreamReader(new FileInputStream(file));
                entries = (Entries) unmarshaller.unmarshal(reader);
            }

            if (entries == null) {
                entries = new Entries();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Settings getSettings(final String username) {
        Validate.notNull(username, "username is null");
        Validate.notEmpty(username, "username is empty");

        Entry entry = getCachedEntry(username);
        return entry != null ? entry.getSettings() : DEFAULT_SETTINGS;
    }

    public synchronized void setSettings(final String username, final Settings settings) {
        Validate.notNull(username, "username is null");
        Validate.notEmpty(username, "username is empty");

        Entry entry = getCachedEntry(username);
        if (entry != null) {
            entry.setSettings(settings);
            entry.setModified(getCurrentTime());
        } else {
            entries.getEntries().add(new Entry(username, getCurrentTime(), settings));
        }

        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(filename);
            marshaller.marshal(entries, outputStream);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Entry getCachedEntry(final String username) {
        assert username != null;
        assert !"".equals(username);

        for (Entry entry : entries.getEntries()) {
            if (username.equals(entry.getUsername())) {
                return entry;
            }
        }
        return null;
    }

    private XMLGregorianCalendar getCurrentTime() {
        try {
            return DatatypeFactory.newInstance()
                .newXMLGregorianCalendar((GregorianCalendar) GregorianCalendar.getInstance());
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @XmlRootElement(namespace = "http://cxf.apache.org/log")
    private static class Entries {
        private List<Entry> entries;

        @XmlElement(name = "entry", namespace = "http://cxf.apache.org/log")
        public List<Entry> getEntries() {
            if (entries == null) {
                entries = new ArrayList<Entry>();
            }
            return this.entries;
        }
    }

    @XmlRootElement(namespace = "http://cxf.apache.org/log")
    private static class Entry {
        private Settings settings;
        private String username;
        private XMLGregorianCalendar modified;

        private Entry() { }

        Entry(final String username, final XMLGregorianCalendar modified, final Settings settings) {
            this.settings = settings;
            this.username = username;
            this.modified = modified;
        }

        @XmlElement(required = true, namespace = "http://cxf.apache.org/log")
        public Settings getSettings() {
            return settings;
        }

        public void setSettings(Settings value) {
            this.settings = value;
        }

        @XmlAttribute(name = "username", namespace = "http://cxf.apache.org/log")
        public String getUsername() {
            return username;
        }

        public void setUsername(String value) {
            this.username = value;
        }

        @XmlAttribute(name = "modified", namespace = "http://cxf.apache.org/log")
        @XmlSchemaType(name = "date")        
        public XMLGregorianCalendar getModified() {
            return modified;
        }

        public void setModified(XMLGregorianCalendar value) {
            this.modified = value;
        }
    }
}

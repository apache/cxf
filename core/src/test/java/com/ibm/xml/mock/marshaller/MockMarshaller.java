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

package com.ibm.xml.mock.marshaller;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.xml.sax.ContentHandler;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;

public class MockMarshaller implements Marshaller {

    private final Map<String, Object> properties = new HashMap<>();

    @Override
    public void marshal(Object jaxbElement, Result result) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, OutputStream os) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, File output) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, Writer writer) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, Node node) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public Node getNode(Object contentTree) throws JAXBException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProperty(String name, Object value) throws PropertyException {
        properties.put(name, value);
    }

    @Override
    public Object getProperty(String name) throws PropertyException {
        return properties.get(name);
    }

    @Override
    public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        // TODO Auto-generated method stub

    }

    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <A extends XmlAdapter<?, ?>> void setAdapter(A adapter) {
        // TODO Auto-generated method stub

    }

    @Override
    public <A extends XmlAdapter<?, ?>> void setAdapter(Class<A> type, A adapter) {
        // TODO Auto-generated method stub

    }

    @Override
    public <A extends XmlAdapter<?, ?>> A getAdapter(Class<A> type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setAttachmentMarshaller(AttachmentMarshaller am) {
        // TODO Auto-generated method stub

    }

    @Override
    public AttachmentMarshaller getAttachmentMarshaller() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSchema(Schema schema) {
        // TODO Auto-generated method stub

    }

    @Override
    public Schema getSchema() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setListener(Listener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public Listener getListener() {
        // TODO Auto-generated method stub
        return null;
    }

}

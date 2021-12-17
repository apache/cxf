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

package org.apache.cxf.systest.jaxrs.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

import javax.xml.bind.Validator;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.UnmarshallerHandler;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;

@Provider
public class CXFJaxbContextResolver implements ContextResolver<JAXBContext> {
    @SuppressWarnings("rawtypes")
    public static class SomeUnmarshaller implements Unmarshaller {
        @Override
        public <A extends XmlAdapter> A getAdapter(Class<A> type) {
            return null;
        }

        @Override
        public AttachmentUnmarshaller getAttachmentUnmarshaller() {
            return null;
        }

        @Override
        public ValidationEventHandler getEventHandler() throws JAXBException {
            return null;
        }

        @Override
        public Listener getListener() {
            return null;
        }

        @Override
        public Object getProperty(String name) throws PropertyException {
            return null;
        }

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public UnmarshallerHandler getUnmarshallerHandler() {
            return null;
        }

        @Override
        public boolean isValidating() throws JAXBException {
            return false;
        }

        @Override
        public void setAdapter(XmlAdapter adapter) {
        }

        @Override
        public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        }

        @Override
        public void setAttachmentUnmarshaller(AttachmentUnmarshaller au) {
        }

        @Override
        public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        }

        @Override
        public void setListener(Listener listener) {
        }

        @Override
        public void setProperty(String name, Object value) throws PropertyException {
        }

        @Override
        public void setSchema(Schema schema) {
        }

        @Override
        public void setValidating(boolean validating) throws JAXBException {
        }

        @Override
        public Object unmarshal(File f) throws JAXBException {
            return null;
        }

        @Override
        public Object unmarshal(InputStream is) throws JAXBException {
            return null;
        }

        @Override
        public Object unmarshal(Reader reader) throws JAXBException {
            return null;
        }

        @Override
        public Object unmarshal(URL url) throws JAXBException {
            return null;
        }

        @Override
        public Object unmarshal(InputSource source) throws JAXBException {
            return null;
        }

        @Override
        public Object unmarshal(Node node) throws JAXBException {
            return node.toString();
        }

        @Override
        public Object unmarshal(Source source) throws JAXBException {
            return null;
        }

        @Override
        public Object unmarshal(XMLStreamReader reader) throws JAXBException {
            return getClass().getSimpleName();
        }

        @Override
        public Object unmarshal(XMLEventReader reader) throws JAXBException {
            return null;
        }

        @Override
        public <T> JAXBElement<T> unmarshal(Node node, Class<T> declaredType) throws JAXBException {
            return null;
        }

        @Override
        public <T> JAXBElement<T> unmarshal(Source source, Class<T> declaredType) throws JAXBException {
            return null;
        }

        @Override
        public <T> JAXBElement<T> unmarshal(XMLStreamReader reader, Class<T> declaredType) throws JAXBException {
            String name = getClass().getSimpleName();
            @SuppressWarnings("unchecked")
            JAXBElement<T> el = new JAXBElement<T>(new QName(name), declaredType, (T) name);
            return el;
        }

        @Override
        public <T> JAXBElement<T> unmarshal(XMLEventReader reader, Class<T> declaredType) throws JAXBException {
            return null;
        }
    }

      
    @SuppressWarnings("rawtypes")
    public static class SomeMarshaller implements Marshaller {
        @Override
        public <A extends XmlAdapter> A getAdapter(Class<A> arg0) {
            return null;
        }

        @Override
        public AttachmentMarshaller getAttachmentMarshaller() {
            return null;
        }

        @Override
        public ValidationEventHandler getEventHandler() throws JAXBException {
            return null;
        }

        @Override
        public Listener getListener() {
            return null;
        }

        @Override
        public Node getNode(Object arg0) throws JAXBException {
            return null;
        }

        @Override
        public Object getProperty(String arg0) throws PropertyException {
            return null;
        }

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public void marshal(Object arg0, Result arg1) throws JAXBException {
        }

        @Override
        public void marshal(Object arg0, OutputStream arg1) throws JAXBException {
            try {
                String value = ((JAXBElement) arg0).getValue().toString();
                arg1.write(value.getBytes());
                arg1.write(getClass().getSimpleName().getBytes());
                arg1.flush();
            } catch (IOException e) {
                throw new JAXBException(e);
            }
        }

        @Override
        public void marshal(Object arg0, File arg1) throws JAXBException {
        }

        @Override
        public void marshal(Object jaxbElement, Writer writer) throws JAXBException {
        }

        @Override
        public void marshal(Object jaxbElement, ContentHandler handler) throws JAXBException {
        }

        @Override
        public void marshal(Object jaxbElement, Node node) throws JAXBException {
        }

        @Override
        public void marshal(Object jaxbElement, XMLStreamWriter writer) throws JAXBException {
        }

        @Override
        public void marshal(Object jaxbElement, XMLEventWriter writer) throws JAXBException {
        }

        @Override
        public void setAdapter(XmlAdapter adapter) {
        }

        @Override
        public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        }

        @Override
        public void setAttachmentMarshaller(AttachmentMarshaller am) {
        }

        @Override
        public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        }

        @Override
        public void setListener(Listener listener) {
        }

        @Override
        public void setProperty(String name, Object value) throws PropertyException {
        }

        @Override
        public void setSchema(Schema schema) {
        }
    }
    
    @SuppressWarnings("deprecation")
    public static class SomeJaxbContext extends JAXBContext {
        @Override
        public Marshaller createMarshaller() throws JAXBException {
            return new SomeMarshaller();
        }

        @Override
        public Unmarshaller createUnmarshaller() throws JAXBException {
            return new SomeUnmarshaller();
        }

        @Override
        public Validator createValidator() throws JAXBException {
            return null;
        }
    }
    
    @Override
    public JAXBContext getContext(Class<?> type) {
        return  new SomeJaxbContext();
    }
}
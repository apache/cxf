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
package org.apache.cxf.jaxrs.provider.dom4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

@Produces({"application/xml", "application/*+xml", "text/xml", "application/json", "application/*+json" })
@Consumes({"application/xml", "application/*+xml", "text/xml", "application/json", "application/*+json" })
public class DOM4JProvider extends AbstractConfigurableProvider
    implements MessageBodyReader<org.dom4j.Document>, MessageBodyWriter<org.dom4j.Document> {
    public static final String SUPPRESS_XML_DECLARATION = "suppress.xml.declaration";
    private static final Class<org.w3c.dom.Document> DOM_DOC_CLS =
        org.w3c.dom.Document.class;

    private Providers providers;

    private boolean convertAlways;

    @Context
    public void setProviders(Providers providers) {
        this.providers = providers;
    }

    public boolean isReadable(Class<?> cls, Type type, Annotation[] anns,
                    MediaType mt) {
        return org.dom4j.Document.class.isAssignableFrom(cls);
    }

    public org.dom4j.Document readFrom(Class<org.dom4j.Document> cls, Type type,
                    Annotation[] anns, MediaType mt,
                    MultivaluedMap<String, String> headers, InputStream is)
        throws IOException, WebApplicationException {
        MessageBodyReader<org.w3c.dom.Document> reader =
            providers.getMessageBodyReader(DOM_DOC_CLS, DOM_DOC_CLS, anns, mt);
        if (reader == null) {
            throw ExceptionUtils.toNotSupportedException(null, null);
        }
        org.w3c.dom.Document domDoc =
            reader.readFrom(DOM_DOC_CLS, DOM_DOC_CLS, anns, mt, headers, is);
        return new org.dom4j.io.DOMReader().read(domDoc);
    }

    public long getSize(org.dom4j.Document doc, Class<?> cls, Type type,
        Annotation[] anns, MediaType mt) {
        return -1;
    }

    public boolean isWriteable(Class<?> cls, Type type,
                               Annotation[] anns, MediaType mt) {
        return org.dom4j.Document.class.isAssignableFrom(cls);
    }

    public void writeTo(org.dom4j.Document doc, Class<?> cls,
                        Type type, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {
        if (!convertAlways && mt.getSubtype().contains("xml")) {

            XMLWriter writer;
            if (MessageUtils.getContextualBoolean(getCurrentMessage(), SUPPRESS_XML_DECLARATION, false)) {
                OutputFormat format = new org.dom4j.io.OutputFormat();
                format.setSuppressDeclaration(true);
                writer = new org.dom4j.io.XMLWriter(os, format);
            } else {
                writer = new org.dom4j.io.XMLWriter(os);
            }
            writer.write(doc);
            writer.flush();
        } else {
            org.w3c.dom.Document domDoc = convertToDOM(doc);

            MessageBodyWriter<org.w3c.dom.Document> writer =
                providers.getMessageBodyWriter(DOM_DOC_CLS, DOM_DOC_CLS, anns, mt);
            if (writer == null) {
                throw ExceptionUtils.toNotAcceptableException(null, null);
            }
            writer.writeTo(domDoc, DOM_DOC_CLS, DOM_DOC_CLS, anns, mt, headers, os);
        }
    }

    protected Message getCurrentMessage() {
        return JAXRSUtils.getCurrentMessage();
    }

    private org.w3c.dom.Document convertToDOM(org.dom4j.Document doc) {
        String xml = doc.asXML();
        try {
            return StaxUtils.read(new StringReader(xml));
        } catch (Exception ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    public void convertToDOMAlways(boolean convert) {
        convertAlways = convert;
    }
}

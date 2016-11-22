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

package org.apache.cxf.jaxrs.provider.atom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.apache.abdera.writer.Writer;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;

public abstract class AbstractAtomProvider<T extends Element> 
    implements MessageBodyWriter<T>, MessageBodyReader<T> {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractAtomProvider.class);
    private static final Abdera ATOM_ENGINE = new Abdera();
    private boolean autodetectCharset;
    private boolean formattedOutput;
    
    public long getSize(T element, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    public void writeTo(T element, Class<?> clazz, Type type, Annotation[] a, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os) 
        throws IOException {
        if (MediaType.APPLICATION_JSON_TYPE.isCompatible(mt)) {
            Writer w = createWriter("json");
            if (w == null) {
                throw ExceptionUtils.toNotSupportedException(null, null);
            }
            element.writeTo(w, os);   
        } else if (formattedOutput) {
            Writer w = createWriter("prettyxml");
            if (w != null) {
                element.writeTo(w, os);
            } else {
                element.writeTo(os);
            }
        } else {
            element.writeTo(os);
        }
    }

    protected Writer createWriter(String writerName) {
        Writer w = ATOM_ENGINE.getWriterFactory().getWriter(writerName);
        if (w == null) {
            LOG.fine("Atom writer \"" + writerName + "\" is not available");
        }
        return w;
    }
    
    public T readFrom(Class<T> clazz, Type t, Annotation[] a, MediaType mt, 
                         MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        Parser parser = ATOM_ENGINE.getParser();
        synchronized (parser) {
            ParserOptions options = parser.getDefaultParserOptions();
            if (options != null) {
                options.setAutodetectCharset(autodetectCharset);
                options.setResolveEntities(false);
            }
        }
        Document<T> doc = parser.parse(is);
        return doc.getRoot();
    }

    public void setFormattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }

    public void setAutodetectCharset(boolean autodetectCharset) {
        this.autodetectCharset = autodetectCharset;
    }
}

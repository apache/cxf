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

package org.apache.cxf.databinding;

import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.validation.Schema;

import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;

/**
 * The 'read' side of the data binding abstraction of CXF. A DataReader&lt;T&gt; reads objects 
 * from a source of type T.
 * @param <T> The type of the source. Each data binding defines the set of source types that it supports.
 */
public interface DataReader<T> {
    String FAULT = DataReader.class.getName() + "Fault";
    String ENDPOINT = DataReader.class.getName() + "Endpoint";

    /**
     * Read an object from the input.
     * @param input input source object.
     * @return item read.
     */
    Object read(T input);
    /**
     * Read an object from the input, applying additional conventions based on the WSDL message
     * part.
     * @param part The message part for this item. If null, this API is equivalent to
     * {@link #read(Object)}.
     * @param input input source object.
     * @return item read.
     */
    Object read(MessagePartInfo part, T input);
    /**
     * Read an object from the input. In the current version of CXF, not all binding support
     * this API, and those that do ignore the element QName parameter.
     * @param elementQName expected element. Generally ignored.
     * @param input input source object.
     * @param type the type of object required/requested. This is generally used 
     * when the caller wants to receive a raw source object and avoid any binding processing.
     * For example, passing javax.xml.transform.Source. The bindings do not necessarily throw
     * if they cannot provide an object of the requested type, and will apply their normal
     * mapping processing, instead.
     * @return item read.
     */
    Object read(QName elementQName, T input, Class type);
    /**
     * Supply a schema to validate the input. Bindings silently ignore this parameter if they
     * do not support schema validation, or the particular form of validation implied by
     * a particular form of Schema.
     * @param s
     */
    void setSchema(Schema s);
    /**
     * Attach a collection of attachments to a binding. This permits a binding to process the contents
     * of one or more attachments as part of reading from this reader.
     * @param attachments attachments.
     */
    void setAttachments(Collection<Attachment> attachments);
    /**
     * Set an arbitrary property on the reader.
     * {@link #FAULT} and {@link #ENDPOINT} specify two common properties: the Fault object being read
     * and the {@link org.apache.cxf.endpoint.Endpoint}.
     * @param prop Name of the property.
     * @param value Value of the property.
     */
    void setProperty(String prop, Object value);
}

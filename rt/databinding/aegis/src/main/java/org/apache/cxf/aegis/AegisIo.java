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

package org.apache.cxf.aegis;

import javax.xml.validation.Schema;


/**
 * Aegis abstraction for reading.
 * 
 *  Note that this interface does not include the 'read' method. Since the caller of a reader has to know
 *  the type of the source object, the read method is not specified here in the interface, but is provided
 *  in the specific type. 
 */
public interface AegisIo {
    /**
     * Supply a schema to validate the input. Bindings silently ignore this parameter if they
     * do not support schema validation, or the particular form of validation implied by
     * a particular form of Schema.
     * @param s
     */
    void setSchema(Schema s);
    /**
     * Set an arbitrary property on the reader.
     * {@link #FAULT} and {@link #ENDPOINT} specify two common properties: the Fault object being read
     * and the {@link org.apache.cxf.endpoint.Endpoint}.
     * @param prop Name of the property.
     * @param value Value of the property.
     */
    void setProperty(String prop, Object value);
}

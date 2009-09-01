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

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.type.AegisType;

/**
 * Interface for Aegis writers.
 * @param <SinkT>
 */
public interface AegisWriter<SinkT> extends AegisIo {
    /**
     * Write an object to the sink.
     * @param obj The object.
     * @param elementName The element QName.
     * @param optional true to omit for null. (minOccurs=0)
     * @param output The output sink.
     * @param aegisType The aegis type to use. Null is allowed, but only if
     * obj is not null. 
     * @throws Exception
     */
    void write(Object obj, 
               QName elementName,
               boolean optional,
               SinkT output, 
               AegisType aegisType) throws Exception;
    
    /**
     * Write an object to the sink, providing a {@link java.lang.reflect.Type} to specify
     * its type.
     * @param obj the object
     * @param elementName XML element name
     * @param optional true if null maps to no output at all.
     * @param output where to put it.
     * @param objectType A description of the type of the object.
     * @throws Exception
     */
    void write(Object obj,
               QName elementName,
               boolean optional,
               SinkT output,
               java.lang.reflect.Type objectType) throws Exception;
}

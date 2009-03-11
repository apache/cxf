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

import javax.xml.validation.Schema;

import org.apache.cxf.message.Attachment;

/**
 * The 'write' side of the data binding abstraction of CXF.
 */
public interface BaseDataWriter {
    String ENDPOINT = DataWriter.class.getName() + "Endpoint";
    
    /**
     * Attach a schema to the writer. If the binding supports validation, it will
     * validate the XML that it produces (assuming that it produces XML). 
     * @param s the schema.
     */
    void setSchema(Schema s);
    /**
     * Attach a collection of attachments to this writer.
     * @param attachments
     */
    void setAttachments(Collection<Attachment> attachments);
    /**
     * Set a property for the writer.
     * @param key property key 
     * @param value property value.
     */
    void setProperty(String key, Object value);
}

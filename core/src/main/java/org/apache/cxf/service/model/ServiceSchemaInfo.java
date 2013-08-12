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

package org.apache.cxf.service.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.common.xmlschema.SchemaCollection;

/**
 * The ServiceInfo class has schema in two forms: the XmlSchema, in a SchemaCollection, and the 
 * DOM trees in the SchemaInfo objects. This class exists in order to allow the WSDL cache to store both.
 */
public class ServiceSchemaInfo {
    private SchemaCollection schemaCollection;
    private List<SchemaInfo> schemaInfoList;
    private Map<String, Element> schemaElementList;
    
    public SchemaCollection getSchemaCollection() {
        return schemaCollection;
    }
    public void setSchemaCollection(SchemaCollection schemaCollection) {
        this.schemaCollection = schemaCollection;
    }
    public List<SchemaInfo> getSchemaInfoList() {
        return schemaInfoList;
    }
    public void setSchemaInfoList(List<SchemaInfo> schemaInfoList) {
        this.schemaInfoList = new ArrayList<SchemaInfo>(schemaInfoList);
    }
    public Map<String, Element> getSchemaElementList() {
        return schemaElementList;
    }
    public void setSchemaElementList(Map<String, Element> l) {
        schemaElementList = l;
    }
}

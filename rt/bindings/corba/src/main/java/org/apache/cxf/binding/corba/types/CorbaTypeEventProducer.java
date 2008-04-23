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
package org.apache.cxf.binding.corba.types;

import java.util.List;

import javax.xml.namespace.QName;

import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

public interface CorbaTypeEventProducer {
    
    boolean hasNext();
    
    /* 
     * return the current event
     */ 
    int next();
    
    /*
     * qname of current content
     */
    QName getName();
    
    /*
     * local name of current content
     */
    String getLocalName();    
    
    /*
     * text of current content
     */
    String getText();

    /*
     * return any attributes for the current type
     */
    List<Attribute> getAttributes();

    /*
     * return any namespace for the current type
     */
    List<Namespace> getNamespaces();    

}

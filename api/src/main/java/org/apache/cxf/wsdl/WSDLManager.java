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

package org.apache.cxf.wsdl;

import java.net.URL;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import org.w3c.dom.Element;
import org.apache.cxf.service.model.ServiceSchemaInfo;


/**
 * WSDLManager
 *
 */
public interface WSDLManager {

    /**
     * Returns the ExtensionRegistry that the WSDLManager
     * uses when reading WSDL files.   Users can use
     * this to register their own extensors.
     * @return the ExtensionRegistry
     */
    ExtensionRegistry getExtensionRegistry();
    
    /**
     * Returns the WSDLFactory that is used to read/write WSDL definitions
     * @return the WSDLFactory
     */
    WSDLFactory getWSDLFactory();
    

    /**
     * Get the WSDL definition for the given URL.  Implementations
     * may return a copy from a local cache or load a new copy 
     * from the URL.
     * @param url - the location of the WSDL to load 
     * @return the wsdl definition
     */
    Definition getDefinition(URL url) throws WSDLException; 

    /**
     * Get the WSDL definition for the given URL.  Implementations
     * may return a copy from a local cache or load a new copy 
     * from the URL.
     * @param url - the location of the WSDL to load 
     * @return the wsdl definition
     */
    Definition getDefinition(String url) throws WSDLException;
    
    /**
     * Get the WSDL definition for the given Element.  Implementations
     * may return a copy from a local cache or load a new copy 
     * from the Element.
     * @param element - the root element of the wsdl 
     * @return the wsdl definition
     */
    Definition getDefinition(Element element) throws WSDLException;  
    

    /**
     * Adds a definition into the cache for lookup later
     * @param key
     * @param wsdl
     */
    void addDefinition(Object key, Definition wsdl);
    
    /**
     * 
     * @return all Definitions in the map
     */
    Map<Object, Definition> getDefinitions();
    
    /**
     * This object will cache the schemas for a WSDL.
     * @param wsdl
     * @return
     */
    ServiceSchemaInfo getSchemasForDefinition(Definition wsdl);
    
    /**
     * Register a collection of schemas for a WSDL.
     * @param wsdl
     * @param schemas
     */
    void putSchemasForDefinition(Definition wsdl, ServiceSchemaInfo schemas);
    
    
    /**
     * If the definition is cached, remove it from the cache
     * @param wsdl
     */
    void removeDefinition(Definition wsdl);
    
}

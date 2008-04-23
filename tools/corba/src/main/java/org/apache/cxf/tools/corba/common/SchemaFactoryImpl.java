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

package org.apache.cxf.tools.corba.common;

import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.xml.WSDLWriter;

import com.ibm.wsdl.DefinitionImpl;
import com.ibm.wsdl.extensions.PopulatedExtensionRegistry;


/**
 * This class is a copy of the WSDLFactoryImpl from the wsdl4j implementation
 * It overwrites the newWSDLWriter method to return a SchemaWriter 
 */
public class SchemaFactoryImpl extends SchemaFactory {
    /**
     * Create a new instance of a Definition, with an instance of a
     * PopulatedExtensionRegistry as its ExtensionRegistry.
     * 
     * @see com.ibm.wsdl.extensions.PopulatedExtensionRegistry
     */
    public Definition newDefinition() {
        Definition def = new DefinitionImpl();
        ExtensionRegistry extReg = newPopulatedExtensionRegistry();

        def.setExtensionRegistry(extReg);

        return def;
    }    

    /**
     * Create a new instance of a SchemaWriter.
     */
    public WSDLWriter newWSDLWriter() {
        return new SchemaWriterImpl();
    }

    /**
     * Create a new instance of an ExtensionRegistry with pre-registered
     * serializers/deserializers for the SOAP, HTTP and MIME extensions. Java
     * extensionTypes are also mapped for all the SOAP, HTTP and MIME
     * extensions.
     */
    public ExtensionRegistry newPopulatedExtensionRegistry() {
        return new PopulatedExtensionRegistry();
    }
}

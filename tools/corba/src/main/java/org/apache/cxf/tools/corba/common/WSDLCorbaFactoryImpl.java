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
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;


/**
 * This class is a copy of the WSDLFactoryImpl from the wsdl4j implementation
 * It overwrites the newWSDLWriter method to return a WSDLCorbaWriter 
 */
public class WSDLCorbaFactoryImpl extends WSDLCorbaFactory {
    WSDLFactory factory;
    
    public WSDLCorbaFactoryImpl() throws WSDLException {
        factory = WSDLFactory.newInstance();
    }
    
    /**
     * Create a new instance of a Definition, with an instance of a
     * PopulatedExtensionRegistry as its ExtensionRegistry.
     */
    public Definition newDefinition() {
        Definition def = factory.newDefinition();
        ExtensionRegistry extReg = newPopulatedExtensionRegistry();
        def.setExtensionRegistry(extReg);
        return def;
    }

    /**
     * Create a new instance of a WSDLReader.
     */
    public WSDLReader newWSDLReader() {
        return factory.newWSDLReader();
    }

    /**
     * Create a new instance of a WSDLWriter.
     */
    public WSDLWriter newWSDLWriter() {
        return new WSDLCorbaWriterImpl(factory.newWSDLWriter());
    }

    /**
     * Create a new instance of an ExtensionRegistry with pre-registered
     * serializers/deserializers for the SOAP, HTTP and MIME extensions. Java
     * extensionTypes are also mapped for all the SOAP, HTTP and MIME
     * extensions.
     */
    public ExtensionRegistry newPopulatedExtensionRegistry() {
        return factory.newPopulatedExtensionRegistry();
    }
}

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

package org.apache.cxf.ws.addressing;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.JAXBException;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.wsdl.JAXBExtensionHelper;
import org.apache.cxf.wsdl.WSDLExtensionLoader;
import org.apache.cxf.wsdl.WSDLManager;

/**
 * 
 */
@NoJSR250Annotations
public final class AddressingWSDLExtensionLoader implements WSDLExtensionLoader { 
    
    public AddressingWSDLExtensionLoader(Bus b) {
        WSDLManager manager = b.getExtension(WSDLManager.class);
        
        createExtensor(manager, javax.wsdl.Binding.class,
                       org.apache.cxf.ws.addressing.wsdl.UsingAddressing.class);        
    }
    public void createExtensor(WSDLManager manager,
                                Class<?> parentType,
                                Class<? extends ExtensibilityElement> elementType) {
        try {
            JAXBExtensionHelper.addExtensions(manager.getExtensionRegistry(),
                                              parentType, 
                                              elementType);
        } catch (JAXBException e) {
            //ignore, won't support XML
        }
    }

}

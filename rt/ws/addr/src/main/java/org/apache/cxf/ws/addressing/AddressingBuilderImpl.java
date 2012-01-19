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


/**
 * Factory for WS-Addressing elements.
 * <p>
 * Note that the JAXB generated types are used directly to represent 
 * WS-Addressing schema types. Hence there are no factory methods defined
 * on this class for those types, as they may be instanted in the normal
 * way via the JAXB generated ObjectFactory.
 */
public class AddressingBuilderImpl extends AddressingBuilder {

    public AddressingBuilderImpl() {
    }

    //--AddressingType implementation

    /**
     * @return WS-Addressing namespace URI
     */
    public String getNamespaceURI() {
        return Names.WSA_NAMESPACE_NAME;
    }

    //--AddresingBuilder implementation

    /**
     * AddressingProperties factory method.
     *  
     * @return a new AddressingProperties instance
     */
    public AddressingProperties newAddressingProperties() {
        return new AddressingPropertiesImpl();
    }
    
    /**
     * AddressingConstants factory method.
     * 
     * @return an AddressingConstants instance
     */
    public AddressingConstants newAddressingConstants() {
        return new AddressingConstantsImpl();
    }
}

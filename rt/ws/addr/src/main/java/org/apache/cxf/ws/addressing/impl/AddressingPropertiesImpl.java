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

package org.apache.cxf.ws.addressing.impl;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.Names;

/**
 * Abstraction of Message Addressing Properties. 
 */
public class AddressingPropertiesImpl extends AddressingProperties {

    /**
     * Constructor, defaults to 2005/08 namespace.
     */
    public AddressingPropertiesImpl() {
        this(Names.WSA_NAMESPACE_NAME);
    }

    /**
     * Constructor.
     * 
     * @param uri the namespace URI
     */
    public AddressingPropertiesImpl(String uri) {
        super(uri);
    }


}

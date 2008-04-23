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

package org.apache.cxf.ws.policy;

import org.w3c.dom.Element;

import org.apache.neethi.Policy;
import org.apache.neethi.PolicyReference;


/**
 * PolicyBuilder provides methods to create Policy and PolicyReferenceObjects
 * from DOM elements.
 */
public interface PolicyBuilder {
    /**
     * Creates a PolicyReference object from a DOM element.
     * 
     * @param element the element
     * @return the PolicyReference object constructed from the element
     */
    PolicyReference getPolicyReference(Element element);
    
    /**
     * Creates a Policy object from an DOM element.
     * 
     * @param element the element
     * @return the Policy object constructed from the element
     */
    Policy getPolicy(Element element);
}

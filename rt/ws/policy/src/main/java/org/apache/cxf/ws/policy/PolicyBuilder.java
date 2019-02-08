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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.apache.neethi.Policy;
import org.apache.neethi.PolicyReference;
import org.apache.neethi.PolicyRegistry;


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
    PolicyReference getPolicyReference(Object element);

    /**
     * Creates a Policy object from an DOM element.
     *
     * @param element the element
     * @return the Policy object constructed from the element
     */
    Policy getPolicy(Object element);


    /**
     * Creates a Policy object from an InputStream.
     *
     * @param stream the inputstream
     * @return the Policy object constructed from the element
     */
    Policy getPolicy(InputStream stream)
        throws IOException, ParserConfigurationException, SAXException;


    /**
     * Return the PolicyRegistry associated with the builder
     * @return the PolicyRegistry
     */
    PolicyRegistry getPolicyRegistry();

}

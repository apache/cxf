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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;


/**
 * AssertionBuilder is an interface used to build an Assertion object from a
 * given xml element. 
 * Domain Policy authors write custom AssertionBuilders to build Assertions for 
 * domain specific assertions. 
 * Note that assertions can include nested policy expressions. To build these,
 * it may be necessary to obtain other AssertionBuilders.
 * Concrete implementations should access the AssertionBuilderRegistry as a
 * Bus extension, so the registry need not passed as an argument here.
 */
public interface AssertionBuilder {

    /**
     * Constructs an assertion from an xml element.
     * 
     * @param element the element from which to build an assertion
     * @return an Assertion built from the given element
     */
    PolicyAssertion build(Element element);


    /**
     * Returns a collection of QNames describing the xml schema types for which this
     * builder can build assertions.
     * 
     * @return collection of QNames of known schema types
     */
    Collection<QName> getKnownElements();


    /**
     * Returns a new assertion that is compatible with the two specified
     * assertions or null if no compatible assertion can be built.
     */ 
    PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b);
}

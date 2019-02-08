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

package org.apache.cxf.ws.transfer.manager;

import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Representation;

/**
 * Interface for managing resource representations.
 */
public interface ResourceManager {

    /**
     * Returns Representation object given by reference parameter.
     * @param ref Reference parameter returned by create method.
     * @return Representation object containing the XML resource.
     * @see ResourceManager#create(org.apache.cxf.ws.transfer.Representation)
     */
    Representation get(ReferenceParametersType ref);

    /**
     * Deletes Representation object given by reference parameter.
     * @param ref Reference parameter returned by create method.
     * @see ResourceManager#create(org.apache.cxf.ws.transfer.Representation)
     */
    void delete(ReferenceParametersType ref);

    /**
     * Replaces Representation object given by reference parameter with newRepresentation.
     * @param ref Reference parameter returned by create method.
     * @param newRepresentation New Representation object, which will replace the old one.
     * @see ResourceManager#create(org.apache.cxf.ws.transfer.Representation)
     */
    void put(ReferenceParametersType ref, Representation newRepresentation);

    /**
     * Creates new Representation object from initRepresenation.
     * @param initRepresentation Representation object containing initial XML resource.
     * @return Reference parameter for newly created Representation object.
     */
    ReferenceParametersType create(Representation initRepresentation);
}

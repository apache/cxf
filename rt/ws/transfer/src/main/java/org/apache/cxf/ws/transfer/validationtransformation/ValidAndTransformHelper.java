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

package org.apache.cxf.ws.transfer.validationtransformation;

import java.util.List;

import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.shared.faults.InvalidRepresentation;

/**
 * Helper class for validation and transformation.
 */
public final class ValidAndTransformHelper {

    private ValidAndTransformHelper() {

    }

    /**
     * Validation and transformation process.
     * @param resourceTypeIdentifiers List of resourceTypeIdentifiers.
     * @param newRepresentation Incoming representation.
     * @param oldRepresentation Representation stored in the ResourceManager.
     */
    public static void validationAndTransformation(
            List<ResourceTypeIdentifier> resourceTypeIdentifiers,
            Representation newRepresentation,
            Representation oldRepresentation) {
        if (resourceTypeIdentifiers != null && !resourceTypeIdentifiers.isEmpty()) {
            for (ResourceTypeIdentifier resourceIdentifier : resourceTypeIdentifiers) {
                ResourceTypeIdentifierResult valResult = resourceIdentifier.identify(newRepresentation);
                if (valResult.isCorrect()) {
                    if (valResult.getTransformer() != null) {
                        ResourceTransformer transformer = valResult.getTransformer();
                        ResourceValidator validator = transformer.transform(newRepresentation, oldRepresentation);
                        if (validator != null && !validator.validate(newRepresentation, oldRepresentation)) {
                            throw new InvalidRepresentation();
                        }
                    }
                    return;
                }
            }
            throw new InvalidRepresentation();
        }
    }

}

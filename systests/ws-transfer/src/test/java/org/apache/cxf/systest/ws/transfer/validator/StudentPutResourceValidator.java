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

package org.apache.cxf.systest.ws.transfer.validator;

import org.w3c.dom.Element;

import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.shared.faults.PutDenied;
import org.apache.cxf.ws.transfer.validationtransformation.ResourceValidator;

public class StudentPutResourceValidator implements ResourceValidator {

    public static final String UID_NAMESPACE = "http://university.edu/student";

    public static final String UID_NAME = "uid";

    @Override
    public boolean validate(Representation newRepresentation, Representation oldRepresentation) {
        Element newRepresentationEl = (Element) newRepresentation.getAny();
        Element oldRepresentationEl = (Element) oldRepresentation.getAny();

        String newUid = newRepresentationEl.getElementsByTagNameNS(UID_NAMESPACE, UID_NAME).item(0).getTextContent();
        String oldUid = oldRepresentationEl.getElementsByTagNameNS(UID_NAMESPACE, UID_NAME).item(0).getTextContent();

        if (!newUid.equals(oldUid)) {
            throw new PutDenied();
        }
        return true;
    }

}

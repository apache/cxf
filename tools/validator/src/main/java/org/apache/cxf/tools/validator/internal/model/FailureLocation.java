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

package org.apache.cxf.tools.validator.internal.model;

import javax.xml.stream.Location;

public final class FailureLocation {

    private Location location;

    private String documentURI;

    public FailureLocation(Location loc, String docURI) {
        this.location = loc;
        this.documentURI = docURI;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(final Location newLocation) {
        this.location = newLocation;
    }

    public String getDocumentURI() {
        return documentURI;
    }

    public void setDocumentURI(final String newDocumentURI) {
        this.documentURI = newDocumentURI;
    }
}

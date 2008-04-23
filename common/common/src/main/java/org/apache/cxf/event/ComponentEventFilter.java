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
package org.apache.cxf.event;

public class ComponentEventFilter implements EventFilter {
    public static final String COMPONENT_CREATED_EVENT = "cxf.component.created.event";
    public static final String COMPONENT_REMOVED_EVENT = "cxf.component.removed.event";

    public boolean isEventEnabled(Event e) {        
        if (e.getID().getLocalPart().compareTo(COMPONENT_CREATED_EVENT) == 0) {
            return true;
        } else if (e.getID().getLocalPart().compareTo(COMPONENT_REMOVED_EVENT) == 0) {
            return true;
        }
        return false;
    }

}

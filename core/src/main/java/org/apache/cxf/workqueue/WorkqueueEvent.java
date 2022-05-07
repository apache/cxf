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

package org.apache.cxf.workqueue;

import java.util.EventObject;

public class WorkqueueEvent extends EventObject {
    private static final long serialVersionUID = 2445723027847701801L;

    private final Object newValue;
    private final Object oldValue;
    private final String propertyName;

    public WorkqueueEvent(
        Object source, String propertyName, Object oldValue, Object newValue) {
        super(source);
        this.propertyName = propertyName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("[propertyName=").append(getPropertyName());
        sb.append("; oldValue=").append(getOldValue());
        sb.append("; newValue=").append(getNewValue());
        sb.append("; source=").append(getSource());
        return sb.append(']').toString();
    }

}

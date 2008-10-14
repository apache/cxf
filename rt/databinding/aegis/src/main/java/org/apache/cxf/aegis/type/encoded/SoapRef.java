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
package org.apache.cxf.aegis.type.encoded;

/**
 * SoapRef represents an encoded SOAP 1.1 href or SOAP 1.2 ref object. This data class is updated when the ref
 * is resolved which can be immedately when the ref is resolved, or later when an instance with the referenced
 * id is unmarshalled.
 * <p/>
 * When the reference is resolved, an optional Action will be invoked which is commonly used to update a
 * property on the source object of the reference.
 */
public class SoapRef {
    private Object instance;
    private Action action;

    /**
     * Gets the referenced object instance or null if the reference has not been resolved yet;
     *
     * @return the referenced object instance or null
     */
    public Object get() {
        return instance;
    }

    /**
     * Sets the referenced object instance.  If an action is registered the onSet method is invoked.
     *
     * @param object the reference instance; not null
     */
    public void set(Object object) {
        if (object == null) {
            throw new NullPointerException("object is null");
        }
        this.instance = object;
        if (action != null) {
            action.onSet(this);
        }
    }

    /**
     * Registers an action to invoke when the instance is set.  If the instance, has already been set, the
     * onSet method will immedately be invoked.
     *
     * @return the action to invoke when this reference is resolved; not null
     */
    public void setAction(Action action) {
        if (action == null) {
            throw new NullPointerException("action is null");
        }
        this.action = action;
        if (instance != null) {
            action.onSet(this);
        }
    }

    public static interface Action {
        void onSet(SoapRef ref);
    }
}
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
package org.apache.cxf.xkms.client;

import org.apache.cxf.xkms.handlers.Applications;

public class X509AppId {
    private final Applications application;
    private final String id;

    public X509AppId(Applications application, String id) {
        this.id = id;
        this.application = application;
    }

    public Applications getApplication() {
        return application;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("application: %s; id: %s", application, id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((application == null)
                ? 0
                : application.hashCode());
        result = prime * result + ((id == null)
                ? 0
                : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof X509AppId)) {
            return false;
        }
        X509AppId other = (X509AppId) obj;
        if (application != other.application) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}

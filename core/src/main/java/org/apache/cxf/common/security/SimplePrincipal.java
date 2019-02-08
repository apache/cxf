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
package org.apache.cxf.common.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * Simple Principal implementation
 *
 */
public class SimplePrincipal implements Principal, Serializable {
    private static final long serialVersionUID = -5171549568204891853L;

    private String name;

    public SimplePrincipal(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Principal name can not be null");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SimplePrincipal)) {
            return false;
        }

        return name.equals(((SimplePrincipal)obj).name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return name;
    }
}

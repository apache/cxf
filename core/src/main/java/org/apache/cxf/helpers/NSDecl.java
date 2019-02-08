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

package org.apache.cxf.helpers;

public final class NSDecl {
    private final String prefix;
    private final String uri;
    private final int hashCode;

    public NSDecl(String pfx, String ur) {
        if (pfx == null) {
            this.prefix = "".intern();
        } else {
            this.prefix = pfx.intern();
        }
        this.uri = ur.intern();
        this.hashCode = (toString()).hashCode();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUri() {
        return uri;
    }

    public String toString() {
        return prefix + ":" + uri;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NSDecl)) {
            return false;
        }
        return uri == ((NSDecl)obj).uri
            && prefix == ((NSDecl)obj).prefix;
    }

}

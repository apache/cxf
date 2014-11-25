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
package org.apache.cxf.jaxrs.client.cache;

import java.io.Serializable;
import java.net.URI;

public class Key implements Serializable {
    private static final long serialVersionUID = 400974121100289840L;

    private int hash;

    private URI uri;
    private String accept;

    public Key(final URI uri, final String accept) {
        this.uri = uri;
        this.accept = accept;

        int result = uri.hashCode();
        result = 31 * result + (accept != null ? accept.hashCode() : 0);
        this.hash = result;
    }

    public Key() {
        // no-op
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public String getAccept() {
        return accept;
    }

    public void setAccept(final String accept) {
        this.accept = accept;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Key.class != o.getClass()) {
            return false;
        }

        final Key key = Key.class.cast(o);
        return !(accept != null ? !accept.equals(key.accept) : key.accept != null) && uri.equals(key.uri);

    }

    @Override
    public int hashCode() {
        return hash;
    }
}

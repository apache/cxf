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

public class BytesEntity implements Serializable {

    private static final long serialVersionUID = -6010007172900653981L;
    private byte[] entity;
    private boolean fromStream;
    public BytesEntity() {

    }
    public BytesEntity(byte[] entity, boolean fromStream) {
        this.entity = entity;
        this.setFromStream(fromStream);
    }
    public byte[] getEntity() {
        return entity;
    }
    public void setEntity(byte[] entity) {
        this.entity = entity;
    }
    public boolean isFromStream() {
        return fromStream;
    }
    public void setFromStream(boolean fromStream) {
        this.fromStream = fromStream;
    }
}

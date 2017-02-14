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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import java.io.Serializable;

public class Nonce implements Serializable {

    private static final long serialVersionUID = -6164115071533503490L;

    private String nonceString;
    private Long ts;

    public Nonce(String nonce, long ts) {
        this.nonceString = nonce;
        this.ts = ts;
    }

    public String getNonceString() {
        return nonceString;
    }

    public long getTs() {
        return ts;
    }

    public int hashCode() {
        return nonceString.hashCode() + 37 * ts.hashCode();
    }
    public boolean equals(Object o) {
        return o instanceof Nonce
            && this.nonceString.equals(((Nonce)o).nonceString)
            && this.ts.equals(((Nonce)o).ts);
    }
}
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
package org.apache.cxf.rs.security.oauth2.tokens.mac;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NonceHistory implements Serializable {

    private static final long serialVersionUID = -6404833046910698956L;

    private final long requestTimeDelta;
    private final List<Nonce> nonceList = new ArrayList<Nonce>();

    public NonceHistory(long requestTimeDelta, Nonce nonce) {
        this.requestTimeDelta = requestTimeDelta;
        nonceList.add(nonce);
    }

    public void addNonce(Nonce nonce) {
        nonceList.add(nonce);
    }

    public long getRequestTimeDelta() {
        return requestTimeDelta;
    }

    public List<Nonce> getNonceList() {
        return nonceList;
    }

    public Collection<Nonce> findMatchingNonces(String nonceString, long ts) {
        List<Nonce> nonceMatches = new ArrayList<Nonce>();
        for (Nonce nonce : getNonceList()) {
            if (nonce.getNonceString().equals(nonceString) && nonce.getTs() == ts) {
                nonceMatches.add(nonce);
            }
        }
        return nonceMatches;
    }

}
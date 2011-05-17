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
package org.apache.cxf.ws.security.policy;

import javax.xml.namespace.QName;

public final class SP13Constants {

    public static final String SP_NS = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802";

    public static final String SP_PREFIX = "sp13";
    
    public static final QName USERNAME_TOKEN_CREATED = new QName(SP_NS, "Created", SP_PREFIX);
    
    public static final QName USERNAME_TOKEN_NONCE = new QName(SP_NS, "Nonce", SP_PREFIX);
    
    private SP13Constants() {
        //
    }

}

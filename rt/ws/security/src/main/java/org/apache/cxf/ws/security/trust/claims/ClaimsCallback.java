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

package org.apache.cxf.ws.security.trust.claims;

import javax.security.auth.callback.Callback;

import org.apache.cxf.message.Message;

/**
 * This Callback class provides a pluggable way of sending Claims to the STS. A CallbackHandler
 * instance will be supplied with this class, which contains a reference to the current
 * Message. The CallbackHandler implementation is required to set the claims Object to be
 * sent in the request. This object can be either a DOM Element to be written out "as is", or else
 * a org.apache.cxf.rt.security.claims.ClaimCollection Object which will be serialized in the
 * request.
 */
public class ClaimsCallback implements Callback {

    private Object claims;

    private Message currentMessage;

    public ClaimsCallback() {
        //
    }

    public ClaimsCallback(Message currentMessage) {
        this.currentMessage = currentMessage;
    }

    public void setClaims(Object claims) {
        this.claims = claims;
    }

    public Object getClaims() {
        return claims;
    }

    public void setCurrentMessage(Message currentMessage) {
        this.currentMessage = currentMessage;
    }

    public Message getCurrentMessage() {
        return currentMessage;
    }

}

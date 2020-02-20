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

package org.apache.cxf.ws.security.trust;

import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;

/**
 *
 */
public class TrustException extends UncheckedException {

    private static final long serialVersionUID = -2957463932630164766L;
    /**
     * @param msg
     */
    public TrustException(Message msg) {
        super(msg);
    }

    /**
     * @param msg
     * @param t
     */
    public TrustException(Message msg, Throwable t) {
        super(msg, t);
    }
    /**
     * @param log
     * @param msg
     * @param params
     */
    public TrustException(Logger log, String msg, Object ... params) {
        super(log, msg, params);
    }
    /**
     * @param cause
     */
    public TrustException(Throwable cause) {
        super(cause);
    }

    public TrustException(String msg, Logger log) {
        super(new Message(msg, log));
    }
    public TrustException(String msg, Throwable t, Logger log) {
        super(new Message(msg, log), t);
    }

}

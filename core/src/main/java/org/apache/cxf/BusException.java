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

package org.apache.cxf;

import org.apache.cxf.common.i18n.Message;

/**
 * The BusException class is used to indicate a bus exception has occurred.
 */
public class BusException extends org.apache.cxf.common.i18n.Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>BusException</code> with the provided detail message.
     */
    public BusException(Message msg) {
        super(msg);
    }

    /**
     * Constructs a <code>BusException</code> with the detail message and cause
     * provided.
     */
    public BusException(Message msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Constructs a <code>BusException</code> with the provided cause.
     */
    public BusException(Throwable cause) {
        super(cause);
    }
}

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
package org.apache.cxf.tools.fortest.cxf669;

public class SayHi {

    /**
     * Describe msg here.
     */
    private String msg;

    /**
     * Get the <code>Msg</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getMsg() {
        return msg;
    }

    /**
     * Set the <code>Msg</code> value.
     *
     * @param newMsg The new Msg value.
     */
    public final void setMsg(final String newMsg) {
        this.msg = newMsg;
    }
}

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

package org.apache.cxf.tools.fortest.exception;

import jakarta.xml.bind.annotation.XmlTransient;

/**
 *
 */
public class TransientMessageException extends Exception {
    private static final long serialVersionUID = 1L;
    int idCode;

    public TransientMessageException() {
    }
    public TransientMessageException(int i) {
        idCode = i;
    }

    public TransientMessageException(int i, String message) {
        super(message);
        idCode = i;
    }

    public TransientMessageException(Throwable cause) {
        super(cause);
    }

    public TransientMessageException(String message, Throwable cause) {
        super(message, cause);
    }
    @XmlTransient
    public String getMessage() {
        return super.getMessage();
    }

    public int getIDCode() {
        return idCode;
    }
    public void setIDCode(int i) {
        idCode = i;
    }
}

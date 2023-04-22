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

package org.apache.handlers;

import jakarta.xml.ws.WebFault;

@WebFault(name = "FaultDetail", targetNamespace = "http://apache.org/handlers/types")
public class AddNumbersFault extends Exception {
    private static final long serialVersionUID = 6949117900244694759L;
    
    private org.apache.handlers.types.FaultDetail faultInfo;

    public AddNumbersFault() {
        super();
    }

    public AddNumbersFault(String message) {
        super(message);
    }

    public AddNumbersFault(String message, java.lang.Throwable cause) {
        super(message, cause);
    }

    public AddNumbersFault(String message, org.apache.handlers.types.FaultDetail faultDetail) {
        super(message);
        this.faultInfo = faultDetail;
    }

    public AddNumbersFault(String message, org.apache.handlers.types.FaultDetail faultDetail, java.lang.Throwable cause) {
        super(message, cause);
        this.faultInfo = faultDetail;
    }

    public org.apache.handlers.types.FaultDetail getFaultInfo() {
        return this.faultInfo;
    }
}

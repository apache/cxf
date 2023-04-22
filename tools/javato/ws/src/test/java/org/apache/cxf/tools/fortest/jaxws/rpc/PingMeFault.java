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

package org.apache.cxf.tools.fortest.jaxws.rpc;

import jakarta.xml.ws.WebFault;


@WebFault(name = "faultDetail",
          targetNamespace = "http://apache.org/hello_world_soap_http/types",
          faultBean = "org.apache.cxf.tools.fortest.jaxws.rpc.types.FaultDetail")

public class PingMeFault extends Exception {

    private static final long serialVersionUID = -4009029959664048871L;
    private int code;

    public int getCode() {
        return this.code;
    }
}

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
package org.apache.cxf.ws.security.wss4j;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.wss4j.dom.handler.WSHandlerResult;

/**
 * A pluggable way to create a CXF SecurityContext Object from a set of WSS4J processing results
 */
public interface WSS4JSecurityContextCreator {

    /**
     * Create a SecurityContext and store it on the SoapMessage parameter
     */
    void createSecurityContext(SoapMessage msg, WSHandlerResult handlerResult);

}

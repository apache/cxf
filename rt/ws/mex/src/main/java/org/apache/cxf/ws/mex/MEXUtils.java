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

package org.apache.cxf.ws.mex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

/**
 * 
 */
public final class MEXUtils {
    private MEXUtils() {
        //utility
    }

    public static Element getWSDL(Server server) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        String base = null;
        String ctxUri = null;
        Map<String, String> params = new HashMap<String, String>();
        params.put("wsdl", "");
        return new WSDLGetUtils().getDocument(message, base, 
                                              params, ctxUri, 
                                              server.getEndpoint().getEndpointInfo()).getDocumentElement();
    }

    public static Map<String, String> getSchemaLocations(Server server) {
        return null;
    }

    public static Map<String, String> getPolicyLocations(Server server) {
        return null;
    }

    public static List<Element> getSchemas(Server server, String id) {
        return null;
    }

    public static List<Element> getPolicies(Server server, String id) {
        return null;
    }
    
}

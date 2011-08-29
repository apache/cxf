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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.UrlUtilities;

/**
 * 
 */
public final class MEXUtils {
    private MEXUtils() {
        //utility
    }

    public static List<Element> getWSDLs(Server server) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        
        String base = (String)message.get(Message.REQUEST_URL);
        String ctxUri = (String)message.get(Message.PATH_INFO);

        WSDLGetUtils utils = new WSDLGetUtils();
        EndpointInfo info = server.getEndpoint().getEndpointInfo();
        List<Element> ret = new LinkedList<Element>();
        for (String id : utils.getWSDLIds(message, base, ctxUri, info)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("wsdl", id);
            ret.add(utils.getDocument(message, base, 
                                      params, ctxUri, 
                                      info).getDocumentElement());
            
        }
        return ret;
    }

    public static Map<String, String> getSchemaLocations(Server server) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        
        String base = (String)message.get(Message.REQUEST_URL);
        String ctxUri = (String)message.get(Message.PATH_INFO);

        WSDLGetUtils utils = new WSDLGetUtils();
        EndpointInfo info = server.getEndpoint().getEndpointInfo();
        return utils.getSchemaLocations(message, base, ctxUri, info);
    }

    public static List<Element> getSchemas(Server server, String id) {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        
        String base = (String)message.get(Message.REQUEST_URL);
        String ctxUri = (String)message.get(Message.PATH_INFO);

        WSDLGetUtils utils = new WSDLGetUtils();
        EndpointInfo info = server.getEndpoint().getEndpointInfo();
        Map<String, String> locs = utils.getSchemaLocations(message,
                                                      base,
                                                      ctxUri,
                                                      info);
        List<Element> ret = new LinkedList<Element>();
        for (Map.Entry<String, String> xsd : locs.entrySet()) {
            
            if (StringUtils.isEmpty(id) 
                || id.equals(xsd.getKey())) {
                String query = xsd.getValue().substring(xsd.getValue().indexOf('?') + 1);
                Map<String, String> params = UrlUtilities.parseQueryString(query);

                ret.add(utils.getDocument(message, base, params, ctxUri, info).getDocumentElement());
            }
        }
        return ret;
    }

    public static Map<String, String> getPolicyLocations(Server server) {
        return null;
    }


    public static List<Element> getPolicies(Server server, String id) {
        return null;
    }
    
}

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

package org.apache.cxf.systest.factory_pattern;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

@WebService(serviceName = "NumberService",
            endpointInterface = "org.apache.cxf.factory_pattern.Number", 
            targetNamespace = "http://cxf.apache.org/factory_pattern")
            
public class ManualNumberImpl extends NumberImpl {

    @Resource
    protected WebServiceContext aContext;
    
    protected WebServiceContext getWebSercviceContext() {
        return aContext;
    }
    
    /**
     * pull id from manual context appendage
     */
    protected String idFromMessageContext(MessageContext mc) {

        String id = null;
        String path = (String)mc.get(MessageContext.PATH_INFO);
        if (null != path) {
            id = path.substring(path.lastIndexOf('/') + 1);
        }
        return id;
    }
}

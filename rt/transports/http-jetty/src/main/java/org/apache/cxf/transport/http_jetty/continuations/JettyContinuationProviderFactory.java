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

package org.apache.cxf.transport.http_jetty.continuations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.ContinuationProviderFactory;
import org.eclipse.jetty.server.Request;

/**
 * 
 */
public class JettyContinuationProviderFactory implements ContinuationProviderFactory {

    public ContinuationProvider createContinuationProvider(Message inMessage, 
                                                           HttpServletRequest req,
                                                           HttpServletResponse resp) {
        if (req instanceof Request && ((Request)req).isAsyncSupported()) {
            return new JettyContinuationProvider(req, resp, inMessage);
        }
        return null;
    }

    public Message retrieveFromContinuation(HttpServletRequest req) {
        return (Message)req.getAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE);
    }

}

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

package org.apache.cxf.systest.ws.ut;

import java.util.List;

import org.w3c.dom.Element;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.processor.UsernameTokenProcessor;

/**
 * A custom Processor that overrides the default CallbackHandler to use a CustomUTPasswordCallback
 */
public class CustomUTProcessor implements Processor {

    @Override
    public List<WSSecurityEngineResult> handleToken(Element elem, RequestData request) throws WSSecurityException {
        request.setCallbackHandler(new CustomUTPasswordCallback());
        UsernameTokenProcessor processor = new UsernameTokenProcessor();
        return processor.handleToken(elem, request);
    }
}

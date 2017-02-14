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

package org.apache.cxf.javascript;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;

/**
 *
 */
public class JavascriptServerListener implements ServerLifeCycleListener {

    public JavascriptServerListener(Bus b) {
    }

    public void startServer(Server server) {
        if (server.getEndpoint().getBinding() instanceof SoapBinding) {
            //found a SOAP binding, add the javascript generation interceptor
            server.getEndpoint().getBinding().getInInterceptors().add(JavascriptGetInterceptor.INSTANCE);
        }
    }

    public void stopServer(Server server) {
    }

}

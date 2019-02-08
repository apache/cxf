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

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;

/**
 *
 */
public class MEXServerListener implements ServerLifeCycleListener {

    public void startServer(Server serv) {
        if (serv.getEndpoint().getBinding() instanceof SoapBinding) {
            QName qn = serv.getEndpoint().getService().getName();
            if (!"http://mex.ws.cxf.apache.org/".equals(qn.getNamespaceURI())) {
                serv.getEndpoint().getInInterceptors().add(new MEXInInterceptor(serv));
            }
        }
    }

    /** {@inheritDoc}*/
    public void stopServer(Server arg0) {

    }

}

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

package sample.ws.service;

import com.arjuna.mw.wst11.client.EnabledWSTXHandler;
import org.jboss.jbossts.txbridge.outbound.JaxWSTxOutboundBridgeHandler;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SecondClient {
    public static SecondServiceAT newInstance() throws Exception {
        URL wsdlLocation = new URL("http://localhost:8082/Service/SecondServiceAT?wsdl");
        QName serviceName = new QName("http://service.ws.sample", "SecondServiceATService");
        QName portName = new QName("http://service.ws.sample", "SecondServiceAT");

        Service service = Service.create(wsdlLocation, serviceName);
        SecondServiceAT client = service.getPort(portName, SecondServiceAT.class);


        List<Handler> handlerChain = new ArrayList<>();
        JaxWSTxOutboundBridgeHandler txOutboundBridgeHandler = new JaxWSTxOutboundBridgeHandler();
        EnabledWSTXHandler wstxHandler = new EnabledWSTXHandler();

        handlerChain.add(txOutboundBridgeHandler);
        handlerChain.add(wstxHandler);

        ((BindingProvider)client).getBinding().setHandlerChain(handlerChain);

        return client;
    }

}

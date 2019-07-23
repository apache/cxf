/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

/**
 * @author <a href="mailto:zfeng@redhat.com">Zheng Feng</a>
 */
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

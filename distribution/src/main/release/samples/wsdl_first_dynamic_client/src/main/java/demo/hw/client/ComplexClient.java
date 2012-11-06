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

package demo.hw.client;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * 
 */
public final class ComplexClient {
    
    private static final QName SERVICE_NAME 
        = new QName("http://Company.com/Application", 
                     "Company_ESB_Application_Biztalk_AgentDetails_4405_AgentDetails_Prt");
    
    private ComplexClient() {
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) { 
            System.out.println("please specify wsdl");
            System.exit(1); 
        }

        URL wsdlURL;
        File wsdlFile = new File(args[0]);
        if (wsdlFile.exists()) {
            wsdlURL = wsdlFile.toURI().toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }
        
        System.out.println(wsdlURL);
        
        JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance();
        Client client = factory.createClient(wsdlURL.toExternalForm(), SERVICE_NAME);
        ClientImpl clientImpl = (ClientImpl) client;
        Endpoint endpoint = clientImpl.getEndpoint();
        ServiceInfo serviceInfo = endpoint.getService().getServiceInfos().get(0);
        QName bindingName = new QName("http://Company.com/Application", 
            "Company_ESB_Application_Biztalk_AgentDetails_4405_AgentDetails_PrtSoap");
        BindingInfo binding = serviceInfo.getBinding(bindingName);
        //{
        QName opName = new QName("http://Company.com/Application", "GetAgentDetails");
        BindingOperationInfo boi = binding.getOperation(opName);
        BindingMessageInfo inputMessageInfo = boi.getInput();
        List<MessagePartInfo> parts = inputMessageInfo.getMessageParts();
        // only one part.
        MessagePartInfo partInfo = parts.get(0);
        Class<?> partClass = partInfo.getTypeClass();
        System.out.println(partClass.getCanonicalName()); // GetAgentDetails
        Object inputObject = partClass.newInstance();
        // Unfortunately, the slot inside of the part object is also called 'part'.
        // this is the descriptor for get/set part inside the GetAgentDetails class.
        PropertyDescriptor partPropertyDescriptor = new PropertyDescriptor("part", partClass);
        // This is the type of the class which really contains all the parameter information.
        Class<?> partPropType = partPropertyDescriptor.getPropertyType(); // AgentWSRequest
        System.out.println(partPropType.getCanonicalName());
        Object inputPartObject = partPropType.newInstance();
        partPropertyDescriptor.getWriteMethod().invoke(inputObject, inputPartObject);
        PropertyDescriptor numberPropertyDescriptor = new PropertyDescriptor("agentNumber", partPropType);
        numberPropertyDescriptor.getWriteMethod().invoke(inputPartObject, new Integer(314159));

        Object[] result = client.invoke(opName, inputObject);
        Class<?> resultClass = result[0].getClass();
        System.out.println(resultClass.getCanonicalName()); // GetAgentDetailsResponse
        PropertyDescriptor resultDescriptor = new PropertyDescriptor("agentWSResponse", resultClass);
        Object wsResponse = resultDescriptor.getReadMethod().invoke(result[0]);
        Class<?> wsResponseClass = wsResponse.getClass();
        System.out.println(wsResponseClass.getCanonicalName());
        PropertyDescriptor agentNameDescriptor = new PropertyDescriptor("agentName", wsResponseClass);
        String agentName = (String)agentNameDescriptor.getReadMethod().invoke(wsResponse);
        System.out.println("Agent name: " + agentName);
            
    }

}

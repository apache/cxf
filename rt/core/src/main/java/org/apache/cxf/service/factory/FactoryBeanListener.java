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

package org.apache.cxf.service.factory;

/**
 * 
 */
public interface FactoryBeanListener {
    enum Event {
        /**
         * Event fired at the very start of processing.  No parameters.  Useful
         * for setting up any state the listener may need to maintain.
         */
        START_CREATE,
        
        /**
         * Event fired at the very end of processing.   One parameter is passed
         * in which is the Service object that was created.
         */
        END_CREATE,

        /**
         * Called at the start of processing when it detects that the service
         * is to be created based on a wsdl contract.   One String parameter
         * of the URL of the wsdl.
         */
        CREATE_FROM_WSDL,
        
        /**
         * Called at the start of processing when it detects that the service
         * is to be created based on a Java class.  One Class<?> parameter
         * of the class that is being analyzed.
         */
        CREATE_FROM_CLASS,
        
        /**
         * Called after the wsdl is loaded/parsed.   Single parameter of the
         * WSS4J Definition of the WSDL.
         */
        WSDL_LOADED,
        
        /**
         * Called after the Service is set into the Factory after which the getService()
         * call will return a valid value.  One parameter of the Service object.
         */
        SERVICE_SET, 
        
        
        /**
         * OperationInfo, Method
         */
        INTERFACE_OPERATION_BOUND,
        
        /**
         * OperationInfo, Method, MessageInfo
         */
        OPERATIONINFO_IN_MESSAGE_SET,
        OPERATIONINFO_OUT_MESSAGE_SET,
        
        /**
         * OperationInfo, Class<? extends Throwable>, FaultInfo
         */
        OPERATIONINFO_FAULT,
        
        /**
         * InterfaceInfo, Class<?>
         */
        INTERFACE_CREATED,
        
        /**
         * DataBinding
         */
        DATABINDING_INITIALIZED,
        
        /**
         * EndpointInfo, Endpoint, Class
         */
        ENDPOINT_CREATED,
        
        /**
         * Server, targetObject, Class
         */
        SERVER_CREATED, 
        
        /**
         * BindingInfo, BindingOperationInfo
         */
        BINDING_OPERATION_CREATED,
        
        /**
         * BindingInfo
         */
        BINDING_CREATED, 
        
        /**
         * Endpoint, Client
         */
        CLIENT_CREATED, 
        
        /**
         * EndpointInfo, Endpoint, Class
         */
        ENDPOINT_SELECTED,
        
        /**
         * EndpointInfo
         */
        ENDPOINTINFO_CREATED, 
        
        /**
         * Class[], InvokationHandler, Proxy
         */
        PROXY_CREATED,
    };
    

    void handleEvent(Event ev, AbstractServiceFactoryBean factory, Object ... args);
}

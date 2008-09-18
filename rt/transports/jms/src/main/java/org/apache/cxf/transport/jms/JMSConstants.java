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

package org.apache.cxf.transport.jms;

public final class JMSConstants {
    
    public static final String JMS_CONTENT_TYPE = "SOAPJMS_contentType";
    public static final String JMS_QUEUE = "queue";
    public static final String JMS_TOPIC = "topic";

    public static final String TEXT_MESSAGE_TYPE = "text";
    public static final String BINARY_MESSAGE_TYPE = "binary";
    public static final String BYTE_MESSAGE_TYPE = "byte";

    public static final String JMS_POOLEDSESSION = "jms.pooled.session";   
    public static final String JMS_SERVER_REQUEST_HEADERS = "org.apache.cxf.jms.server.request.headers";
    public static final String JMS_SERVER_RESPONSE_HEADERS = "org.apache.cxf.jms.server.response.headers";
    public static final String JMS_REQUEST_MESSAGE = "org.apache.cxf.jms.request.message";
    public static final String JMS_RESPONSE_MESSAGE = "org.apache.cxf.jms.reponse.message";
    public static final String JMS_CLIENT_REQUEST_HEADERS = "org.apache.cxf.jms.client.request.headers";
    public static final String JMS_CLIENT_RESPONSE_HEADERS = 
        "org.apache.cxf.jms.client.response.headers";
    
    public static final String JMS_CLIENT_RECEIVE_TIMEOUT = "org.apache.cxf.jms.client.timeout";
    
    public static final String JMS_SERVER_CONFIGURATION_URI = 
        "http://cxf.apache.org/configuration/transport/jms-server";
    public static final String JMS_CLIENT_CONFIGURATION_URI = 
        "http://cxf.apache.org/configuration/transport/jms-client";
    public static final String ENDPOINT_CONFIGURATION_URI = 
        "http://cxf.apache.org/jaxws/endpoint-config";
    public static final String SERVICE_CONFIGURATION_URI = 
        "http://cxf.apache.org/jaxws/service-config";
    public static final String PORT_CONFIGURATION_URI = 
        "http://cxf.apache.org/jaxws/port-config";
    
    public static final String JMS_CLIENT_CONFIG_ID = "jms-client";
    public static final String JMS_SERVER_CONFIG_ID = "jms-server";
    
    public static final String JMS_REBASED_REPLY_TO = "org.apache.cxf.jms.server.replyto";
    
    
    private JMSConstants() {
        //utility class
    }
    
}

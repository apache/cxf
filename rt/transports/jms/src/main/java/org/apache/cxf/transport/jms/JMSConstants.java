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

    @Deprecated
    public static final String JMS_CONTENT_TYPE = "SOAPJMS_contentType";
    
    @Deprecated
    public static final String JMS_QUEUE = "queue";
    @Deprecated
    public static final String JMS_TOPIC = "topic";

    public static final String TEXT_MESSAGE_TYPE = "text";
    public static final String BINARY_MESSAGE_TYPE = "binary";
    public static final String BYTE_MESSAGE_TYPE = "byte";

    @Deprecated
    public static final String JMS_POOLEDSESSION = "jms.pooled.session";
    public static final String JMS_SERVER_REQUEST_HEADERS = "org.apache.cxf.jms.server.request.headers";
    public static final String JMS_SERVER_RESPONSE_HEADERS = "org.apache.cxf.jms.server.response.headers";
    public static final String JMS_REQUEST_MESSAGE = "org.apache.cxf.jms.request.message";
    @Deprecated
    public static final String JMS_RESPONSE_MESSAGE = "org.apache.cxf.jms.reponse.message";
    public static final String JMS_CLIENT_REQUEST_HEADERS = "org.apache.cxf.jms.client.request.headers";
    public static final String JMS_CLIENT_RESPONSE_HEADERS =
        "org.apache.cxf.jms.client.response.headers";

    @Deprecated
    public static final String JMS_CLIENT_RECEIVE_TIMEOUT = "org.apache.cxf.jms.client.timeout";

    @Deprecated
    public static final String JMS_SERVER_CONFIGURATION_URI =
        "http://cxf.apache.org/configuration/transport/jms-server";
    @Deprecated
    public static final String JMS_CLIENT_CONFIGURATION_URI =
        "http://cxf.apache.org/configuration/transport/jms-client";
    @Deprecated
    public static final String ENDPOINT_CONFIGURATION_URI =
        "http://cxf.apache.org/jaxws/endpoint-config";
    @Deprecated
    public static final String SERVICE_CONFIGURATION_URI =
        "http://cxf.apache.org/jaxws/service-config";
    @Deprecated
    public static final String PORT_CONFIGURATION_URI =
        "http://cxf.apache.org/jaxws/port-config";

    @Deprecated
    public static final String JMS_CLIENT_CONFIG_ID = "jms-client";
    @Deprecated
    public static final String JMS_SERVER_CONFIG_ID = "jms-server";

    // Is used by WS-Addressing
    public static final String JMS_REBASED_REPLY_TO = "org.apache.cxf.jms.server.replyto";
    public static final String JMS_SET_REPLY_TO = "org.apache.cxf.jms.client.set.replyto";
    public static final String JMS_MESSAGE_TYPE = "JMSMessageType";
    public static final String TARGET_SERVICE_IN_REQUESTURI = "target.service.inrequesturi";
    public static final String MALFORMED_REQUESTURI = "malformed.requesturi";
    
    public static final String RS_CONTENT_TYPE = "org.apache.cxf.jms.rs.ContentType";
    
    public static final String RS_CONTENT_LENGTH = "org.apache.cxf.jms.rs.ContentLength";

    private JMSConstants() {
        //utility class
    }

}

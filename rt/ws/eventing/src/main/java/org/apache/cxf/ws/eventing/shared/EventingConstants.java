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

package org.apache.cxf.ws.eventing.shared;

/**
 * This class contains String constants needed for WS-Eventing.
 */
public final class EventingConstants {

    public static final String ACTION_SUBSCRIBE = "http://www.w3.org/2011/03/ws-evt/Subscribe";
    public static final String ACTION_SUBSCRIBE_RESPONSE
        = "http://www.w3.org/2011/03/ws-evt/SubscribeResponse";

    public static final String ACTION_RENEW = "http://www.w3.org/2011/03/ws-evt/Renew";
    public static final String ACTION_RENEW_RESPONSE = "http://www.w3.org/2011/03/ws-evt/RenewResponse";

    public static final String ACTION_GET_STATUS = "http://www.w3.org/2011/03/ws-evt/GetStatus";
    public static final String ACTION_GET_STATUS_RESPONSE
        = "http://www.w3.org/2011/03/ws-evt/GetStatusResponse";

    public static final String ACTION_UNSUBSCRIBE = "http://www.w3.org/2011/03/ws-evt/Unsubscribe";
    public static final String ACTION_UNSUBSCRIBE_RESPONSE
        = "http://www.w3.org/2011/03/ws-evt/UnsubscribeResponse";

    public static final String ACTION_FAULT = "http://www.w3.org/2011/03/ws-evt/fault";

    public static final String EVENTING_2011_03_NAMESPACE = "http://www.w3.org/2011/03/ws-evt";

    public static final String RESPONSE_RENEW = "RenewResponse";

    public static final String WRAPPED_SINK_PORT_TYPE = "WrappedSinkPortType";

    public static final String RESPONSE_SUBSCRIBE = "SubscribeResponse";
    public static final String OPERATION_SUBSCRIBE = "Subscribe";
    public static final String OPERATION_RENEW = "Renew";
    public static final String RESPONSE_GET_STATUS = "GetStatusResponse";
    public static final String OPERATION_GET_STATUS = "GetStatus";
    public static final String RESPONSE_UNSUBSCRIBE = "UnsubscribeResponse";
    public static final String OPERATION_UNSUBSCRIBE = "Unsubscribe";
    public static final String NOTIFY = "Notify";
    public static final String PARAMETER = "parameter";
    public static final String OPERATION_NOTIFY_EVENT = "NotifyEvent";
    public static final String ACTION_SUBSCRIPTION_END = "http://www.w3.org/2011/03/ws-evt/SubscriptionEnd";
    public static final String ACTION_NOTIFY_EVENT_WRAPPED_DELIVERY
        = "http://www.w3.org/2011/03/ws-evt/WrappedSinkPortType/NotifyEvent";

    public static final String DELIVERY_FORMAT_WRAPPED
        = "http://www.w3.org/2011/03/ws-evt/DeliveryFormats/Wrap";
    public static final String DELIVERY_FORMAT_UNWRAPPED
        = "http://www.w3.org/2011/03/ws-evt/DeliveryFormats/Unwrap";

    public static final String SUBSCRIPTION_ID_DEFAULT_NAMESPACE = "http://cxf.apache.org/ws-eventing";
    public static final String SUBSCRIPTION_ID_DEFAULT_ELEMENT_NAME = "SubscriptionID";

    public static final String SUBSCRIPTION_END_DELIVERY_FAILURE
        = "http://www.w3.org/2011/03/ws-evt/DeliveryFailure";
    public static final String SUBSCRIPTION_END_SHUTTING_DOWN
        = "http://www.w3.org/2011/03/ws-evt/SourceShuttingDown";
    public static final String SUBSCRIPTION_END_SOURCE_CANCELLING
        = "http://www.w3.org/2011/03/ws-evt/SourceCancelling";


    private EventingConstants() {

    }
}

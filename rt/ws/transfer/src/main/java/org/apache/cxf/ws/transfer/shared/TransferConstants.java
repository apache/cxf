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

package org.apache.cxf.ws.transfer.shared;

/**
 * Helper class for holding of constants.
 */
public final class TransferConstants {

    public static final String TRANSFER_2011_03_NAMESPACE = "http://www.w3.org/2011/03/ws-tra";

    public static final String NAME_RESOURCE = "Resource";
    public static final String NAME_RESOURCE_FACTORY = "ResourceFactory";

    public static final String NAME_OPERATION_GET = "Get";
    public static final String NAME_OPERATION_DELETE = "Delete";
    public static final String NAME_OPERATION_PUT = "Put";
    public static final String NAME_OPERATION_CREATE = "Create";

    public static final String NAME_MESSAGE_GET = "Get";
    public static final String NAME_MESSAGE_GET_RESPONSE = "GetResponse";
    public static final String NAME_MESSAGE_DELETE = "Delete";
    public static final String NAME_MESSAGE_DELETE_RESPONSE = "DeleteResponse";
    public static final String NAME_MESSAGE_PUT = "Put";
    public static final String NAME_MESSAGE_PUT_RESPONSE = "PutResponse";
    public static final String NAME_MESSAGE_CREATE = "Create";
    public static final String NAME_MESSAGE_CREATE_RESPONSE = "CreateResponse";

    public static final String ACTION_GET = "http://www.w3.org/2011/03/ws-tra/Get";
    public static final String ACTION_GET_RESPONSE = "http://www.w3.org/2011/03/ws-tra/GetResponse";
    public static final String ACTION_DELETE = "http://www.w3.org/2011/03/ws-tra/Delete";
    public static final String ACTION_DELETE_RESPONSE = "http://www.w3.org/2011/03/ws-tra/DeleteResponse";
    public static final String ACTION_PUT = "http://www.w3.org/2011/03/ws-tra/Put";
    public static final String ACTION_PUT_RESPONSE = "http://www.w3.org/2011/03/ws-tra/PutResponse";
    public static final String ACTION_CREATE = "http://www.w3.org/2011/03/ws-tra/Create";
    public static final String ACTION_CREATE_RESPONSE = "http://www.w3.org/2011/03/ws-tra/CreateResponse";

    public static final String RESOURCE_REMOTE_SUFFIX = "_factory";

    private TransferConstants() {

    }
}

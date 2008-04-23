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
package org.apache.cxf.connector;

import javax.resource.ResourceException;

/**
 * Interface implemented by the Web service client proxy returned by
 * {@link CXFConnectionFactory}. It allows the caller to return the proxy to
 * the application server's pool when is no longer needed.
 */

public interface Connection {

    /**
     * close the connection handle. A caller should not use a closed connection.
     * 
     * @throws ResourceException if an error occurs during close.
     */
    void close() throws ResourceException;

}

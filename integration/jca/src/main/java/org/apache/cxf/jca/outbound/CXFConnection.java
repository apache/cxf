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
package org.apache.cxf.jca.outbound;

/**
 * A CXFConnection is obtains from {@link CXFConnectionFactory}.  It provides
 * access to a CXF web service for client to invoke.  The client should close
 * the CxfConnection when the web service is no longer needed.
 */

public interface CXFConnection {
    
    /**
     * Retrieves a service object to invoke.  The serviceInterface class must
     * match the serviceClass in the CXFConnectionSpec that is used to
     * obtain this CXFConnection.  Application can continue to use the service
     * object after the the connection has been closed by calling {@link #close()}.
     * 
     * @param <T>
     * @param serviceClass
     * @return service object
     * @throws Exception
     */
    <T> T getService(Class<T> serviceClass) throws Exception;
    
    /**
     * Closes the connection handle. A caller should not use a closed connection.
     * 
     * @throws Exception if an error occurs during close.
     */
    void close() throws Exception;


}

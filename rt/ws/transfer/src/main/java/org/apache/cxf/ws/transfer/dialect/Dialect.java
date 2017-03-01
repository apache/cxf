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

package org.apache.cxf.ws.transfer.dialect;

import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.Representation;

/**
 * The interface for a Dialect objects.
 */
public interface Dialect {

    /**
     * Method for processing incoming Get message by Dialect extension.
     * @param body Get body
     * @param representation XML representation stored in the ResourceManager
     * @return Representation, which will be returned in response.
     */
    Object processGet(Get body, Representation representation);

    /**
     * Method for processing incoming Put message by Dialect extension.
     * @param body Put body
     * @param representation XML representation stored in the ResourceManager
     * @return Representation, which will be stored in ResourceManager.
     */
    Representation processPut(Put body, Representation representation);

    /**
     * Method for processing incoming Delete message by Dialect extension.
     * @param body Delete body
     * @param representation XML representation stored in the ResourceManager
     * @return Representation, which will be stored in ResourceManager.
     */
    boolean processDelete(Delete body, Representation representation);

    /**
     * Method for processing incoming Create message by Dialect extension.
     * @param body Create body
     * @return Representation, which will be stored in ResourceManager.
     */
    Representation processCreate(Create body);

}

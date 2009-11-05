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
package org.apache.cxf.jaxrs.ext;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

/**
 * Can be used to affect the way the JAXRS selection algorithm chooses
 * between multiple matching resource classes and methods 
 *
 */
public interface ResourceComparator {

    /**
     * Compares two resource classes
     * @param cri1 First resource class
     * @param cri2 Second resource class
     * @param message incoming message
     * @return -1 if cri1 < cri2, 1 if if cri1 > cri2, 0 otherwise 
     */
    int compare(ClassResourceInfo cri1, 
                ClassResourceInfo cri2,
                Message message);
    
    /**
     * Compares two resource methods
     * @param oper1 First resource method
     * @param oper2 Second resource method
     * @param message incoming message
     * @return -1 if oper1 < oper2, 1 if if oper1 > oper2, 0 otherwise 
     */
    int compare(OperationResourceInfo oper1, 
                OperationResourceInfo oper2,
                Message message);
}

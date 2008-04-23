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
package org.apache.cxf.aegis.type.array;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(name = "DuplicateArrayService")
public class DuplicateArrayServiceBean implements DuplicateArrayService {

    @WebMethod
    public DuplicateArrayReturnItem[] lookup(String indexid) {
        return null;
    }

    @WebMethod
    public List<List<DuplicateArrayReturnItem>> lookupBatch(String indexid) {
        return null;
    }
    
    @WebMethod
    public Foo<String> doFoo(Foo<Integer> foo) {
        return null;
    }


}

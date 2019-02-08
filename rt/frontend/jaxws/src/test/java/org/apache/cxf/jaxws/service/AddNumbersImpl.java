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

package org.apache.cxf.jaxws.service;

import java.util.ArrayList;
import java.util.List;

@javax.jws.WebService(endpointInterface = "org.apache.cxf.jaxws.service.AddNumbers")
public class AddNumbersImpl implements AddNumbers {

    public List<Integer> addNumbers(List<String> arg) {
        List<Integer> res = new ArrayList<>();
        res.add(100);
        res.add(200);
        return res;

    }

}

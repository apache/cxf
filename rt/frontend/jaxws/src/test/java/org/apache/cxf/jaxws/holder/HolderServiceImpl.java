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
package org.apache.cxf.jaxws.holder;

import javax.jws.WebService;
import javax.xml.ws.Holder;


@WebService(endpointInterface = "org.apache.cxf.jaxws.holder.HolderService")
public class HolderServiceImpl implements HolderService {

    public String echo(String s1, String s2, Holder<String> outS2) {
        outS2.value = s2;
        return s1;
    }

    public String echo2(String s1, Holder<String> outS2, String s2) {
        outS2.value = s2;
        return s1;
    }

    public String echo3(Holder<String> header, String s1) {
        return s1;
    }
}

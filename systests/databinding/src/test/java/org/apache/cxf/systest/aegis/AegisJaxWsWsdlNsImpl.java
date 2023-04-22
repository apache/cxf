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
package org.apache.cxf.systest.aegis;

import java.util.List;

import jakarta.jws.WebService;

@WebService(endpointInterface = "org.apache.cxf.systest.aegis.AegisJaxWsWsdlNs",
        targetNamespace = "http://v1_1_2.rtf2pdf.doc.ws.daisy.marbes.cz")
public class AegisJaxWsWsdlNsImpl implements AegisJaxWsWsdlNs {


    public void updateVO(VO vo) {
        //System.out.println(vo.getStr());
    }


    public Integer updateInteger(Integer idInteger) {
        return idInteger;
    }


    public void updateIntegerList(List<Integer> idIntegerList) {
        //
    }

}

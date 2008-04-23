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


import java.util.Arrays;
import java.util.List;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(endpointInterface = "org.apache.cxf.jaxws.service.ArrayService",
            serviceName = "ArrayService",
            portName = "ArrayPort",
            targetNamespace = "http://service.jaxws.cxf.apache.org/")
public class ArrayServiceImpl implements ArrayService {

    public String[] arrayOutput() {
        return new String[] {"string1", "string2", "string3"};
    }

    public List<String> listOutput() {
        return Arrays.asList("string1", "string2", "string3");
    }

    public String arrayInput(@WebParam(name = "input")String[] inputs) {
        StringBuffer buf = new StringBuffer();
        for (String s : inputs) {
            buf.append(s);
        }
        return buf.toString();
    }

    public String listInput(@WebParam(name = "input")List<String> inputs) {
        StringBuffer buf = new StringBuffer();
        for (String s : inputs) {
            buf.append(s);
        }
        return buf.toString();
    }

    public String[] arrayInputAndOutput(@WebParam(name = "input")String[] inputs) {
        String[] results = new String[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            results[i] = inputs[i] + String.valueOf(i + 1);
        }
        return results;
    }
}

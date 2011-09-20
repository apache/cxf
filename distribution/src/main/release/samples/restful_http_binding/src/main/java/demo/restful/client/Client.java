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
package demo.restful.client;

import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;

public final class Client {
    private Client() { }

    public static void main(String[] args) throws Exception {
        // Sent HTTP GET request to query customer info
        System.out.println("Sent HTTP GET request to query customer info");
        URL url = new URL("http://localhost:8080/xml/customers");
        InputStream in = url.openStream();
        System.out.println(getStringFromInputStream(in));
    }

    private static String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}

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

package org.apache.cxf.sample.test;

import org.junit.BeforeClass;
import org.junit.Test;

import demo.hw.client.Client;
import demo.hw.server.Server;


/**
 * 
 */
public class TestAegisSample {
    @BeforeClass
    public static void startServer() throws Exception {
        Server.startServer();
    }
    
    @Test
    public void testClient() throws Exception {
        Client.main(null);
    }

}

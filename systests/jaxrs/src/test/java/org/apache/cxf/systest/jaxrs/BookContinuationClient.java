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
package org.apache.cxf.systest.jaxrs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;

public class BookContinuationClient extends AbstractTestServerBase {
    public static final String PORT = TestUtil.getPortNumber(BookContinuationServlet3Server.class);

    protected void run() {

    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        final String url = "http://localhost:" + PORT + "/async/bookstore/disconnect";
        WebClient wc = WebClient.create(url);
        try {
            System.out.println("server ready");
            wc.async().get().get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("server stopped");
            System.out.println("done!");
            System.exit(0);
        }
    }
}

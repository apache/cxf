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

package org.apache.cxf.testutil.common;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;

public abstract class AbstractServerTestServerBase extends AbstractTestServerBase {

    private Bus b;
    private Server server;

    @Override
    protected final void run() throws Exception {
        b = BusFactory.getDefaultBus();
        server = createServer(b);
        BusFactory.setDefaultBus(null);
        BusFactory.setThreadDefaultBus(null);
    }

    protected abstract Server createServer(Bus bus) throws Exception;

    @Override
    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;

        b.shutdown(true);
        b = null;
    }

}

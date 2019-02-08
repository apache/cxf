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

package demo.jaxrs.tracing;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.exceptions.SenderException;
import io.jaegertracing.spi.Sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jLogSender implements Sender {
    private static final Logger LOG = LoggerFactory.getLogger(Slf4jLogSender.class);
    @Override
    public int append(JaegerSpan span) throws SenderException {
        LOG.info("{}", span);
        return 0;
    }

    @Override
    public int flush() throws SenderException {
        return 0;
    }

    @Override
    public int close() throws SenderException {
        return 0;
    }
}

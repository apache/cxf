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
package org.apache.cxf.systest.jaeger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.exceptions.SenderException;
import io.jaegertracing.spi.Sender;

public class TestSender implements Sender {
    private static List<JaegerSpan> spans = Collections.synchronizedList(new ArrayList<>());
    
    @Override
    public int append(JaegerSpan span) throws SenderException {
        spans.add(span);
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

    public static List<JaegerSpan> getAllSpans() {
        return spans;
    }

    public static void clear() {
        spans.clear();
    }
}

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

package demo.jaxrs.tracing.conf;

import org.apache.htrace.core.SpanReceiver;
import org.apache.htrace.core.StandardOutSpanReceiver;

public final class TracingConfiguration {
    public static final Class<? extends SpanReceiver> SPAN_RECEIVER = StandardOutSpanReceiver.class;
    // Apache HBase client still uses Apache HTrace 3.x release branch
    public static final Class<? extends org.apache.htrace.SpanReceiver> HBASE_SPAN_RECEIVER = 
        org.apache.htrace.impl.StandardOutSpanReceiver.class;
    
    private TracingConfiguration() {
    }
}

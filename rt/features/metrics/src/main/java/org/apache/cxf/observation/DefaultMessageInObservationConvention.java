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

package org.apache.cxf.observation;

import org.apache.cxf.message.Exchange;

import io.micrometer.common.KeyValues;

/**
 *
 */
public class DefaultMessageInObservationConvention implements MessageInObservationConvention {

    public static final DefaultMessageInObservationConvention INSTANCE = new DefaultMessageInObservationConvention();

    @Override
    public KeyValues getLowCardinalityKeyValues(MessageInContext context) {
        return MessageInObservationConvention.super.getLowCardinalityKeyValues(context);
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(MessageInContext context) {
        return MessageInObservationConvention.super.getHighCardinalityKeyValues(context);
    }

    @Override
    public String getName() {
        return "rpc.server.duration";
    }

    @Override
    public String getContextualName(MessageInContext context) {
        Exchange exchange = context.getMessage().getExchange();
        return exchange.getService().getName().getLocalPart() + "/" + exchange.getBindingOperationInfo().getName().getLocalPart(); // TODO: Check this out
    }
}

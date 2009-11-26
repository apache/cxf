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
package org.apache.cxf.jaxrs.ext.logging.atom.deliverer;

import org.apache.abdera.model.Element;
import org.apache.commons.lang.Validate;

/**
 * Fires delivery of wrapper deliverer and forgets about status always assuming success. Fire-and-forget works
 * only for regular flow, runtime and interrupted exceptions are not handled.
 */
public final class FireAndForgetDeliverer implements Deliverer {

    private Deliverer deliverer;

    public FireAndForgetDeliverer(Deliverer worker) {
        Validate.notNull(worker, "worker is null");
        deliverer = worker;
    }

    public boolean deliver(Element element) throws InterruptedException {
        deliverer.deliver(element);
        return true;
    }
}

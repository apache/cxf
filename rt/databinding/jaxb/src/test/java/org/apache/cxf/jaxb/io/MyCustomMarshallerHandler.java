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

package org.apache.cxf.jaxb.io;

import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.util.ValidationEventCollector;
import org.apache.cxf.jaxb.MarshallerEventHandler;
import org.apache.cxf.jaxb.UnmarshallerEventHandler;

public class MyCustomMarshallerHandler extends ValidationEventCollector implements
        UnmarshallerEventHandler, MarshallerEventHandler {
    private boolean used;
    private boolean onMarshalComplete;
    private boolean onUnmarshalComplete;

    public boolean getUsed() {
        return used;
    }

    public boolean isOnMarshalComplete() {
        return onMarshalComplete;
    }

    public boolean isOnUnmarshalComplete() {
        return onUnmarshalComplete;
    }

    public boolean handleEvent(ValidationEvent event) {
        super.handleEvent(event);

        used = true;
        return true;
    }

    @Override
    public void onUnmarshalComplete() throws UnmarshalException {
        this.onUnmarshalComplete = true;

        if (hasEvents()) {
            throw new UnmarshalException("My unmarshalling exception");
        }
    }

    @Override
    public void onMarshalComplete() throws MarshalException {
        this.onMarshalComplete = true;

        if (hasEvents()) {
            throw new MarshalException("My marshalling exception");
        }
    }
}
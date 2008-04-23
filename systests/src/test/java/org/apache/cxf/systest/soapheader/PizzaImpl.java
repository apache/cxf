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

package org.apache.cxf.systest.soapheader;

import org.apache.cxf.pizza.Pizza;
import org.apache.cxf.pizza.types.CallerIDHeaderType;
import org.apache.cxf.pizza.types.OrderPizzaResponseType;
import org.apache.cxf.pizza.types.OrderPizzaType;

public class PizzaImpl implements Pizza {

    public OrderPizzaResponseType orderPizza(OrderPizzaType body, CallerIDHeaderType callerID) {
        OrderPizzaResponseType resp = new OrderPizzaResponseType();
        if (body.getToppings().getTopping().get(0).contains("NoHeader")) {
            resp.setMinutesUntilReady(100);
        } else {
            resp.setMinutesUntilReady(100 + Integer.parseInt(callerID.getPhoneNumber()));
        }
        return resp;
    }
}

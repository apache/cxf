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

package org.apache.cxf.management.web.browser.client.ui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasWidgets;


public abstract class BasePresenter implements Presenter {

    @Nonnull
    protected final EventBus eventBus;

    @Nonnull
    private final View view;

    @Nonnull
    private final BindStrategy bindStrategy;

    @Nonnull
    private final List<HandlerRegistration> handlerRegistrations;

    public BasePresenter(@Nonnull final EventBus eventBus, @Nonnull final View view,
                         @Nonnull final BindStrategy bindStrategy) {
        this.eventBus = eventBus;
        this.view = view;
        this.bindStrategy = bindStrategy;
        this.handlerRegistrations = new ArrayList<HandlerRegistration>();
    }

    public void go(HasWidgets container) {
        bindStrategy.bind(container, view);
    }

    public void unbind() {
        bindStrategy.unbind(view);
        
        for (HandlerRegistration handlerRegistration : handlerRegistrations) {
            handlerRegistration.removeHandler();
        }
        handlerRegistrations.clear();
    }

    protected void registerHandler(@Nonnull final HandlerRegistration handlerRegistration) {
        handlerRegistrations.add(handlerRegistration);
    }
}

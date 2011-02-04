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

package org.apache.cxf.management.web.browser.client;

import javax.annotation.Nonnull;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import org.apache.cxf.management.web.browser.client.ui.ErrorDialog;

public class LogBrowser implements EntryPoint {

    @Nonnull
    private final Injector injector = GWT.create(Injector.class);

    public void onModuleLoad() {
        injector.getResources().css().ensureInjected();
        
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

            public void onUncaughtException(@Nonnull final Throwable throwable) {
                ErrorDialog errorDialog = injector.getErrorDialog();
                errorDialog.setException(throwable);
                errorDialog.center();
            }
        });
        
        injector.getDispatcher().start();
    }
}

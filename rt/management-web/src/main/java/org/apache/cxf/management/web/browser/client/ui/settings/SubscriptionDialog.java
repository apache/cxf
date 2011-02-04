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

package org.apache.cxf.management.web.browser.client.ui.settings;

import java.util.Map;

import com.google.gwt.user.client.ui.HasValue;

import org.apache.cxf.management.web.browser.client.service.settings.Subscription;

public interface SubscriptionDialog {

    public interface Presenter {

        void onSaveButtonClicked(String id, HasValue<String> name, HasValue<String> url);

        void onCancelButtonClicked();
    }

    void center();

    void show();

    void hide();

    void setValidationErrors(Map<HasValue, String> errors);

    void setTitle(String title);

    void setData(Subscription subscription);

    void setPresenter(Presenter presenter);
}
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

package org.apache.cxf.management.web.browser.client.ui.common;

import javax.annotation.Nonnull;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class NavigationHeaderViewImpl extends Composite implements NavigationHeaderView {

    @UiTemplate("NavigationHeaderView.ui.xml")
    interface NavigationHeaderViewUiBinder extends UiBinder<Widget, NavigationHeaderViewImpl> { }

    private static final NavigationHeaderViewUiBinder UI_BINDER =
            GWT.create(NavigationHeaderViewUiBinder.class);

    @Nonnull
    private Presenter presenter;

    public NavigationHeaderViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    @UiHandler("settingsHyperlink")
    void onSignOutHyperlinkClicked(@Nonnull final ClickEvent event) {
        assert presenter != null;
        presenter.onSettingsButtonClicked();
    }

    public void setPresenter(@Nonnull final Presenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        return this;
    }
}

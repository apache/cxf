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

package org.apache.cxf.management.web.browser.client.ui.accesscontroler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Singleton;

@Singleton
public class AccessControlViewImpl extends Composite implements AccessControlView {

    @UiTemplate("AccessControlView.ui.xml")
    interface AccessControlViewUiBinder extends UiBinder<Widget, AccessControlViewImpl> { }

    private static final AccessControlViewUiBinder UI_BINDER = GWT.create(AccessControlViewUiBinder.class);
    
    @UiField @Nonnull
    TextBox usernameTextBox;

    @UiField @Nonnull
    CheckBox rememberMeCheckBox;

    @UiField @Nonnull
    PasswordTextBox passwordTextBox;

    @UiField @Nonnull
    Label accessDeniedLabel;

    @Nullable
    private Presenter presenter;

    public AccessControlViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    @UiHandler("signInButton")
    void onAddButtonClicked(@Nonnull final ClickEvent event) {
        assert presenter != null;
        presenter.onSignInButtonClicked();
    }

    @Nonnull
    public HasValue<String> getPassword() {
        return passwordTextBox;
    }

    @Nonnull
    public HasValue<Boolean> getRememberMe() {
        return rememberMeCheckBox;
    }

    @Nonnull
    public HasValue<String> getUsername() {
        return usernameTextBox;
    }

    public void showAccessDeniedMessage() {
        accessDeniedLabel.setVisible(true);
    }

    public void setPresenter(@Nonnull final Presenter presenter) {
        this.presenter = presenter;
    }

    @Nonnull
    public Widget asWidget() {
        return this;
    }
}

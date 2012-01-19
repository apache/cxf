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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.apache.cxf.management.web.browser.client.service.settings.Subscription;

public class SubscriptionDialogImpl extends DialogBox implements SubscriptionDialog {

    @Nonnull
    private Form form;

    @Nullable
    private Presenter presenter;

    @Nullable
    private String subscriptionId;

    public SubscriptionDialogImpl() {
        form = new Form();
        form.nameErrorLabel.setVisible(false);
        form.urlErrorLabel.setVisible(false);

        form.cancelButton.addClickHandler(new ClickHandler() {

            public void onClick(@Nonnull final ClickEvent clickEvent) {
                assert presenter != null;
                presenter.onCancelButtonClicked();
            }
        });
        form.saveButton.addClickHandler(new ClickHandler() {

            public void onClick(@Nonnull final ClickEvent clickEvent) {
                assert presenter != null;
                presenter.onSaveButtonClicked(subscriptionId, form.nameTextBox, form.urlTextBox);
            }
        });

        setGlassEnabled(true);
        setAnimationEnabled(false);
        setAutoHideEnabled(true);
        setWidget(form);
    } 

    public void setValidationErrors(@Nullable Map<HasValue, String> errors) {
        form.nameErrorLabel.setVisible(false);
        form.urlErrorLabel.setVisible(false);
        
        if (errors == null) {
            return;
        }

        if (errors.containsKey(form.nameTextBox)) {
            form.nameErrorLabel.setText(errors.get(form.nameTextBox));
            form.nameErrorLabel.setVisible(true);
        }
        if (errors.containsKey(form.urlTextBox)) {
            form.urlErrorLabel.setText(errors.get(form.urlTextBox));
            form.urlErrorLabel.setVisible(true);
        }
    }

    public void setTitle(@Nonnull final String title) {
        setText(title);
    }

    public void setData(@Nullable final Subscription subscription) {
        if (subscription == null) {
            form.nameTextBox.setValue("");
            form.urlTextBox.setValue("");
            subscriptionId = null;
        } else {
            form.nameTextBox.setValue(subscription.getName());
            form.urlTextBox.setValue(subscription.getUrl());
            subscriptionId = subscription.getId();
        }
    }

    public void setPresenter(@Nonnull final Presenter presenter) {
        this.presenter = presenter;
    }

    protected static class Form extends Composite {

        @UiTemplate("SubscriptionForm.ui.xml")
        interface FormViewUiBinder extends UiBinder<Widget, Form> { }

        private static final FormViewUiBinder UI_BINDER = GWT.create(FormViewUiBinder.class);

        @UiField @Nonnull
        TextBox nameTextBox;

        @UiField @Nonnull
        Label nameErrorLabel;

        @UiField @Nonnull
        TextBox urlTextBox;

        @UiField @Nonnull
        Label urlErrorLabel;

        @UiField @Nonnull
        Button saveButton;

        @UiField @Nonnull
        Button cancelButton;

        public Form() {
            initWidget(UI_BINDER.createAndBindUi(this));
        }
    }
}

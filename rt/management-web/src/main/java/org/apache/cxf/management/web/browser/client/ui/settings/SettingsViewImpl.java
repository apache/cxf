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

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Singleton;

import org.apache.cxf.management.web.browser.client.service.settings.Subscription;

@Singleton
public class SettingsViewImpl extends Composite implements SettingsView {

    @UiTemplate("SettingsView.ui.xml")
    interface SettingsViewUiBinder extends UiBinder<Widget, SettingsViewImpl> {
    }

    private static final SettingsViewUiBinder UI_BINDER = GWT.create(SettingsViewUiBinder.class);

    @UiField @Nonnull
    DecoratedTabPanel tabPanel;

    @UiField @Nonnull
    FlowPanel navigationHeaderSlot;

    @UiField @Nonnull
    FlexTable subscriptionsTable;

    @Nullable
    private Presenter presenter;

    public SettingsViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));
        tabPanel.selectTab(0);
    }

    public HasWidgets getNaviagationHeaderSlot() {
        return navigationHeaderSlot;
    }

    public void setData(@Nonnull final List<Subscription> subscriptions) {
        subscriptionsTable.clear();

        int row = 0;
        for (final Subscription subscription : subscriptions) {
            SubscriptionEntry subscriptionEntry = new SubscriptionEntry();

            subscriptionEntry.nameLabel.setText(subscription.getName());
            subscriptionEntry.urlLabel.setText(subscription.getUrl());

            subscriptionEntry.editButton.addClickHandler(new ClickHandler() {

                public void onClick(@Nonnull final ClickEvent event) {
                    assert presenter != null;
                    presenter.onEditSubscriptionButtonClicked(subscription);
                }
            });
            subscriptionEntry.removeButton.addClickHandler(new ClickHandler() {

                public void onClick(@Nonnull final ClickEvent event) {
                    assert presenter != null;
                    presenter.onRemoveSubscriptionButtonClicked(subscription);
                }
            });

            subscriptionsTable.setWidget(row, 0, subscriptionEntry);

            row++;
        }
    }

    @UiHandler("backHyperlink")
    void onBackHyperlinkClicked(@Nonnull final ClickEvent event) {
        assert presenter != null;
        presenter.onBackHyperlinkClicked();
    }

    @UiHandler("addSubscriptionButton")
    void onAddSubscriptionButtonClicked(@Nonnull final ClickEvent event) {
        assert presenter != null;
        presenter.onAddSubscriptionButtonClicked();
    }

    public void setPresenter(@Nonnull final Presenter presenter) {
        this.presenter = presenter;
    }

    @Nonnull
    public Widget asWidget() {
        return this;
    }

    protected static class SubscriptionEntry extends Composite {

        @UiTemplate("SubscriptionEntry.ui.xml")
        interface SubscriptionEntryUiBinder extends UiBinder<Widget, SubscriptionEntry> { }

        private static final SubscriptionEntryUiBinder UI_BINDER =
            GWT.create(SubscriptionEntryUiBinder.class);

        @UiField @Nonnull
        Label nameLabel;

        @UiField @Nonnull
        Label urlLabel;

        @UiField @Nonnull
        Button editButton;

        @UiField @Nonnull
        Button removeButton;

        public SubscriptionEntry() {
            initWidget(UI_BINDER.createAndBindUi(this));
        }
    }

}

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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.cxf.management.web.browser.client.event.ChangedSubscriptionsEvent;
import org.apache.cxf.management.web.browser.client.event.GoToBrowserEvent;
import org.apache.cxf.management.web.browser.client.service.settings.SettingsFacade;
import org.apache.cxf.management.web.browser.client.service.settings.Subscription;
import org.apache.cxf.management.web.browser.client.ui.BasePresenter;
import org.apache.cxf.management.web.browser.client.ui.BindStrategy;
import org.apache.cxf.management.web.browser.client.ui.common.NavigationHeaderPresenter;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserConstans;

@Singleton
public class SettingsPresenter extends BasePresenter
        implements SettingsView.Presenter, SubscriptionDialog.Presenter {

    @Nonnull
    private final SettingsView view;

    @Nonnull
    private final SettingsFacade settingsFacade;

    @Nonnull
    private final SubscriptionDialog subscriptionDialog;

    @Nonnull
    private final LogBrowserConstans constans;

    @Inject
    public SettingsPresenter(@Nonnull final EventBus eventBus,
                             @Nonnull final SettingsView view,
                             @Nonnull @Named("BindStrategyForSettings") final BindStrategy bindStrategy,
                             @Nonnull final NavigationHeaderPresenter navigationHeaderPresenter,
                             @Nonnull final SettingsFacade settingsFacade,
                             @Nonnull final SubscriptionDialog subscriptionDialog,
                             @Nonnull final LogBrowserConstans constans) {
        super(eventBus, view, bindStrategy);

        this.view = view;
        this.settingsFacade = settingsFacade;
        this.subscriptionDialog = subscriptionDialog;
        this.constans = constans;
        
        this.view.setPresenter(this);
        this.subscriptionDialog.setPresenter(this);

        navigationHeaderPresenter.go(view.getNaviagationHeaderSlot());

        updateSubscriptions();
    }

    public void go(@Nonnull final HasWidgets container) {
        container.clear();
        container.add(view.asWidget());
    }

    public void onAddSubscriptionButtonClicked() {
        showSubscriptionDialog(constans.settingsTabAddSubscriptionDialogTitle(), null);
    }

    public void onEditSubscriptionButtonClicked(@Nonnull final Subscription subscription) {
        showSubscriptionDialog(constans.settingsTabEditSubscriptionDialogTitle(), subscription);
    }

    public void onRemoveSubscriptionButtonClicked(@Nonnull final Subscription subscription) {
        settingsFacade.removeSubscription(subscription);
        updateSubscriptions();
    }

    public void onSaveButtonClicked(@Nullable final String id,
                                    @Nonnull final HasValue<String> name,
                                    @Nonnull final HasValue<String> url) {
        Map<HasValue, String> errors = validate(name, url);

        if (errors.isEmpty()) {
            String nameValue = name.getValue();
            String urlValue = url.getValue();

            if (id == null) {
                settingsFacade.addSubscription(nameValue, urlValue);
            } else {
                settingsFacade.updateSubscription(new Subscription(id, nameValue, urlValue));
            }

            updateSubscriptions();
            subscriptionDialog.hide();
        } else {
            subscriptionDialog.setValidationErrors(errors);
        }
    }

    public void onCancelButtonClicked() {
        subscriptionDialog.hide();
    }

    public void onBackHyperlinkClicked() {
        eventBus.fireEvent(new GoToBrowserEvent());
    }

    private void updateSubscriptions() {
        eventBus.fireEvent(new ChangedSubscriptionsEvent());
        view.setData(settingsFacade.getSubscriptions());
    }

    private void showSubscriptionDialog(@Nonnull final String title, @Nullable final Subscription data) {
        subscriptionDialog.setTitle(title);
        subscriptionDialog.setData(data);
        subscriptionDialog.setValidationErrors(null);
        subscriptionDialog.center();
        subscriptionDialog.show();
    }

    private Map<HasValue, String> validate(@Nonnull final HasValue<String> name,
                                           @Nonnull final HasValue<String> url) {
        Map<HasValue, String> errors = new HashMap<HasValue, String>();

        String nameValue = name.getValue();
        if (nameValue == null || nameValue.length() == 0) {
            errors.put(name, constans.settingsTabSubscriptionDialogEmptyName());
        }

        String urlValue = url.getValue();
        if (urlValue == null || urlValue.length() == 0) {
            errors.put(url, constans.settingsTabSubscriptionDialogEmptyUrl());
        }

        return errors;
    }
}

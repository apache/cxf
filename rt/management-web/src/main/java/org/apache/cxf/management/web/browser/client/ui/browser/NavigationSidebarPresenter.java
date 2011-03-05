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

package org.apache.cxf.management.web.browser.client.ui.browser;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.cxf.management.web.browser.client.EventBus;
import org.apache.cxf.management.web.browser.client.event.ChangedFilterOptionsEvent;
import org.apache.cxf.management.web.browser.client.event.ChangedFilterOptionsEventHandler;
import org.apache.cxf.management.web.browser.client.event.ChangedSubscriptionsEvent;
import org.apache.cxf.management.web.browser.client.event.ChangedSubscriptionsEventHandler;
import org.apache.cxf.management.web.browser.client.event.GoToEditCriteriaEvent;
import org.apache.cxf.management.web.browser.client.event.GoToSettingsEvent;
import org.apache.cxf.management.web.browser.client.event.SelectedSubscriptionEvent;
import org.apache.cxf.management.web.browser.client.service.browser.FilterOptions;
import org.apache.cxf.management.web.browser.client.service.browser.FilterOptions.Level;
import org.apache.cxf.management.web.browser.client.service.settings.SettingsFacade;
import org.apache.cxf.management.web.browser.client.service.settings.Subscription;
import org.apache.cxf.management.web.browser.client.ui.BasePresenter;
import org.apache.cxf.management.web.browser.client.ui.BindStrategy;

@Singleton
public class NavigationSidebarPresenter extends BasePresenter implements NavigationSidebarView.Presenter {
    @Nonnull private final NavigationSidebarView view;
    @Nonnull private final SettingsFacade settingsManager;

    @Nonnull private FilterOptions filterOptions = FilterOptions.EMPTY;

    @Nullable private Subscription selectedSubscription;
    @Nonnull private List<Subscription> subscriptions;

    @Inject
    public NavigationSidebarPresenter(@Nonnull final EventBus eventBus,
            @Nonnull final NavigationSidebarView view,
            @Nonnull @Named("BindStrategyForNavigationSidebar") final BindStrategy bindStrategy,
            @Nonnull final SettingsFacade settingsManager) {
        super(eventBus, view, bindStrategy);

        this.view = view;
        this.view.setPresenter(this);

        this.settingsManager = settingsManager;

        bind();

        updateSubscriptions();
    }

    public void onExploreSubcriptionItemClicked(int row) {
        assert row >= 0 && row < subscriptions.size();
        selectedSubscription = subscriptions.get(row);
        eventBus.fireEvent(new SelectedSubscriptionEvent(selectedSubscription.getUrl()));
    }

    public void onFilterSubcriptionItemClicked(int row) {
        assert row >= 0 && row < subscriptions.size();
        selectedSubscription = subscriptions.get(row);
        selectSubscriptionWithFilterOptions();
    }

    private void selectSubscriptionWithFilterOptions() {
        assert selectedSubscription != null;

        StringBuilder url = new StringBuilder(selectedSubscription.getUrl());

        if (filterOptions != FilterOptions.EMPTY) {
            url.append("?_s=");

            if (filterOptions.getFrom() != null) {
                url.append("date=ge=");
                url.append(filterOptions.getFrom().getTime());
                url.append(";");
            }

            if (filterOptions.getTo() != null) {
                url.append("date=lt=");
                url.append(filterOptions.getTo().getTime());
                url.append(";");
            }

            if (!filterOptions.getLevels().isEmpty()) {
                for (Level level : filterOptions.getLevels()) {
                    url.append("level==");
                    url.append(level);
                    url.append(";");
                }
            }
        }

        eventBus.fireEvent(new SelectedSubscriptionEvent(url.toString()));
    }

    public void onManageSubscriptionsButtonClicked() {
        eventBus.fireEvent(new GoToSettingsEvent());
    }

    public void onEditCriteriaHyperinkClicked() {
        eventBus.fireEvent(new GoToEditCriteriaEvent());
    }

    private void updateSubscriptions() {
        subscriptions = settingsManager.getSubscriptions();
        view.setSubscriptions(subscriptions);
    }

    private void bind() {
        eventBus.addHandler(ChangedSubscriptionsEvent.TYPE, new ChangedSubscriptionsEventHandler() {

            public void onChangedSubscriptions(ChangedSubscriptionsEvent event) {
                updateSubscriptions();
            }
        });

        eventBus.addHandler(ChangedFilterOptionsEvent.TYPE, new ChangedFilterOptionsEventHandler() {

            public void onChangedFilterOptions(ChangedFilterOptionsEvent event) {
                filterOptions = event.getFilterOptions();
                selectSubscriptionWithFilterOptions();
            }
        });
    }
}

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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Singleton;

import org.apache.cxf.management.web.browser.client.service.settings.Subscription;

@Singleton
public class NavigationSidebarViewImpl extends Composite implements NavigationSidebarView {

    @UiTemplate("NavigationSidebarView.ui.xml")
    interface NavigationSidebarViewUiBinder extends UiBinder<Widget, NavigationSidebarViewImpl> { }

    private static final NavigationSidebarViewUiBinder UI_BINDER =
            GWT.create(NavigationSidebarViewUiBinder.class);    

    @UiField @Nonnull
    SubscriptionTable exploreSubscriptionTable;

    @UiField @Nonnull
    SubscriptionTable filterSubscriptionTable;

    @Nullable
    private Presenter presenter;

    public NavigationSidebarViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));

        addColumnDefinitions(exploreSubscriptionTable);
        exploreSubscriptionTable.addSelectRowHandler(new SelectableTable.SelectRowHandler() {

            public void onSelectRow(int row) {

                // Remove selection from subscription list in filter section
                filterSubscriptionTable.deselect();
                presenter.onExploreSubcriptionItemClicked(row);
            }
        });

        addColumnDefinitions(filterSubscriptionTable);
        filterSubscriptionTable.addSelectRowHandler(new SelectableTable.SelectRowHandler() {

            public void onSelectRow(int row) {

                // Remove selection from subscription list in explore section
                exploreSubscriptionTable.deselect();
                presenter.onFilterSubcriptionItemClicked(row);
            }
        });
    }

    public void setSubscriptions(@Nonnull final List<Subscription> subscriptions) {
        exploreSubscriptionTable.setData(subscriptions);
        filterSubscriptionTable.setData(subscriptions);
    }

    @UiHandler("editCriteriaHyperlink")
    void onEditCriteriaHyperlinkClicked(@Nonnull final ClickEvent event) {
        assert presenter != null;
        presenter.onEditCriteriaHyperinkClicked();
    }

    @UiHandler("manageSubscriptionsHyperlink")
    void onManageSubscriptionsHyperlinkClicked(@Nonnull final ClickEvent event) {
        assert presenter != null;
        presenter.onManageSubscriptionsButtonClicked();
    }

    @SuppressWarnings("unchecked")
    private void addColumnDefinitions(@Nonnull SubscriptionTable table) {
        table.setColumnDefinitions(new SelectableTable.ColumnDefinition<Subscription>() {

            public String getContent(Subscription subscription) {
                return subscription.getName();
            }

            public String getWidth() {
                return null;
            }
        });
    }

    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        return this;
    }
}

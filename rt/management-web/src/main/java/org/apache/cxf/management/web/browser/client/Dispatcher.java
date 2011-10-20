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
import javax.annotation.Nullable;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.cxf.management.web.browser.client.event.GoToBrowserEvent;
import org.apache.cxf.management.web.browser.client.event.GoToBrowserEventHandler;
import org.apache.cxf.management.web.browser.client.event.GoToEditCriteriaEvent;
import org.apache.cxf.management.web.browser.client.event.GoToEditCriteriaEventHandler;
import org.apache.cxf.management.web.browser.client.event.GoToSettingsEvent;
import org.apache.cxf.management.web.browser.client.event.GoToSettingsEventHandler;
import org.apache.cxf.management.web.browser.client.service.settings.SettingsFacade;
import org.apache.cxf.management.web.browser.client.service.settings.SettingsFacade.StorageStrategy;
import org.apache.cxf.management.web.browser.client.ui.Presenter;
import org.apache.cxf.management.web.browser.client.ui.browser.BrowsePresenter;
import org.apache.cxf.management.web.browser.client.ui.browser.EditCriteriaPresenter;
import org.apache.cxf.management.web.browser.client.ui.settings.SettingsPresenter;

public class Dispatcher {

    @Nonnull
    private final EventBus eventBus;

    @Nonnull
    private final Provider<BrowsePresenter> browseProvider;

    @Nonnull
    private final Provider<EditCriteriaPresenter> editCriteriaProvider;

    @Nonnull
    private final Provider<SettingsPresenter> settingsProvider;

    @Nonnull
    private final SettingsFacade settingsFacade;

    @Nullable
    private Presenter currentPresenter;

    @Inject
    public Dispatcher(@Nonnull final EventBus eventBus,
                      @Nonnull final SettingsFacade settingsFacade,
                      @Nonnull final Provider<BrowsePresenter> browseProvider,
                      @Nonnull final Provider<EditCriteriaPresenter> editCriteriaProvider,
                      @Nonnull final Provider<SettingsPresenter> settingsProvider) {
        this.eventBus = eventBus;
        this.browseProvider = browseProvider;
        this.editCriteriaProvider = editCriteriaProvider;
        this.settingsProvider = settingsProvider;
        this.settingsFacade = settingsFacade;

        bind();
    }

    public void start() {
        settingsFacade.initialize(StorageStrategy.REMOTE);
        eventBus.fireEvent(new GoToBrowserEvent());
    }

    private void go(@Nonnull final Presenter newPresenter) {
        if (currentPresenter != null) {
            currentPresenter.unbind();
        }

        currentPresenter = newPresenter;

        currentPresenter.go(RootLayoutPanel.get());
    }

    private void bind() {

        eventBus.addHandler(GoToBrowserEvent.TYPE, new GoToBrowserEventHandler() {
            public void onGoToBrowser(@Nonnull final GoToBrowserEvent event) {
                go(browseProvider.get());
            }
        });

        eventBus.addHandler(GoToEditCriteriaEvent.TYPE, new GoToEditCriteriaEventHandler() {
            public void onGoToEditCriteria(@Nonnull final GoToEditCriteriaEvent event) {
                go(editCriteriaProvider.get());
            }
        });

        eventBus.addHandler(GoToSettingsEvent.TYPE, new GoToSettingsEventHandler() {

            public void onGoToSettings(@Nonnull final GoToSettingsEvent event) {
                go(settingsProvider.get());
            }
        });
    }
}

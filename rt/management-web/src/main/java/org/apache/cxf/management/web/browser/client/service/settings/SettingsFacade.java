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

package org.apache.cxf.management.web.browser.client.service.settings;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.cxf.management.web.browser.client.event.ChangedSubscriptionsEvent;

//TODO Remove StorageStrategy feature
@Singleton
public class SettingsFacade {

    @Nonnull
    private final RemoteStorageProxy remoteStorage;

    @Nonnull
    private final EventBus eventBus;

    @Nullable
    private StorageLayer storageLayer;

    @Nonnull
    private final IdentifierGenerator identifierGenerator;

    private boolean initialized;

    public enum StorageStrategy {
        REMOTE
    }    

    @Inject
    public SettingsFacade(@Nonnull final RemoteStorageProxy remoteStorage,
                          @Nonnull final EventBus eventBus,
                          @Nonnull final IdentifierGenerator identifierGenerator) {
        this.remoteStorage = remoteStorage;
        this.eventBus = eventBus;
        this.identifierGenerator = identifierGenerator;
    }

    public void initialize(@Nonnull final StorageStrategy strategy) {
        storageLayer = createStorageLayers(strategy);
        storageLayer.initialize();
        initialized = true;
    }

    public void addSubscription(@Nonnull final String name, @Nonnull final String url) {
        assert !"".equals(name);
        assert !"".equals(url);
        assert storageLayer != null;
        isValid();

        String id = identifierGenerator.generateUUID();
        assert id != null && !"".equals(id);

        storageLayer.getSettings().getSubscriptions().add(new Subscription(id, name, url));
        storageLayer.update();
    }

    public void updateSubscription(@Nonnull final Subscription subscription) {
        assert subscription.getUrl() != null && !"".equals(subscription.getUrl());
        assert storageLayer != null;
        isValid();

        storageLayer.getSettings().getSubscriptions().remove(subscription);
        storageLayer.getSettings().getSubscriptions().add(subscription);
        storageLayer.update();
    }

    public void removeSubscription(@Nonnull final Subscription subscription) {
        assert subscription.getUrl() != null && !"".equals(subscription.getUrl());
        assert storageLayer != null;
        isValid();

        storageLayer.getSettings().getSubscriptions().remove(subscription);
        storageLayer.update();
    }

    @Nonnull
    public List<Subscription> getSubscriptions() {
        assert storageLayer != null;
        isValid();

        return new ArrayList<Subscription>(storageLayer.getSettings().getSubscriptions());
    }

    private void isValid() {
        if (!initialized) {
            throw new IllegalStateException("Storage layers not initialized");
        }
    }

    @Nonnull
    private StorageLayer createStorageLayers(@Nonnull final StorageStrategy storageStrategy) {
        switch(storageStrategy) {
        case REMOTE:
            return new RemoteStorageLayer(remoteStorage, new MemoryStorageLayer());
        default:
            throw new IllegalArgumentException("Unknown storage strategy type");
        }
    }

    private interface StorageLayer {

        boolean initialize();

        Settings getSettings();

        void update(Settings settings);

        void update();

        void clear();
    }

    private static class MemoryStorageLayer implements StorageLayer {

        @Nonnull
        private Settings settings;

        public boolean initialize() {
            this.settings = new Settings();
            return false;
        }

        @Nonnull
        public Settings getSettings() {
            return this.settings;
        }

        public void update(@Nonnull final Settings newSettings) {
            this.settings = newSettings;
        }

        public void update() {
        }

        public void clear() {
            this.settings = new Settings();
        }
    }

    private class RemoteStorageLayer implements StorageLayer {

        @Nonnull
        private final StorageLayer parent;

        @Nonnull
        private final RemoteStorageProxy remoteStorage;

        RemoteStorageLayer(@Nonnull final RemoteStorageProxy remoteStorage,
                                  @Nonnull final StorageLayer parent) {
            this.parent = parent;
            this.remoteStorage = remoteStorage;
        }

        public boolean initialize() {
            boolean isSuccess = parent.initialize();
            
            if (!isSuccess) {
                remoteStorage.saveSettings(new RemoteStorageProxyImpl.Callback() {

                    @Override
                    public void onSuccess(@Nullable final Settings retrievedSettings) {
                        Settings settings = retrievedSettings != null ? retrievedSettings : new Settings();

                        parent.update(settings);

                        eventBus.fireEvent(new ChangedSubscriptionsEvent());
                    }
                });
            }

            return true;
        }

        @Nonnull
        public Settings getSettings() {
            return parent.getSettings();
        }

        public void update(@Nonnull final Settings settings) {
            assert settings != null;

            parent.update(settings);

            remoteStorage.retrieveSettings(settings, new RemoteStorageProxyImpl.NoActionCallback());
        }
        
        public void update() {
            update(parent.getSettings());
        }

        public void clear() {
            parent.clear();
        }
    }
}


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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.cxf.management.web.browser.client.event.ChangedFilterOptionsEvent;
import org.apache.cxf.management.web.browser.client.service.browser.FilterOptions;
import org.apache.cxf.management.web.browser.client.service.browser.FilterOptions.Level;
import org.apache.cxf.management.web.browser.client.ui.BasePresenter;
import org.apache.cxf.management.web.browser.client.ui.BindStrategy;

@Singleton
public class EditCriteriaPresenter extends BasePresenter implements EditCriteriaView.Presenter {

    @Nonnull
    private final EditCriteriaView view;

    @Inject
    public EditCriteriaPresenter(@Nonnull final EventBus eventBus,
            @Nonnull final EditCriteriaView view,
            @Nonnull @Named("BindStrategyForEditCriteria") final BindStrategy bindStrategy) {
        super(eventBus, view, bindStrategy);

        this.view = view;
        this.view.setPresenter(this);
    }

    public void onSaveButtonClicked() {
        String phrase = view.getPhraseValue().getValue();
        Date from = view.getFromValue().getValue();
        Date to = view.getToValue().getValue();

        List<Level> acceptedLevels = new ArrayList<FilterOptions.Level>();

        if (view.getDebugValue().getValue()) {
            acceptedLevels.add(Level.DEBUG);
        }
        if (view.getInfoValue().getValue()) {
            acceptedLevels.add(Level.INFO);
        }
        if (view.getWarnValue().getValue()) {
            acceptedLevels.add(Level.WARN);
        }
        if (view.getErrorValue().getValue()) {
            acceptedLevels.add(Level.ERROR);
        }

        FilterOptions filterOptions;
        if (from == null && to == null && acceptedLevels.isEmpty()) {
            filterOptions = FilterOptions.EMPTY;
        } else {
            filterOptions = new FilterOptions(phrase, from, to, acceptedLevels);
        }

        eventBus.fireEvent(new ChangedFilterOptionsEvent(filterOptions));
    }
}

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
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.cxf.management.web.browser.client.service.browser.Entry;
import org.apache.cxf.management.web.browser.client.service.browser.Links;
import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserConstans;

@Singleton
public class ViewerViewImpl extends Composite implements ViewerView {

    private static final DateTimeFormat DT_FORMATTER =
        DateTimeFormat.getFormat("HH:mm:ss dd.MM.yyyy");

    @UiTemplate("ViewerView.ui.xml")
    interface ViewerViewUiBinder extends UiBinder<Widget, ViewerViewImpl> { }

    private static final ViewerViewUiBinder UI_BINDER =
            GWT.create(ViewerViewUiBinder.class);

    @UiField @Nonnull
    EntryTable entryTable;

    @UiField @Nonnull
    HTML entryDetails;

    @UiField @Nonnull
    Anchor refreshButton;

    @UiField @Nonnull
    Anchor newerButton;

    @UiField @Nonnull
    Anchor olderButton;

    @UiField @Nonnull
    Anchor lastButton;

    @UiField @Nonnull
    Anchor firstButton;    

    private Presenter presenter;

    @Nonnull
    private final LogBrowserConstans constans;

    @Inject
    public ViewerViewImpl(@Nonnull final LogBrowserConstans constans) {
        this.constans = constans;
        
        initWidget(UI_BINDER.createAndBindUi(this));

        initEntryTable();
    }

    public void setEntries(@Nonnull final List<Entry> entries) {
        entryTable.setData(entries);
    }


    public void setMessageInsteadOfEntries(@Nonnull final String message, @Nullable final String styleName) {
        entryTable.setMessageInsteadOfData(message, styleName);
    }

    public void setEntryDetails(@Nullable final Entry entry) {
        if (entry == null) {
            entryDetails.setHTML("");
            return;
        }

        String val = entry.getMessage() != null 
            ? entry.getMessage() : entry.getThrowable() != null
            ? entry.getThrowable() : "";
        entryDetails.setHTML(SafeHtmlUtils.fromString(val));
    }

    public void setLinks(@Nonnull final Links links) {
        olderButton.setVisible(links.previousAvailable());
        newerButton.setVisible(links.nextAvailable());
        refreshButton.setVisible(links.selfAvailable());
        lastButton.setVisible(links.lastAvailable());
        firstButton.setVisible(links.firstAvailable());
    }

    @UiHandler("firstButton")
    void onFirstButtonClicked(@Nonnull ClickEvent event) {
        assert presenter != null;
        presenter.onFirstButtonClicked();
    }

    @UiHandler("newerButton")
    void onNewerButtonClicked(@Nonnull ClickEvent event) {
        assert presenter != null;
        presenter.onNewerButtonClicked();
    }

    @UiHandler("refreshButton")
    void onRefreshButtonClicked(@Nonnull ClickEvent event) {
        assert presenter != null;
        presenter.onRefreshButtonClicked();
    }

    @UiHandler("olderButton")
    void onOlderButtonClicked(@Nonnull ClickEvent event) {
        assert presenter != null;
        presenter.onOlderButtonClicked();
    }

    @UiHandler("lastButton")
    void onLastButtonClicked(@Nonnull ClickEvent event) {
        assert presenter != null;
        presenter.onLastButtonClicked();
    }

    @SuppressWarnings("unchecked")
    private void initEntryTable() {
        entryTable.setColumnDefinitions(
            new SelectableTable.ColumnDefinition<Entry>() {
    
                public String getContent(Entry entry) {
                    return DT_FORMATTER.format(entry.getEventTimestamp());
                }

                public String getWidth() {
                    return constans.browseTabDatatimeColumnWidth();
                }
            },
            new SelectableTable.ColumnDefinition<Entry>() {

                public String getContent(Entry entry) {
                    return entry.getLevel();
                }

                public String getWidth() {
                    return constans.browseTabLevelColumnWidth();
                }
            },
            new SelectableTable.ColumnDefinition<Entry>() {

                public String getContent(Entry entry) {
                    return entry.getTitle();
                }

                public String getWidth() {
                    return null;
                }
            }
        );

        entryTable.addSelectRowHandler(new SelectableTable.SelectRowHandler() {

            public void onSelectRow(int row) {
                assert presenter != null;
                presenter.onEntryItemClicked(row);
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
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
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.UIObject;

import org.apache.cxf.management.web.browser.client.ui.resources.LogBrowserResources;

public class SelectableTable<T> extends Composite {

    @Nonnull
    private final ScrollPanel scroller;

    @Nullable
    private final FocusPanel focuser;

    @Nonnull
    private final FlexTable table;

    @Nonnull
    private final List<SelectRowHandler> selectRowHandlers;

    private final boolean hotkeysEnabled;

    private int selectedRowIndex;
    
    private boolean isRowSelected;

    private Label messageLabel;

    @Nullable
    private List<ColumnDefinition<T>> columnDefinitions;

    @Nonnull
    private LogBrowserResources resources = GWT.create(LogBrowserResources.class);

    public SelectableTable(final boolean hotkeysEnabled) {
        this.hotkeysEnabled = hotkeysEnabled;
        selectRowHandlers = new ArrayList<SelectRowHandler>();

        table = new FlexTable();
        table.setCellPadding(0);
        table.setCellSpacing(0);
        table.setVisible(false);

        messageLabel = new Label();
        messageLabel.setVisible(false);

        FlowPanel content = new FlowPanel();
        content.add(messageLabel);
        content.add(table);

        scroller = new ScrollPanel();

        if (hotkeysEnabled) {
            focuser = new FocusPanel();
            focuser.setWidth("99%");
            focuser.add(content);

            scroller.add(focuser);
        } else {
            focuser = null;

            scroller.add(content);
        }

        addEventHandlers();

        initWidget(scroller);
    }

    public void setMessageInsteadOfData(@Nonnull final String message, @Nullable final String styleName) {
        messageLabel.setText(message);

        messageLabel.setVisible(true);
        table.setVisible(false);

        if (styleName != null) {
            messageLabel.setStyleName(styleName);
        }
    }

    public void setData(@Nonnull final List<T> entries) {
        assert columnDefinitions != null;
        
        table.removeAllRows();

        messageLabel.setVisible(false);
        table.setVisible(true);
        
        for (int i = 0; i < entries.size(); i++) {
            T entry = entries.get(i);
            for (int j = 0; j < columnDefinitions.size(); j++) {
                ColumnDefinition<T> columnDefinition = columnDefinitions.get(j);
                table.setText(i, j, columnDefinition.getContent(entry));
                table.getCellFormatter().addStyleName(i, j, resources.css().selectableTableRow());
            }
        }

        restoreRowSelection();

        if (hotkeysEnabled) {
            focuser.setFocus(true);
        }
    }

    public void setColumnDefinitions(@Nonnull final List<ColumnDefinition<T>> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
        setColumnsWidth();
    }

    public void setColumnDefinitions(ColumnDefinition<T>... columnDefinitions) {
        this.columnDefinitions = Arrays.asList(columnDefinitions);
        setColumnsWidth();
    }

    public void addSelectRowHandler(@Nonnull final SelectRowHandler selectRowHandler) {
        selectRowHandlers.add(selectRowHandler);
    }

    public void deselect() {
        if (table.getRowCount() > 0) {
            styleRow(selectedRowIndex, false);
            selectedRowIndex = 0;
            isRowSelected = false;
        }
    }

    private void addEventHandlers() {
        table.addClickHandler(new ClickHandler() {

            public void onClick(@Nonnull final ClickEvent event) {
                performClickAction(event);
            }
        });

        if (hotkeysEnabled) {
            focuser.addKeyDownHandler(new KeyDownHandler() {

                public void onKeyDown(@Nonnull final KeyDownEvent event) {
                    performKeyDownAction(event);
                }
            });
        }
    }

    private void performClickAction(@Nonnull final ClickEvent event) {
        Cell cell = table.getCellForEvent(event);
        if (cell != null) {
            int row = cell.getRowIndex();
            selectRow(row);
        }
    }

    private void performKeyDownAction(@Nonnull final KeyDownEvent event) {
        if (isRowSelected) {
            event.preventDefault();

            if (event.isUpArrow()) {
                selectRow(selectedRowIndex - 1);
            } else if (event.isDownArrow()) {
                selectRow(selectedRowIndex + 1);
            }

            ScrollMarker scrollMarker =
                new ScrollMarker(table.getRowFormatter().getElement(selectedRowIndex));
            scroller.ensureVisible(scrollMarker);
        }
    }

    private void selectRow(final int row) {
        if (row >= 0 && row < table.getRowCount()) {
            if (isRowSelected) {
                styleRow(selectedRowIndex, false);
            }

            selectedRowIndex = row;
            styleRow(selectedRowIndex, true);
            isRowSelected = true;

            fireSelectRowEvent();
        }
    }

    private void styleRow(final int row, final boolean selected) {
        String style = resources.css().browserTabSelectedRow();

        if (selected) {
            table.getRowFormatter().addStyleName(row, style);
        } else {
            table.getRowFormatter().removeStyleName(row, style);
        }
    }

    private void fireSelectRowEvent() {
        for (SelectRowHandler selectRowHandler : selectRowHandlers) {
            selectRowHandler.onSelectRow(selectedRowIndex);
        }
    }
    
    private void restoreRowSelection() {
        if (isRowSelected && selectedRowIndex < table.getRowCount()) {
            selectRow(selectedRowIndex);
        } else {
            isRowSelected = false;
        }
    }
    
    private void setColumnsWidth() {
        assert columnDefinitions != null;
        for (int j = 0; j < columnDefinitions.size(); j++) {
            ColumnDefinition columnDefinition = columnDefinitions.get(j);
            table.getColumnFormatter().setWidth(j, columnDefinition.getWidth());
        }
    }

    private class ScrollMarker extends UIObject {
        public ScrollMarker(Element element) {
            setElement(element);
        }
    }

    public interface SelectRowHandler {
        void onSelectRow(int row);
    }

    public interface ColumnDefinition<T> {
        String getContent(T t);
        
        String getWidth();
    }
}

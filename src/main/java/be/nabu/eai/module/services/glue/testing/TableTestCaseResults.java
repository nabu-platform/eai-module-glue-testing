/*
 * Copyright (C) 2016 Alexander Verbruggen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package be.nabu.eai.module.services.glue.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import be.nabu.glue.impl.formatted.FormattedScriptResult;
import be.nabu.glue.impl.formatted.FormattedValidation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class TableTestCaseResults {

    private static final String STYLE_ERROR = "-fx-control-inner-background: #ffd5d6;";
    private static final String STYLE_WARNING = "-fx-control-inner-background: #ffe190;";
    private static final String STYLE_OK = "-fx-control-inner-background: #ecfdc3;";

    @SuppressWarnings("unchecked")
    public static void initialize(TableView<FormattedScriptResult> tblResults) {
        for (int i = tblResults.getColumns().size() - 1; i < 8; i++) {
            tblResults.getColumns().add(new TableColumn<FormattedScriptResult, String>());
        }

        List<TableColumn<FormattedScriptResult, ?>> columns = tblResults.getColumns();

        TableColumn<FormattedScriptResult, String> statusColumn = (TableColumn<FormattedScriptResult, String>) columns.get(0);
        statusColumn.setText("Status");
        statusColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedScriptResult, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedScriptResult, String> arg0) {
                Severity severity = arg0.getValue().getSeverity();
                return new SimpleStringProperty(severity == Severity.INFO ? "PASSED" : "FAILED");
            }
        });
        statusColumn.setCellFactory(new Callback<TableColumn<FormattedScriptResult, String>, TableCell<FormattedScriptResult, String>>() {
            @Override
            public TableCell<FormattedScriptResult, String> call(TableColumn<FormattedScriptResult, String> arg0) {
                return new TableCell<FormattedScriptResult, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        TableRow<FormattedScriptResult> row = getTableRow();
                        FormattedScriptResult result = row == null ? null : row.getItem();
                        Severity severity = result == null ? null : result.getSeverity();
                        if (severity != null) {
                            if (Severity.ERROR.equals(severity) || Severity.CRITICAL.equals(severity)) {
                                row.setStyle(STYLE_ERROR);
                            }
                            else if (Severity.WARNING.equals(severity)) {
                                row.setStyle(STYLE_WARNING);
                            }
                            else {
                                row.setStyle(STYLE_OK);
                            }
                        }
                        else {
                            row.setStyle("");
                        }
                    }
                };
            }
        });

        TableColumn<FormattedScriptResult, String> scriptColumn = (TableColumn<FormattedScriptResult, String>) columns.get(1);
        scriptColumn.setText("Test Case");
        scriptColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedScriptResult, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedScriptResult, String> arg0) {
                String namespace = arg0.getValue().getNamespace();
                String name = arg0.getValue().getName();
                if (namespace == null || namespace.isEmpty()) {
                    return new SimpleStringProperty(name);
                }
                return new SimpleStringProperty(namespace + "." + name);
            }
        });
        scriptColumn.minWidthProperty().set(250);

        TableColumn<FormattedScriptResult, String> startedColumn = (TableColumn<FormattedScriptResult, String>) columns.get(2);
        startedColumn.setText("Started");
        startedColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedScriptResult, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedScriptResult, String> arg0) {
                return new SimpleStringProperty(arg0.getValue().getStarted() == null ? "" : arg0.getValue().getStarted().toString());
            }
        });
        startedColumn.minWidthProperty().set(160);

        TableColumn<FormattedScriptResult, String> stoppedColumn = (TableColumn<FormattedScriptResult, String>) columns.get(3);
        stoppedColumn.setText("Stopped");
        stoppedColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedScriptResult, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedScriptResult, String> arg0) {
                return new SimpleStringProperty(arg0.getValue().getStopped() == null ? "" : arg0.getValue().getStopped().toString());
            }
        });
        stoppedColumn.minWidthProperty().set(160);

        TableColumn<FormattedScriptResult, String> totalColumn = (TableColumn<FormattedScriptResult, String>) columns.get(4);
        totalColumn.setText("Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<FormattedScriptResult, String>("amountValidations"));
        totalColumn.minWidthProperty().set(70);

        TableColumn<FormattedScriptResult, String> okColumn = (TableColumn<FormattedScriptResult, String>) columns.get(5);
        okColumn.setText("Successful");
        okColumn.setCellValueFactory(new PropertyValueFactory<FormattedScriptResult, String>("amountSuccessful"));
        okColumn.minWidthProperty().set(90);

        TableColumn<FormattedScriptResult, String> errorColumn = (TableColumn<FormattedScriptResult, String>) columns.get(6);
        errorColumn.setText("Error");
        errorColumn.setCellValueFactory(new PropertyValueFactory<FormattedScriptResult, String>("amountError"));
        errorColumn.minWidthProperty().set(70);

        TableColumn<FormattedScriptResult, String> criticalColumn = (TableColumn<FormattedScriptResult, String>) columns.get(7);
        criticalColumn.setText("Critical");
        criticalColumn.setCellValueFactory(new PropertyValueFactory<FormattedScriptResult, String>("amountCritical"));
        criticalColumn.minWidthProperty().set(70);
    }

    @SuppressWarnings("unchecked")
    public static void initializeValidations(TableView<FormattedValidation> tblValidations) {
        for (int i = tblValidations.getColumns().size() - 1; i < 5; i++) {
            tblValidations.getColumns().add(new TableColumn<FormattedValidation, String>());
        }

        List<TableColumn<FormattedValidation, ?>> columns = tblValidations.getColumns();

        TableColumn<FormattedValidation, String> levelColumn = (TableColumn<FormattedValidation, String>) columns.get(0);
        levelColumn.setText("Status");
        levelColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedValidation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedValidation, String> arg0) {
                Severity severity = arg0.getValue().getSeverity();
                return new SimpleStringProperty(severity == Severity.INFO ? "PASSED" : "FAILED");
            }
        });
        levelColumn.setCellFactory(new Callback<TableColumn<FormattedValidation, String>, TableCell<FormattedValidation, String>>() {
            @Override
            public TableCell<FormattedValidation, String> call(TableColumn<FormattedValidation, String> arg0) {
                return new TableCell<FormattedValidation, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        TableRow<FormattedValidation> row = getTableRow();
                        FormattedValidation validation = row == null ? null : row.getItem();
                        Severity severity = validation == null ? null : validation.getSeverity();
                        if (severity != null) {
                            if (Severity.ERROR.equals(severity) || Severity.CRITICAL.equals(severity)) {
                                row.setStyle(STYLE_ERROR);
                            }
                            else if (Severity.WARNING.equals(severity)) {
                                row.setStyle(STYLE_WARNING);
                            }
                            else {
                                row.setStyle(STYLE_OK);
                            }
                        }
                        else {
                            row.setStyle("");
                        }
                    }
                };
            }
        });

        TableColumn<FormattedValidation, String> messageColumn = (TableColumn<FormattedValidation, String>) columns.get(1);
        messageColumn.setText("Description");
        messageColumn.setCellValueFactory(new PropertyValueFactory<FormattedValidation, String>("message"));
        messageColumn.minWidthProperty().set(150);

        TableColumn<FormattedValidation, String> checkColumn = (TableColumn<FormattedValidation, String>) columns.get(2);
        checkColumn.setText("Validation");
        checkColumn.setCellValueFactory(new PropertyValueFactory<FormattedValidation, String>("validation"));
        checkColumn.minWidthProperty().set(250);

        TableColumn<FormattedValidation, String> scriptColumn = (TableColumn<FormattedValidation, String>) columns.get(3);
        scriptColumn.setText("Location");
        scriptColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedValidation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedValidation, String> arg0) {
                List<String> callStack = new ArrayList<String>(arg0.getValue().getCallStack() == null ? Collections.emptyList() : arg0.getValue().getCallStack());
                Collections.reverse(callStack);
                StringBuilder builder = new StringBuilder();
                for (String item : callStack) {
                    if (!builder.toString().isEmpty()) {
                        builder.append(" > ");
                    }
                    builder.append(item);
                }
                return new SimpleStringProperty(builder.toString());
            }
        });
        scriptColumn.minWidthProperty().set(0);

        TableColumn<FormattedValidation, String> lineColumn = (TableColumn<FormattedValidation, String>) columns.get(4);
        lineColumn.setText("Line");
        lineColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FormattedValidation, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<FormattedValidation, String> arg0) {
                int line = arg0.getValue().getLineNumber();
                return new SimpleStringProperty(line < 0 ? "" : "" + (line + 1));
            }
        });
    }

    public static SplitPane build(TableView<FormattedScriptResult> tblResults) {
        initialize(tblResults);

        TableView<FormattedValidation> tblValidations = new TableView<FormattedValidation>();
        initializeValidations(tblValidations);

        TextArea txtLog = new TextArea();
        txtLog.setEditable(false);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        Tab logTab = new Tab("Log");
        logTab.setContent(txtLog);
        Tab validationsTab = new Tab("Validations");
        validationsTab.setContent(tblValidations);
        tabs.getTabs().addAll(logTab, validationsTab);

        SplitPane split = new SplitPane();
        split.getItems().add(tblResults);

        tblResults.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<FormattedScriptResult>() {
            @Override
            public void changed(ObservableValue<? extends FormattedScriptResult> observable, FormattedScriptResult oldValue, FormattedScriptResult newValue) {
                if (newValue == null) {
                    txtLog.clear();
                    tblValidations.getItems().clear();
                    split.getItems().remove(tabs);
                }
                else {
                    txtLog.setText(newValue.getLog() == null ? "" : newValue.getLog());
                    List<FormattedValidation> validations = newValue.getValidations() == null ? Collections.emptyList() : newValue.getValidations();
                    tblValidations.getItems().setAll(validations);
                    if (!split.getItems().contains(tabs)) {
                        split.getItems().add(tabs);
                        split.setDividerPositions(0.5d);
                    }
                }
            }
        });

        return split;
    }
}

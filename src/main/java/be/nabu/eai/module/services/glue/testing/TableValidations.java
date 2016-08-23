package be.nabu.eai.module.services.glue.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class TableValidations {
	
	@SuppressWarnings("unchecked")
	public static void initialize(TableView<Validation<?>> tblValidations) {
		for (int i = tblValidations.getColumns().size() - 1; i < 5; i++) {
			tblValidations.getColumns().add(new TableColumn<Validation<?>, String>());
		}
		
		List<TableColumn<Validation<?>, ?>> columns = tblValidations.getColumns();

		TableColumn<Validation<?>, String> levelColumn = (TableColumn<Validation<?>, String>) columns.get(0);
		levelColumn.setText("Status");
		levelColumn.setCellValueFactory(
			new Callback<TableColumn.CellDataFeatures<Validation<?>,String>, ObservableValue<String>>() {
				@Override
				public ObservableValue<String> call(CellDataFeatures<Validation<?>, String> arg0) {
					return new SimpleStringProperty(arg0.getValue().getSeverity() == Severity.INFO ? "PASSED" : "FAILED");
				}
			}
		);
		levelColumn.setCellFactory(new Callback<TableColumn<Validation<?>, String>, TableCell<Validation<?>, String>>() {
			@Override
			public TableCell<Validation<?>, String> call(TableColumn<Validation<?>, String> arg0) {
				return new TableCell<Validation<?>, String>() {
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						setText(item);
						TableRow<Validation<?>> row = getTableRow();
						if (item != null) {
							if (Severity.ERROR.name().equals(item) || Severity.CRITICAL.name().equals(item)) {
								row.setStyle("-fx-control-inner-background: #ffd5d6;");
							}
							else if (Severity.WARNING.name().equals(item)) {
								row.setStyle("-fx-control-inner-background: #ffe190;");
							}
							else {
								row.setStyle("-fx-control-inner-background: #ecfdc3;");
							}
						}
						else {
							row.setStyle("");
						}
					}
				};
			}
		});
		
		TableColumn<Validation<?>, String> messageColumn = (TableColumn<Validation<?>, String>) columns.get(1);
		messageColumn.setText("Description");
		messageColumn.setCellValueFactory(
		    new PropertyValueFactory<Validation<?>, String>("description")
		);
		messageColumn.minWidthProperty().set(350);
		
		TableColumn<Validation<?>, String> checkColumn = (TableColumn<Validation<?>, String>) columns.get(2);
		checkColumn.setText("Validation");
		checkColumn.setCellValueFactory(
		    new PropertyValueFactory<Validation<?>, String>("message")
		);
		checkColumn.minWidthProperty().set(450);
		
		TableColumn<Validation<?>, String> scriptColumn = (TableColumn<Validation<?>, String>) columns.get(3);
		scriptColumn.setText("Location");
		scriptColumn.setCellValueFactory(
		    new Callback<TableColumn.CellDataFeatures<Validation<?>,String>, ObservableValue<String>>() {
				@SuppressWarnings("rawtypes")
				@Override
				public ObservableValue<String> call(CellDataFeatures<Validation<?>, String> arg0) {
					StringBuilder builder = new StringBuilder();
					List<?> callStack = new ArrayList(arg0.getValue().getContext());
					Collections.reverse(callStack);
					for (Object item : callStack) {
						if (!builder.toString().isEmpty()) {
							builder.append(" > ");
						}
						builder.append(item.toString());
					}
					return new SimpleStringProperty(builder.toString());
				}
			}
		);
		scriptColumn.minWidthProperty().set(200);
		
		TableColumn<Validation<?>, String> lineColumn = (TableColumn<Validation<?>, String>) columns.get(4);
		lineColumn.setText("Line");
		lineColumn.setCellValueFactory(
		    new Callback<TableColumn.CellDataFeatures<Validation<?>,String>, ObservableValue<String>>() {
				@Override
				public ObservableValue<String> call(CellDataFeatures<Validation<?>, String> arg0) {
					return arg0.getValue() instanceof GlueValidation && ((GlueValidation) arg0.getValue()).getExecutor() != null && ((GlueValidation) arg0.getValue()).getExecutor().getContext() != null
						? new SimpleStringProperty("" + (((GlueValidation) arg0.getValue()).getExecutor().getContext().getLineNumber() + 1))
						: new SimpleStringProperty("");
				}
			}
		);
	}
}

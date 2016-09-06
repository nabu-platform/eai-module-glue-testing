package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.GlueServiceGUIManager;
import be.nabu.eai.module.services.glue.testing.CustomFormatter.Handler;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.glue.ScriptRuntime;
import be.nabu.glue.ScriptUtils;
import be.nabu.glue.api.ExecutionEnvironment;
import be.nabu.glue.api.ParameterDescription;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatters.MarkdownOutputFormatter;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

// TODO: default service run uses local service runtime instead of an actual runner, this should probably be updated
public class GlueTestServiceGUIManager extends GlueServiceGUIManager {

	private ComboBox<String> environments;
	private Button start, stop;
	private BooleanProperty running = new SimpleBooleanProperty(false);
	private TableView<Validation<?>> validations = new TableView<Validation<?>>();
	private ScriptRuntime runtime;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GlueTestServiceGUIManager() {
		super("Test Case", (Class) GlueTestServiceArtifact.class, new GlueTestServiceManager());
		TableValidations.initialize(validations);
	}
	
	@Override
	public void display(MainController controller, AnchorPane pane, GlueServiceArtifact artifact) throws IOException, ParseException {
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.VERTICAL);
		final AceEditor ace = getEditor(artifact);
		ace.setLiveAutocompletion(false);
		
		addAutocomplete(artifact, ace);
		
		TabPane tabs = new TabPane();
		tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		initializeRunner(tabs, artifact);
		
		Tab tabResources = new Tab("Resources");
		tabResources.setContent(getResources(controller, artifact));
		
		Tab tabIface = new Tab("Interface");
		tabIface.setContent(getIface(controller, artifact));
		tabs.getTabs().addAll(tabResources, tabIface);

		split.getItems().addAll(ace.getWebView(), tabs);
		pane.getChildren().add(split);
		
		pane.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (start != null && event.getCode() == KeyCode.R && event.isControlDown() && event.isShiftDown() && !event.isAltDown()) {
					start.fire();
				}
			}
		});
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
	}

	private void initializeRunner(TabPane tabs, final GlueServiceArtifact artifact) throws IOException {
		VBox runner = new VBox();
		HBox buttons = new HBox();
		start = new Button("Start");
		stop = new Button("Stop");
		environments = new ComboBox<String>();
		start.disableProperty().bind(running);
		stop.disableProperty().bind(running.not());
		environments.disableProperty().bind(running);
		environments.getItems().addAll(SimpleExecutionEnvironment.getEnvironments());
		final TextArea txtLog = new TextArea();
		if (!environments.getItems().contains("local")) {
			environments.getItems().add("local");
		}
		environments.getSelectionModel().select("local");
		start.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				txtLog.clear();
				ExecutionEnvironment environment;
				try {
					environment = new SimpleExecutionEnvironment(MainController.getInstance().getServer().getName());
				}
				catch (Exception e) {
					txtLog.setText("Can not find environment");
					throw new RuntimeException(e);
				}
				Map<String, Object> input = new HashMap<String, Object>();
				Script script = artifact.getScript();
				try {
					List<ParameterDescription> parameters = ScriptUtils.getInputs(script);
					if (parameters == null || parameters.isEmpty()) {
						run(txtLog, environment, input, script);
					}
					else {
						Set<Property<?>> properties = new LinkedHashSet<Property<?>>();
						for (ParameterDescription parameter : parameters) {
							properties.add(new SimpleProperty<String>(parameter.getName(), String.class, false));
						}
						final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, properties);
						EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Run Test Case", new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								for (ParameterDescription parameter : parameters) {
									String value = updater.getValue(parameter.getName());
									if (value != null && !value.isEmpty()) {
										input.put(parameter.getName(), value);
									}
								}
								run(txtLog, environment, input, script);
							}
						});
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			private void run(final TextArea txtLog, ExecutionEnvironment environment, Map<String, Object> input, Script script) {
				runtime = new ScriptRuntime(
					script, 
					environment, 
					false, 
					input
				);
				runtime.setFormatter(new CustomFormatter(new MarkdownOutputFormatter(new TextAreaWriter(txtLog)), new Handler() {
					@Override
					public void start() {
						running.set(true);
						validations.getItems().clear();
					}
					@Override
					public void validate(GlueValidation...glueValidations) {
						validations.getItems().addAll(glueValidations);
					}
					@Override
					public void stop(Severity state) {
						running.set(false);
					}
				}));
				
				Thread runThread = new Thread(runtime);
				runThread.setDaemon(true);
				runThread.start();
			}
		});
		stop.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (runtime != null) {
					runtime.abort();
				}
			}
		});
		
		buttons.getChildren().addAll(start, stop);

		runner.getChildren().addAll(buttons, txtLog);
		VBox.setVgrow(txtLog, Priority.ALWAYS);
		Tab runTab = new Tab("Run");
		runTab.setContent(runner);
		tabs.getTabs().add(runTab);
		
		Tab validationsTab = new Tab("Validations");
		validationsTab.setContent(validations);
		tabs.getTabs().add(validationsTab);
	}

	@Override
	public String getCategory() {
		return "Testing";
	}

	@Override
	protected GlueServiceArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new GlueTestServiceArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}
	
	public ReadOnlyBooleanProperty runningProperty() {
		return running;
	}
}

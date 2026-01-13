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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.ComplexContentEditor;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils.PropertiesHandler;
import be.nabu.eai.developer.util.EAIDeveloperUtils.PropertyUpdaterListener;
import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.GlueServiceGUIManager;
import be.nabu.eai.module.services.glue.testing.CustomFormatter.Handler;
import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceConfiguration;
import be.nabu.eai.module.services.glue.testing.RunProfileConfiguration.ServiceProfile;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.glue.api.ExecutionEnvironment;
import be.nabu.glue.api.LabelEvaluator;
import be.nabu.glue.api.ParameterDescription;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.core.impl.executors.EvaluateExecutor;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatters.MarkdownOutputFormatter;
import be.nabu.glue.services.CombinedExecutionContext;
import be.nabu.glue.services.CombinedExecutionContextImpl;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.glue.utils.ScriptUtils;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.FeaturedExecutionContext;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
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
		
		Tab tabStubs = new Tab("Stubs");
		tabStubs.setContent(displayRunProfile(artifact.getId(), ((GlueTestServiceArtifact) artifact).getRunProfile()));
		
		Tab tabIface = new Tab("Interface");
		tabIface.setContent(getIface(controller, artifact));
		tabs.getTabs().addAll(tabResources, tabStubs, tabIface);

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
	
	private CombinedExecutionContext getGlueExecutionContext(GlueServiceArtifact artifact) {
		ExecutionEnvironment executionEnvironment = new ExecutionEnvironment() {
			@Override
			public Map<String, String> getParameters() {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(EvaluateExecutor.DEFAULT_VARIABLE_NAME_PARAMETER, "$result");
				return map;
			}
			@Override
			public String getName() {
				return "default";
			}
		};
		ExecutionContext context = EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT);
		if (context instanceof FeaturedExecutionContext) {
			((FeaturedExecutionContext) context).getEnabledFeatures().add(artifact.getId());
		}
		CombinedExecutionContext combinedExecutionContext = new CombinedExecutionContextImpl(context, executionEnvironment, new LabelEvaluator() {
			@Override
			public boolean shouldExecute(String label, ExecutionEnvironment environment) {
				return true;
			}
		});
		return combinedExecutionContext;
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
						run(artifact, txtLog, environment, input, script);
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
								run(artifact, txtLog, environment, input, script);
							}
						});
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			private void run(GlueServiceArtifact artifact, final TextArea txtLog, ExecutionEnvironment environment, Map<String, Object> input, Script script) {
//				runtime = new ScriptRuntime(
//					script, 
//					environment, 
//					false, 
//					input
//				);
				runtime = new ScriptRuntime(
					script, 
					getGlueExecutionContext(artifact), 
					input
				);
				// not using the dynamic cache for now
//				((GlueTestServiceArtifact) artifact).setDynamicCacheProvider(new DynamicCacheProvider(runtime, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT)));
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
	
	protected Pane displayRunProfile(String id, RunProfileConfiguration configuration) {
		AnchorPane pane = new AnchorPane();
		displayRunProfile(id, configuration, pane);
		return pane;
	}
	
	protected void displayRunProfile(String id, RunProfileConfiguration instance, Pane pane) {
		VBox box = new VBox();
		
		BooleanProperty hasLock = MainController.getInstance().hasLock(id);
		
		// buttons at the top to add service configurations
		HBox buttons = new HBox();
		buttons.disableProperty().bind(hasLock.not());
		
		buttons.setPadding(new Insets(10));
		
		// then an anchor pane with one entry per service
		// within each anchor pane first a list of queries
		// then an error code + message
		// then a complex editor for the output
		// then finally buttons (to remove mostly)
		Accordion accordion = new Accordion();
		box.getChildren().addAll(buttons, accordion);
		
		// draw the existing profiles
		if (instance.getProfiles() != null) {
			for (ServiceProfile profile : instance.getProfiles()) {
				drawServiceProfile(id, instance, accordion, profile, false);
			}
		}
		
		Button create = new Button("Add Service");
		create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				SimpleProperty<DefinedService> property = new SimpleProperty<DefinedService>("Service", DefinedService.class, true);
				
				EAIDeveloperUtils.buildPopup(MainController.getInstance(), "Add Service", Arrays.asList(property), new PropertiesHandler() {
					@Override
					public void handle(SimplePropertyUpdater updater) {
						DefinedService service = updater.getValue("Service");
						if (service != null) {
							// the same service can only be added once
							if (instance.getProfiles() == null) {
								instance.setProfiles(new ArrayList<ServiceProfile>());
							}
							boolean found = false;
							for (ServiceProfile profile : instance.getProfiles()) {
								if (profile.getService().getId().equals(service.getId())) {
									found = true;
									break;
								}
							}
							if (!found) {
								ServiceProfile serviceProfile = new ServiceProfile();
								serviceProfile.setService(service);
								instance.getProfiles().add(serviceProfile);
								drawServiceProfile(id, instance, accordion, serviceProfile, true);
								MainController.getInstance().setChanged();
							}
						}
					}
				}, false, MainController.getInstance().getActiveStage());
			}
		});
		buttons.getChildren().add(create);
		
		pane.getChildren().add(box);
		AnchorPane.setBottomAnchor(box, 0d);
		AnchorPane.setTopAnchor(box, 0d);
		AnchorPane.setRightAnchor(box, 0d);
		AnchorPane.setLeftAnchor(box, 0d);
	}
	
	
	protected void drawServiceProfile(String id, RunProfileConfiguration runProfile, Accordion accordion, ServiceProfile profile, boolean open) {
		BooleanProperty hasLock = MainController.getInstance().hasLock(id);
		
		AnchorPane pane = new AnchorPane();
		pane.getStyleClass().add("service-profile-pane");
		TitledPane titledPane = new TitledPane(profile.getService().getId(), pane);
		accordion.getPanes().add(titledPane);
		if (open) {
			accordion.setExpandedPane(titledPane);
		}
		VBox box = new VBox();
		
		pane.getChildren().add(box);
		AnchorPane.setBottomAnchor(box, 0d);
		AnchorPane.setTopAnchor(box, 0d);
		AnchorPane.setRightAnchor(box, 0d);
		AnchorPane.setLeftAnchor(box, 0d);
		
		HBox buttons = new HBox();
		buttons.disableProperty().bind(hasLock.not());
		
		buttons.setPadding(new Insets(10));
		box.getChildren().add(buttons);
		
		Button create = new Button("Add Configuration");
		create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (profile.getConfigurations() == null) {
					profile.setConfigurations(new ArrayList<ServiceConfiguration>());
				}
				ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
				profile.getConfigurations().add(serviceConfiguration);
				drawServiceConfiguration(id, runProfile, profile, box, serviceConfiguration);
				MainController.getInstance().setChanged();
			}
		});
		buttons.getChildren().add(create);
		
		Button delete = new Button("Delete All");
		delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Confirm.confirm(ConfirmType.QUESTION, "Delete All Configurations", "Are you sure you want to delete all configurations for this service?", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						runProfile.getProfiles().remove(profile);
						accordion.getPanes().remove(titledPane);
						MainController.getInstance().setChanged();
					}
				});
			}
		});
		buttons.getChildren().add(delete);
		
		if (profile.getConfigurations() != null) {
			for (ServiceConfiguration configuration : profile.getConfigurations()) {
				drawServiceConfiguration(id, runProfile, profile, box, configuration);
			}
		}
	}
	
	protected void drawServiceConfiguration(String id, RunProfileConfiguration runProfile, ServiceProfile profile, VBox parent, ServiceConfiguration configuration) {
		BooleanProperty hasLock = MainController.getInstance().hasLock(id);
		SimplePropertyUpdater updater = EAIDeveloperUtils.createUpdater(configuration, new PropertyUpdaterListener() {
			@Override
			public boolean updateProperty(Property<?> property, Object value) {
				MainController.getInstance().setChanged();
				return true;
			}
		}, "output");
		updater.setSourceId(id);
		VBox profileInstanceBox = new VBox();
		
		VBox inputQueryContainer = new VBox();
		VBox box = new VBox();
		box.setPadding(new Insets(10));
		MainController.getInstance().showProperties(updater, box, false);
		
		HBox buttons = new HBox();
		setComplexContent(runProfile, profile, configuration, box, null, buttons, "application/json");
		
//		box.getChildren().add(formats);
		
		buttons.disableProperty().bind(hasLock.not());
		buttons.setPadding(new Insets(10));
		Button delete = new Button("Delete Configuration");
		delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Confirm.confirm(ConfirmType.QUESTION, "Delete Configuration", "Are you sure you want to delete this configuration?", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						profile.getConfigurations().remove(configuration);
						parent.getChildren().remove(profileInstanceBox);
						MainController.getInstance().setChanged();
					}
				});
			}
		});
		Button addCondition = new Button("Add Condition");
		addCondition.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (configuration.getInputQueries() == null) {
					configuration.setInputQueries(new ArrayList<String>());
				}
				configuration.getInputQueries().add("true");
				MainController.getInstance().setChanged();
				drawInputQueries(inputQueryContainer, configuration);
			}
		});
		drawInputQueries(inputQueryContainer, configuration);
		buttons.getChildren().addAll(delete, addCondition);
		profileInstanceBox.getChildren().addAll(inputQueryContainer, box, buttons);
		parent.getChildren().addAll(profileInstanceBox);
	}
	
	private void drawInputQueries(VBox container, ServiceConfiguration configuration) {
		container.getChildren().clear();
		if (configuration.getInputQueries() != null) {
			for (int i = 0; i < configuration.getInputQueries().size(); i++) {
				String query = configuration.getInputQueries().get(i);
				HBox box = new HBox();
				TextField field = new TextField();
				Button button = new Button("Remove");
				final int pos = i;
				button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						configuration.getInputQueries().remove(pos);
						drawInputQueries(container, configuration);
						MainController.getInstance().setChanged();
					}
				});
				field.setText(query);
				field.textProperty().addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
						configuration.getInputQueries().set(pos, arg2);
						MainController.getInstance().setChanged();
					}
				});
				box.setPadding(new Insets(10));
				HBox.setMargin(button, new Insets(0, 0, 10, 0));
				box.getChildren().addAll(field, button);
				container.getChildren().add(box);
			}
		}
	}

	private void setComplexContent(RunProfileConfiguration runProfile, ServiceProfile profile, ServiceConfiguration configuration, VBox box, String newContent, HBox buttons, String contentType) {
		ComplexType outputDefinition = profile.getService().getServiceInterface().getOutputDefinition();
//		XMLBinding binding = new XMLBinding(outputDefinition, Charset.forName("UTF-8"));
//		binding.setIgnoreUndefined(true);
		JSONBinding binding = new JSONBinding(outputDefinition, Charset.forName("UTF-8"));
		binding.setPrettyPrint(true);
		binding.setIgnoreUnknownElements(true);
		
		ComplexContent content;
		if (configuration.getOutput() != null && !configuration.getOutput().trim().isEmpty()) {
			try {
				content = binding.unmarshal(new ByteArrayInputStream((newContent == null ? configuration.getOutput() : newContent).getBytes(Charset.forName("UTF-8"))), new Window[0]);
				if (newContent != null) {
					configuration.setOutput(newContent);
					MainController.getInstance().setChanged();
				}
			}
			catch (Exception e) {
				// if we received new content, we failed to parse it, it is invalid, back to json with you!
				// this is not very clean code but it should work...
				if (newContent != null) {
					setStringContent(runProfile, profile, configuration, box, newContent, buttons, contentType);
					return;
				}
				content = outputDefinition.newInstance();
				MainController.getInstance().notify(e);
			}
		}
		else {
			content = outputDefinition.newInstance();
		}
		final ComplexContent finalContent = content;
		ComplexContentEditor complexContentEditor = new ComplexContentEditor(content, true, EAIResourceRepository.getInstance()) {
			@Override
			public void update() {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try {
					binding.marshal(output, finalContent);
					configuration.setOutput(new String(output.toByteArray(), Charset.forName("UTF-8")));
					MainController.getInstance().setChanged();
				}
				catch (IOException e) {
					MainController.getInstance().notify(e);
				}
				super.update();
			}
		};
		box.getChildren().clear();
		box.getChildren().add(complexContentEditor.getTree());
		
		Button button = new Button("Edit as JSON");
		button.setId("asJson");
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				String content = null;
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try {
					binding.marshal(output, finalContent);
					content = new String(output.toByteArray(), Charset.forName("UTF-8"));
					MainController.getInstance().setChanged();
				}
				catch (IOException e) {
					MainController.getInstance().notify(e);
				}
				setStringContent(runProfile, profile, configuration, box, content, buttons, "application/json");
			}
		});
		Node lookup = buttons.lookup("#asContent");
		if (lookup != null) {
			buttons.getChildren().remove(lookup);
		}
		buttons.getChildren().add(0, button);
		
		// need to manually subtract the padding
		complexContentEditor.getTree().prefWidthProperty().bind(box.widthProperty().subtract(20));
	}
	
	private void setStringContent(RunProfileConfiguration runProfile, ServiceProfile profile, ServiceConfiguration configuration, VBox box, String content, HBox buttons, String contentType) {
		AceEditor editor = new AceEditor();
		editor.setContent(contentType, content == null ? configuration.getOutput() : content);
		editor.setKeyCombination("commit", new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN));
		editor.subscribe("commit", new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				setComplexContent(runProfile, profile, configuration, box, editor.getContent(), buttons, contentType);
			}
		});
		box.getChildren().clear();
		box.getChildren().addAll(editor.getWebView());

		Button button = new Button("Push to content");
		button.setId("asContent");
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				setComplexContent(runProfile, profile, configuration, box, editor.getContent(), buttons, contentType);
			}
		});
		Node lookup = buttons.lookup("#asJson");
		if (lookup != null) {
			buttons.getChildren().remove(lookup);
		}
		// you can get redirect back here very fast, we don't want to the button added multiple times
		lookup = buttons.lookup("#asContent");
		if (lookup != null) {
			buttons.getChildren().remove(lookup);
		}
		buttons.getChildren().add(0, button);
	}
}

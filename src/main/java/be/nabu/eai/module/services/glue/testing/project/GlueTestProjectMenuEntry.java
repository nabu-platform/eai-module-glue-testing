package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.module.services.glue.testing.GlueTestServiceArtifact;
import be.nabu.eai.module.services.glue.testing.TableTestCaseResults;
import be.nabu.eai.module.services.glue.testing.project.GlueTestProjectArtifact.GlueTestProjectOutput;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.glue.impl.ScriptResultListener;
import be.nabu.glue.impl.formatted.FormattedScriptResult;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

public class GlueTestProjectMenuEntry implements EntryContextMenuProvider {

	private static int counter = 1;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public MenuItem getContext(Entry entry) {
		boolean isTestCase = entry.isNode() && GlueTestServiceArtifact.class.isAssignableFrom(entry.getNode().getArtifactClass());
		boolean isContainer = !entry.isLeaf();
		if (isTestCase || isContainer) {
			Menu menu = new Menu("Testing");
			MenuItem runTests = new MenuItem("Run tests");
			runTests.addEventHandler(ActionEvent.ANY, event -> {
				try {
					EAIResourceRepository repository = EAIResourceRepository.getInstance();
					List<GlueTestServiceArtifact> tests = repository.getArtifacts(GlueTestServiceArtifact.class);
					tests = GlueTestREST.filter(tests, entry.getId());
					logger.info("Found " + tests.size() + " testcases");
					List list = tests;
					AtomicBoolean hasError = new AtomicBoolean(false);
					Tab tab = MainController.getInstance().newTab("Testrun " + counter++);
					tab.setClosable(false);
					ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
					progressIndicator.setPrefSize(16, 16);
					Circle doneIndicator = new Circle(6);
					doneIndicator.setVisible(false);
					StackPane progressGraphic = new StackPane(progressIndicator, doneIndicator);
					tab.setGraphic(progressGraphic);
					TableView<FormattedScriptResult> results = new TableView<FormattedScriptResult>();
					tab.setContent(TableTestCaseResults.build(results));
					ScriptResultListener listener = result -> {
						FormattedScriptResult formatted = result instanceof FormattedScriptResult ? (FormattedScriptResult) result : FormattedScriptResult.format(result, null);
						logger.info(formatted.getNamespace() + "." + formatted.getName() + " => " + formatted.getAmountSuccessful() + " / " + formatted.getAmountValidations());
						Severity severity = formatted.getSeverity();
						boolean isError = Severity.ERROR.equals(severity) || Severity.CRITICAL.equals(severity);
						hasError.compareAndSet(false, isError);
						Platform.runLater(() -> {
							results.getItems().add(formatted);
							if (hasError.get()) {
								progressIndicator.setStyle("-fx-progress-color: #d9534f;");
							}
						});
					};
					ForkJoinPool.commonPool().submit(() -> {
						try {
							GlueTestREST.runAll(repository, list, listener);
						}
						catch (Exception e) {
							logger.error("Could not run all tests", e);
						}
						finally {
							Platform.runLater(() -> {
								progressIndicator.setVisible(false);
								doneIndicator.setVisible(true);
								doneIndicator.setStyle(hasError.get() ? "-fx-fill: #d9534f;" : "-fx-fill: #5cb85c;");
								tab.setClosable(true);
							});
						}
					});
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			});
			menu.getItems().add(runTests);
			return menu;
		}
		return null;
	}

}

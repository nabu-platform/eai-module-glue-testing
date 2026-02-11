package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;

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
					Tab tab = MainController.getInstance().newTab("Testrun " + counter++);
					TableView<FormattedScriptResult> results = new TableView<FormattedScriptResult>();
					tab.setContent(TableTestCaseResults.build(results));
					ScriptResultListener listener = result -> {
						FormattedScriptResult formatted = result instanceof FormattedScriptResult ? (FormattedScriptResult) result : FormattedScriptResult.format(result, null);
						logger.info(formatted.getNamespace() + "." + formatted.getName() + " => " + formatted.getAmountSuccessful() + " / " + formatted.getAmountValidations());
						Platform.runLater(() -> results.getItems().add(formatted));
					};
					ForkJoinPool.commonPool().submit(() -> {
						try {
							GlueTestREST.runAll(repository, list, listener);
						}
						catch (Exception e) {
							logger.error("Could not run all tests", e);
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

package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;

import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.module.services.glue.testing.GlueTestServiceArtifact;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.services.api.ServiceException;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class GlueTestProjectMenuEntry implements EntryContextMenuProvider {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public MenuItem getContext(Entry entry) {
		boolean isTestCase = entry.isNode() && GlueTestServiceArtifact.class.isAssignableFrom(entry.getNode().getArtifactClass());
		boolean isContainer = !entry.isLeaf();
		if (isTestCase || isContainer) {
			Menu menu = new Menu("Testing");
			MenuItem runTests = new MenuItem("Run tests");
			runTests.addEventHandler(ActionEvent.ANY, event -> {
				EAIResourceRepository repository = EAIResourceRepository.getInstance();
				List<GlueTestServiceArtifact> tests = repository.getArtifacts(GlueTestServiceArtifact.class);
				tests = GlueTestREST.filter(tests, entry.getId());
				List list = tests;
				try {
					GlueTestREST.runAll(repository, list, result -> {
						
					});
				}
				catch (ServiceException e) {
					e.printStackTrace();
				}
			});
			menu.getItems().add(runTests);
			return menu;
		}
		return null;
	}

}

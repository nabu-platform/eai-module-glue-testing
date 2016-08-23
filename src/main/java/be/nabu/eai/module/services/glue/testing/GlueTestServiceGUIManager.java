package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;
import java.text.ParseException;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.GlueServiceGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.libs.property.api.Value;

public class GlueTestServiceGUIManager extends GlueServiceGUIManager {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public GlueTestServiceGUIManager() {
		super("Test Case", (Class) GlueTestServiceArtifact.class, new GlueTestServiceManager());
	}
	
	@Override
	public void display(MainController controller, AnchorPane pane, GlueServiceArtifact artifact) throws IOException, ParseException {
		SplitPane split = new SplitPane();
		split.setOrientation(Orientation.VERTICAL);
		final AceEditor ace = getEditor(artifact);
		
		TabPane tabs = new TabPane();
		Tab tab = new Tab("Interface");
		tab.setContent(getIface(controller, artifact));
		tabs.getTabs().add(tab);

		split.getItems().addAll(ace.getWebView(), tabs);
		pane.getChildren().add(split);
		
		AnchorPane.setBottomAnchor(split, 0d);
		AnchorPane.setTopAnchor(split, 0d);
		AnchorPane.setLeftAnchor(split, 0d);
		AnchorPane.setRightAnchor(split, 0d);
	}

	@Override
	public String getCategory() {
		return "Testing";
	}

	@Override
	protected GlueServiceArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>...values) throws IOException {
		return new GlueTestServiceArtifact(entry.getId(), entry.getRepository());
	}
}

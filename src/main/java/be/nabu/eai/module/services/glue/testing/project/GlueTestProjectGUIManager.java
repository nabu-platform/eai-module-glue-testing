package be.nabu.eai.module.services.glue.testing.project;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class GlueTestProjectGUIManager extends BaseJAXBGUIManager<GlueTestProjectConfiguration, GlueTestProjectArtifact> {

	public GlueTestProjectGUIManager() {
		super("Test Project", GlueTestProjectArtifact.class, new GlueTestProjectManager(), GlueTestProjectConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected GlueTestProjectArtifact newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new GlueTestProjectArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	public String getCategory() {
		return "Testing";
	}
}

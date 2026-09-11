package be.nabu.eai.module.services.glue.testing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.GlueServiceArtifactFragmentManager;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;

public class GlueTestServiceArtifactFragmentManager extends GlueServiceArtifactFragmentManager {

	private static final String STUB_PATH = "stub.xml";
	private static final String CONTENT_TYPE = "application/xml";
	private static final String FRAGMENT_TYPE = "stubs";
	private static final String ARTIFACT_TYPE = "glueTestService";
	private static final String GUIDELINES_PATH = "/guidelines/glue-test-service.md";

	@Override
	public Entry createArtifact(Entry parent, String name) {
		try {
			RepositoryEntry entry = ((RepositoryEntry) parent).createNode(name, new GlueTestServiceManager(), true);
			GlueTestServiceArtifact artifact = new GlueTestServiceArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
			artifact.setContent("");
			artifact.setRunProfile(new RunProfileConfiguration());
			new GlueTestServiceManager().save(entry, artifact);
			return entry;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ArtifactFragment> listFragments(GlueServiceArtifact artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>(super.listFragments(artifact));
		if (artifact instanceof GlueTestServiceArtifact) {
			Entry entry = EAIResourceRepository.getInstance().getEntry(artifact.getId());
			fragments.add(new StubFragment((GlueTestServiceArtifact) artifact, entry instanceof ResourceEntry && entry.isEditable()));
		}
		return fragments;
	}

	@Override
	public List<Validation<?>> updateFragment(GlueServiceArtifact artifact, String path, String oldContent, String newContent) {
		if (!STUB_PATH.equals(path)) {
			return super.updateFragment(artifact, path, oldContent, newContent);
		}
		List<Validation<?>> validations = new ArrayList<Validation<?>>();
		if (!(artifact instanceof GlueTestServiceArtifact)) {
			validations.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Stubs are only supported for Glue test services"));
			return validations;
		}
		Entry entry = EAIResourceRepository.getInstance().getEntry(artifact.getId());
		if (!(entry instanceof ResourceEntry) || !entry.isEditable()) {
			validations.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Updating stubs requires an editable resource-backed Glue test service"));
			return validations;
		}
		try {
			GlueTestServiceArtifact candidate = (GlueTestServiceArtifact) newArtifact((ResourceEntry) entry);
			candidate.setContent(artifact.getContent());
			candidate.setRunProfile(unmarshal(candidate, newContent));
			validations.addAll(saveArtifact((ResourceEntry) entry, candidate));
			if (!hasErrors(validations)) {
				((GlueTestServiceArtifact) artifact).setRunProfile(candidate.getRunProfile());
			}
		}
		catch (Exception e) {
			validations.add(new ValidationMessage(ValidationMessage.Severity.ERROR, e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
		}
		return validations;
	}

	@Override
	protected GlueServiceArtifact newArtifact(ResourceEntry entry) throws Exception {
		return new GlueTestServiceArtifact(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	@Override
	protected List<Validation<?>> saveArtifact(ResourceEntry entry, GlueServiceArtifact artifact) throws Exception {
		return new GlueTestServiceManager().save(entry, artifact);
	}

	@Override
	protected void copyAdditionalState(GlueServiceArtifact source, GlueServiceArtifact target) {
		if (source instanceof GlueTestServiceArtifact && target instanceof GlueTestServiceArtifact) {
			((GlueTestServiceArtifact) target).setRunProfile(((GlueTestServiceArtifact) source).getRunProfile());
		}
	}

	@Override
	public String getGuidelines(List<String> fragmentTypes) {
		List<String> sections = new ArrayList<String>();
		String shared = super.getGuidelines(fragmentTypes);
		if (shared != null && !shared.trim().isEmpty()) {
			sections.add(shared.trim());
		}
		if (fragmentTypes == null || fragmentTypes.isEmpty() || fragmentTypes.contains(ARTIFACT_TYPE) || fragmentTypes.contains(FRAGMENT_TYPE) || fragmentTypes.contains(STUB_PATH)) {
			String guidelines = EAIRepositoryUtils.loadCachedClasspathResource(GlueTestServiceArtifactFragmentManager.class, GUIDELINES_PATH);
			if (guidelines != null && !guidelines.trim().isEmpty()) {
				sections.add(guidelines.trim());
			}
		}
		return sections.isEmpty() ? null : String.join("\n\n", sections);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class<GlueServiceArtifact> getArtifactClass() {
		Class artifactClass = GlueTestServiceArtifact.class;
		return artifactClass;
	}

	@Override
	public String getArtifactType() {
		return ARTIFACT_TYPE;
	}

	private RunProfileConfiguration unmarshal(GlueTestServiceArtifact artifact, String content) throws Exception {
		JAXBContext context = JAXBContext.newInstance(RunProfileConfiguration.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		unmarshaller.setAdapter(new ArtifactXMLAdapter(artifact.getRepository()));
		return (RunProfileConfiguration) unmarshaller.unmarshal(new ByteArrayInputStream(content.getBytes("UTF-8")));
	}

	private String marshal(GlueTestServiceArtifact artifact) throws Exception {
		JAXBContext context = JAXBContext.newInstance(RunProfileConfiguration.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.setAdapter(new ArtifactXMLAdapter(artifact.getRepository()));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		marshaller.marshal(artifact.getRunProfile(), output);
		return new String(output.toByteArray(), "UTF-8");
	}

	private boolean hasErrors(List<Validation<?>> validations) {
		for (Validation<?> validation : validations) {
			if (validation != null && validation.getSeverity() == ValidationMessage.Severity.ERROR) {
				return true;
			}
		}
		return false;
	}

	private class StubFragment implements ArtifactFragment {

		private final GlueTestServiceArtifact artifact;
		private final boolean editable;

		private StubFragment(GlueTestServiceArtifact artifact, boolean editable) {
			this.artifact = artifact;
			this.editable = editable;
		}

		@Override
		public boolean isEditable() {
			return editable;
		}

		@Override
		public boolean isRemovable() {
			return false;
		}

		@Override
		public String getPath() {
			return STUB_PATH;
		}

		@Override
		public String getContent() {
			try {
				return marshal(artifact);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String getContentType() {
			return CONTENT_TYPE;
		}

		@Override
		public String getArtifactId() {
			return artifact.getId();
		}

		@Override
		public String getFragmentType() {
			return FRAGMENT_TYPE;
		}

		@Override
		public Map<String, String> getProperties() {
			return new LinkedHashMap<String, String>();
		}

		@Override
		public Long getLastModified() {
			return getFragmentLastModified(artifact.getId(), STUB_PATH);
		}
	}
}

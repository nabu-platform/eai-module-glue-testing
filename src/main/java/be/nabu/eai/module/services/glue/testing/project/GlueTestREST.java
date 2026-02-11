package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import be.nabu.eai.module.services.glue.testing.GlueTestServiceArtifact;
import be.nabu.eai.module.services.glue.testing.project.GlueTestProjectArtifact.GlueTestProjectOutput;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.eai.server.Server;
import be.nabu.glue.impl.ScriptResultListener;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;

@Path("/test")
public class GlueTestREST {
	
	@Context
	private Server server;
	
	@Path("/run")
	public GlueTestProjectOutput run(@QueryParam("id") String id, @QueryParam("concurrency") Integer threadCount) throws ServiceException {
		List<GlueTestServiceArtifact> artifacts = server.getRepository().getArtifacts(GlueTestServiceArtifact.class);
		artifacts = filter(artifacts, id);
		return runAll((EAIResourceRepository) server.getRepository(), artifacts, null);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static GlueTestProjectOutput runAll(EAIResourceRepository repository, List<GlueTestServiceArtifact> tests, ScriptResultListener listener) throws ServiceException {
		GlueTestProjectArtifact project = new GlueTestProjectArtifact("$generated", null, repository);
		List list = tests;
		project.getConfig().setTests(list);
		project.setResultListener(listener);
		ComplexContent execute = project.newInstance().execute(repository.newExecutionContext(SystemPrincipal.ROOT), null);
		return TypeUtils.getAsBean((ComplexContent) execute.get("result"), GlueTestProjectOutput.class);
	}
	
	public static List<GlueTestServiceArtifact> filter(List<GlueTestServiceArtifact> artifacts, String id) {
		return artifacts.stream()
			.filter(artifact -> id == null || artifact.getId().startsWith(id + ".") || artifact.getId().equals(id))
			.toList();
	}
}

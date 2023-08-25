package be.nabu.eai.module.services.glue.testing.project;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.services.glue.GlueServiceArtifact;
import be.nabu.eai.module.services.glue.testing.GlueTestServiceArtifact;
import be.nabu.eai.repository.RepositoryThreadFactory;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.glue.api.ExecutionEnvironment;
import be.nabu.glue.api.OutputFormatter;
import be.nabu.glue.api.OutputFormatterProvider;
import be.nabu.glue.api.ParserProvider;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.ScriptFilter;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.api.ScriptRuntimeRunnableWrapper;
import be.nabu.glue.api.runs.ScriptResult;
import be.nabu.glue.api.runs.ScriptResultInterpretation;
import be.nabu.glue.api.runs.ScriptResultInterpreter;
import be.nabu.glue.api.runs.ScriptRunner;
import be.nabu.glue.core.impl.EnvironmentLabelEvaluator;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.repositories.DynamicScript;
import be.nabu.glue.impl.MatrixScriptRepository;
import be.nabu.glue.impl.MultithreadedScriptRunner;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatted.FormattedDashboard;
import be.nabu.glue.impl.formatted.FormattedScriptResult;
import be.nabu.glue.json.JSONOutputFormatter;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.glue.utils.ScriptUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.memory.MemoryDirectory;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.ServiceUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.services.pojo.MethodServiceInterface;
import be.nabu.libs.types.api.ComplexContent;

public class GlueTestProjectArtifact extends JAXBArtifact<GlueTestProjectConfiguration> implements DefinedService {

	public static class GlueTestProjectOutput {
		private List<FormattedScriptResult> results;
		private FormattedDashboard summary;
		public List<FormattedScriptResult> getResults() {
			return results;
		}
		public void setResults(List<FormattedScriptResult> results) {
			this.results = results;
		}
		public FormattedDashboard getSummary() {
			return summary;
		}
		public void setSummary(FormattedDashboard summary) {
			this.summary = summary;
		}
	}
	
	public GlueTestProjectArtifact(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "test-project.xml", GlueTestProjectConfiguration.class);
	}

	@Override
	public ServiceInterface getServiceInterface() {
		return MethodServiceInterface.wrap(GlueTestProjectInterface.class, "run");
	}

	@Override
	public ServiceInstance newInstance() {
		return new GlueTestProjectInstance(this);
	}

	@Override
	public Set<String> getReferences() {
		return null;
	}

	public class GlueTestProjectInstance implements ServiceInstance {

		private GlueTestProjectArtifact project;

		public GlueTestProjectInstance(GlueTestProjectArtifact glueTestProjectArtifact) {
			this.project = glueTestProjectArtifact;
		}

		@Override
		public Service getDefinition() {
			return project;
		}

		@Override
		public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
			try {
				ScriptFilter filter = new ScriptFilter() {
					@Override
					public boolean accept(Script script) {
						return true;
					}
				};
				ScriptResultInterpreter interpreter = new ScriptResultInterpreter() {
					@Override
					public ScriptResultInterpretation interpret(ScriptResult result) {
						return null;
					}
				};
				Integer amountOfThreads = input == null ? null : (Integer) input.get("amountOfThreads");
				if (amountOfThreads == null) {
					amountOfThreads = project.getConfiguration().getAmountOfThreads();
				}
				Long maxScriptRuntime = input == null ? null : (Long) input.get("maxScriptRuntime");
				if (maxScriptRuntime == null) {
					maxScriptRuntime = project.getConfiguration().getMaxScriptRuntime();
				}
				ScriptRepository repository = new MatrixScriptRepository(new GlueTestProjectRepository(project), filter);
				ScriptRunner runner = new MultithreadedScriptRunner(amountOfThreads == null ? 1 : amountOfThreads, maxScriptRuntime == null ? 300000 : maxScriptRuntime, false, new RepositoryThreadFactory(project.getRepository()));
				((MultithreadedScriptRunner) runner).setRuntimeWrapper(new ScriptRuntimeRunnableWrapper() {
					@Override
					public Runnable wrap(ScriptRuntime runtime) {
						return new Runnable() {
							@Override
							public void run() {
								ServiceRuntime.setGlobalContext(new HashMap<String, Object>());
								try {
									runtime.run();
								}
								finally {
									ServiceRuntime.setGlobalContext(null);
								}
							}
						};
					}
				});
				((MultithreadedScriptRunner) runner).setOutputFormatterProvider(new OutputFormatterProvider() {
					@Override
					public OutputFormatter newFormatter(OutputFormatter parent) {
						return new JSONOutputFormatter(parent);
					}
				});
				ExecutionEnvironment environment = new SimpleExecutionEnvironment("local");
				List<ScriptResult> results = runner.run(environment, repository, filter, new EnvironmentLabelEvaluator(null));
				List<FormattedScriptResult> formatted = new ArrayList<FormattedScriptResult>();
				for (ScriptResult result : results) {
					formatted.add(FormattedScriptResult.format(result, null));
				}
				FormattedDashboard dashboard = FormattedDashboard.format(interpreter, results.toArray(new ScriptResult[results.size()]));
				GlueTestProjectOutput result = new GlueTestProjectOutput();
				result.setResults(formatted);
				result.setSummary(dashboard);
				ComplexContent output = project.getServiceInterface().getOutputDefinition().newInstance();
				output.set("result", result);
				//output.set("results", formatted);
				//output.set("summary", dashboard);
				return output;
			}
			catch (IOException e) {
				throw new ServiceException(e);
			}
		}
		
	}
	
	public static class GlueTestProjectRepository implements ScriptRepository {

		private Logger logger = LoggerFactory.getLogger(getClass());
		private GlueTestProjectArtifact project;
		private Map<String, Script> scripts;
		private ParserProvider provider;
		
		public GlueTestProjectRepository(GlueTestProjectArtifact project) throws IOException {
			this.project = project;
			refresh();
		}
		
		@Override
		public Iterator<Script> iterator() {
			return scripts.values().iterator();
		}

		@Override
		public Script getScript(String name) throws IOException, ParseException {
			return scripts.get(name);
		}

		@Override
		public ParserProvider getParserProvider() {
			return provider;
		}

		@Override
		public ScriptRepository getParent() {
			return null;
		}

		@Override
		public void refresh() throws IOException {
			scripts = new HashMap<String, Script>();
			if (project.getConfiguration().getTests() != null) {
				for (DefinedService testService : project.getConfiguration().getTests()) {
					if (testService instanceof GlueTestServiceArtifact) {
						scripts.put(ScriptUtils.getFullName(((GlueServiceArtifact) testService).getScript()), ((GlueServiceArtifact) testService).getScript());
						if (provider == null) {
							provider = ((GlueServiceArtifact) testService).getParserProvider();
						}
					}
					else {
						if (provider == null) {
							provider = new GlueParserProvider(new ServiceMethodProvider(project.getRepository(), project.getRepository()));
						}
						String id = ((DefinedService) testService).getId();
						DynamicScript script = new DynamicScript(
							id.indexOf('.') > 0 ? id.substring(0, id.lastIndexOf('.')) : null,
							(id.indexOf('.') > 0 ? id.substring(id.lastIndexOf('.') + 1) : id) + "$glue",
							this,
							Charset.defaultCharset(),
							new MemoryDirectory()
						);
						try {
							script.setContent(id + "()");
							scripts.put(ScriptUtils.getFullName(script), script);
						}
						catch (ParseException e) {
							logger.error("Could not generate dynamic script for: " + id, e);
						}
					}
				}
			}
		}
		
	}
}

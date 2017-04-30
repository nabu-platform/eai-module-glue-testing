package nabu.testing.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.glue.api.ExecutionException;
import be.nabu.glue.api.Executor;
import be.nabu.glue.api.ExecutorContext;
import be.nabu.glue.api.ExecutorGroup;
import be.nabu.glue.api.ScriptRepository;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.glue.core.impl.methods.TestMethods;
import be.nabu.glue.core.impl.parsers.GlueParserProvider;
import be.nabu.glue.core.repositories.DynamicScript;
import be.nabu.glue.core.repositories.DynamicScriptRepository;
import be.nabu.glue.impl.SimpleExecutionEnvironment;
import be.nabu.glue.impl.formatted.FormattedValidation;
import be.nabu.glue.services.CombinedExecutionContextImpl;
import be.nabu.glue.services.ServiceMethodProvider;
import be.nabu.glue.utils.ScriptRuntime;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ExecutionContext;

@WebService
public class Services {

	private ExecutionContext executionContext;
	private boolean generated;
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateEquals(@WebParam(name = "message") String message, @WebParam(name = "expected") Object expected, @WebParam(name = "actual") Object actual) throws IOException {
		start();
		try {
			TestMethods.validateEquals(message, expected, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateNotEquals(@WebParam(name = "message") String message, @WebParam(name = "expected") Object expected, @WebParam(name = "actual") Object actual) throws IOException {
		start();
		try {
			TestMethods.validateNotEquals(message, expected, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateTrue(@WebParam(name = "message") String message, @WebParam(name = "actual") Boolean actual) throws IOException {
		start();
		try {
			TestMethods.validateTrue(message, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}

	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateFalse(@WebParam(name = "message") String message, @WebParam(name = "actual") Boolean actual) throws IOException {
		start();
		try {
			TestMethods.validateFalse(message, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateMatches(@WebParam(name = "message") String message, @WebParam(name = "regex") String regex, @WebParam(name = "actual") String actual) throws IOException {
		start();
		try {
			TestMethods.validateMatches(message, regex, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}
	
	@SuppressWarnings("unchecked")
	@WebResult(name = "result")
	public FormattedValidation validateNotMatches(@WebParam(name = "message") String message, @WebParam(name = "regex") String regex, @WebParam(name = "actual") String actual) throws IOException {
		start();
		try {
			TestMethods.validateNotMatches(message, regex, actual);
			List<GlueValidation> messages = (List<GlueValidation>) ScriptRuntime.getRuntime().getContext().get(TestMethods.VALIDATION);
			// get the last one
			return FormattedValidation.format(messages.get(messages.size() - 1));
		}
		finally {
			stop();
		}
	}

	private void start() {
		ScriptRuntime runtime = ScriptRuntime.getRuntime();
		if (runtime == null) {
			generated = true;
			runtime = generateRuntime();
			runtime.registerInThread();
		}
	}
	private void stop() {
		if (generated) {
			ScriptRuntime.getRuntime().unregisterInThread();
		}
	}
	
	private ScriptRuntime generateRuntime() {
		GlueParserProvider provider = new GlueParserProvider(new ServiceMethodProvider(EAIResourceRepository.getInstance(), EAIResourceRepository.getInstance()));
		ScriptRepository scriptRepository = new DynamicScriptRepository(provider);
		try {
			// get the id of the parent service, the current service is the validation routine
			String id = ((DefinedService) ServiceRuntime.getRuntime().getParent().getService()).getId();
			DynamicScript script = new DynamicScript(
				id.indexOf('.') > 0 ? id.substring(0, id.lastIndexOf('.')) : null,
				id.indexOf('.') > 0 ? id.substring(id.lastIndexOf('.') + 1) : id,
				scriptRepository,
				Charset.defaultCharset(),
				null
			);
			CombinedExecutionContextImpl executionEnvironment =	new CombinedExecutionContextImpl(executionContext, new SimpleExecutionEnvironment("local"), null);
			ScriptRuntime runtime = new ScriptRuntime(script, executionEnvironment, new HashMap<String, Object>());
			runtime.getExecutionContext().setCurrent(new Executor() {
				@Override
				public boolean shouldExecute(be.nabu.glue.api.ExecutionContext context) throws ExecutionException {
					return true;
				}
				@Override
				public void execute(be.nabu.glue.api.ExecutionContext context) throws ExecutionException {
					// do nothing
				}
				@Override
				public ExecutorContext getContext() {
					return null;
				}
				@Override
				public String getId() {
					return id;
				}
				@Override
				public ExecutorGroup getParent() {
					return null;
				}
				@Override
				public boolean isGenerated() {
					return true;
				}
			});
			return runtime;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}

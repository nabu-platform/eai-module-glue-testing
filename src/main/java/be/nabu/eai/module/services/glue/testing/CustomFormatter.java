package be.nabu.eai.module.services.glue.testing;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import be.nabu.glue.api.Executor;
import be.nabu.glue.api.OutputFormatter;
import be.nabu.glue.api.Script;
import be.nabu.glue.api.runs.GlueValidation;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

public class CustomFormatter implements OutputFormatter {

	private OutputFormatter chained;
	private int depth;
	private Severity overallSeverity = Severity.INFO;
	
	public static interface Handler {
		public void start();
		public void validate(GlueValidation...validations);
		public void stop(Severity state);
	}
	
	private static Map<String, byte[]> resources = new HashMap<String, byte[]>();
	private Handler handler;
	
	public CustomFormatter(OutputFormatter chained, Handler handler) {
		this.chained = chained;
		this.handler = handler;
	}
	
	@Override
	public void start(Script script) {
		if (depth++ == 0) {
			handler.start();
		}
		chained.start(script);
	}

	@Override
	public void before(Executor executor) {
		chained.before(executor);
	}

	@Override
	public void after(Executor executor) {
		if (depth == 1) {
			handler.stop(Severity.INFO);
		}
		chained.after(executor);
	}

	@Override
	public void validated(final GlueValidation...validations) {
		handler.validate(validations);
		for (GlueValidation validation : validations) {
			if (Severity.ERROR.equals(validation.getSeverity()) || Severity.CRITICAL.equals(validation.getSeverity())) {
				overallSeverity = Severity.ERROR;
			}
		}
		chained.validated(validations);
	}
	
	@Override
	public void print(Object... messages) {
		chained.print(messages);
	}

	@Override
	public void end(Script script, Date started, Date stopped, Exception exception) {
		if (--depth == 0) {
			if (exception != null) {
				overallSeverity = Severity.ERROR;
			}
			handler.stop(overallSeverity);
		}
		chained.end(script, started, stopped, exception);
	}

	@Override
	public boolean shouldExecute(Executor executor) {
		return true;
	}
	
}


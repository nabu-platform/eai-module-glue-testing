package be.nabu.eai.module.services.glue.testing;

import javax.jws.WebResult;

import be.nabu.glue.impl.formatted.FormattedScriptResult;

public interface TestCase {
	@WebResult(name = "result")
	public FormattedScriptResult run();
}

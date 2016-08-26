package be.nabu.eai.module.services.glue.testing.project;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.glue.impl.formatted.FormattedDashboard;
import be.nabu.glue.impl.formatted.FormattedScriptResult;

@XmlRootElement(name = "result")
public class GlueTestProjectResult {
	
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

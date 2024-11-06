/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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

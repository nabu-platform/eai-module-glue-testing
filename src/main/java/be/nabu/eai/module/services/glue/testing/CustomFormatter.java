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

package be.nabu.eai.module.services.glue.testing;

import java.util.Date;

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


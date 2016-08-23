package be.nabu.eai.module.services.glue.testing;

import java.io.IOException;
import java.io.Writer;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

public class TextAreaWriter extends Writer {

	private TextArea textarea;

	public TextAreaWriter(TextArea textarea) {
		this.textarea = textarea;
	}
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		final String string = new String(cbuf, off, len);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				textarea.appendText(string);
			}
		});
	}

	@Override
	public void flush() throws IOException {
		// do nothing
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}
}

package wdutil.wdjws.ws;

import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;

public class Log4JWriter extends Writer {

	protected final StringBuilder sb = new StringBuilder();
	private final Logger logger;

	public Log4JWriter(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		sb.append(cbuf, off, len);
		if (sb.indexOf("\n") > -1) {
			flush();
		}
	}

	@Override
	public void flush() throws IOException {
		if (sb.length() > 0) {
			logger.debug(sb.toString());
		}
		sb.setLength(0);

	}

	@Override
	public void close() throws IOException {
		flush();

	}
}

package com.sarxos.medusa.data;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sarxos.medusa.market.Quote;
import com.sarxos.medusa.market.Symbol;


/**
 * Quotes stream reader.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class QuotesStreamReader implements Closeable {

	/**
	 * Full data format.
	 */
	public static final SimpleDateFormat DATE_FORMAT_FULL = new SimpleDateFormat("yyyyMMddHHmmss");

	/**
	 * Short data format.
	 */
	public static final SimpleDateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Possible stream formats.
	 * 
	 * @author Bartosz Firyn (SarXos)
	 */
	public static enum Format {
		UNKNOWN,
		MST,
		CGL_INTRA,
		CGL_DAY,
		OBL;
	}

	/**
	 * Underlying buffered reader.
	 */
	private BufferedReader br = null;

	/**
	 * File line number.
	 */
	private int num = 0;

	/**
	 * Already processed line.
	 */
	private String line = null;

	/**
	 * Recognized stream format.
	 */
	private Format format = null;

	private boolean closed = false;

	/**
	 * Create PRN reader from input stream.
	 * 
	 * @param is - input stream
	 */
	public QuotesStreamReader(InputStream is) {
		this(new InputStreamReader(is));
	}

	/**
	 * @return true in case if stream is closed, false otherwise
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Create PRN reader from character stream reader.
	 * 
	 * @param r - characters reader
	 */
	public QuotesStreamReader(Reader r) {
		if (r == null) {
			throw new IllegalArgumentException("Reader cannot be null");
		}
		this.br = new BufferedReader(r);
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			br.close();
			br = null;
		}
	}

	/**
	 * Recognize stream format.
	 * 
	 * @throws IOException
	 * @throws IllegalAccessException
	 */
	private void recognize() throws IOException, IllegalAccessException {
		if (format != null) {
			throw new IllegalAccessException("Format has been already set");
		}
		if (br.ready()) {
			br.mark(1024);
			String fline = getLine();
			if (fline.startsWith("\"")) {
				format = Format.OBL;
			} else if (fline.startsWith("<")) {
				format = Format.MST;
			} else {
				String[] parts = fline.split(",");
				if (parts.length == 10) {
					format = Format.CGL_INTRA;
				} else {
					format = Format.CGL_DAY;
				}
			}
			br.reset();
		}
		if (format == null) {
			format = Format.UNKNOWN;
		}
	}

	/**
	 * Reads and return quotes from the stream. Maximum number of reads is equal
	 * to the input array length, however there can be less quotes in the
	 * underlying stream and in this case this method returns integer less then
	 * input array length.
	 * 
	 * @param quotes - array where quotes should be stored
	 * @return Number of read quotes
	 * @throws IOException - when underlying stream is closed
	 * @throws ParseException - if underlying stream cannot be parsed
	 */
	public int read(Quote[] quotes) throws IOException, ParseException {

		if (quotes == null) {
			throw new IllegalArgumentException("Quotes array canot be null");
		}
		if (!check()) {
			return -1;
		}

		int i = 0;
		for (i = 0; i < quotes.length; i++) {
			if (br.ready()) {
				quotes[i] = read();
			} else {
				break;
			}
		}

		return i;
	}

	/**
	 * Reads and return quote from the stream.
	 * 
	 * @return Quote which has been read
	 * @throws IOException - when underlying stream is closed
	 * @throws ParseException - if underlying stream cannot be parsed
	 */
	public Quote read() throws IOException, ParseException {

		if (!check()) {
			return null;
		}

		Quote q = null;
		String line = getLine();

		switch (format) {
			case CGL_INTRA:
				q = fromCGLIntra(line);
				break;

			case CGL_DAY:
				// read
				break;

			case OBL:
			case MST:
				if (line.startsWith("<") || line.startsWith("\"")) {
					// omit headers
					line = getLine();
				}
				q = fromMSTDay(line);
				// read
				break;
		}

		return q;
	}

	/**
	 * Check stream configuration.
	 * 
	 * @return true if underlying stream is ready, false otherwise
	 * @throws ParseException - when stream format is unknown
	 * @throws IOException - when underlying stream cannot be read
	 */
	private boolean check() throws ParseException, IOException {
		if (closed) {
			throw new IOException("Stream is already closed and cannot be read");
		}
		if (!br.ready()) {
			return false;
		}
		if (format == null) {
			try {
				recognize();
			} catch (IllegalAccessException e) {
				throw new ParseException(e.getMessage(), num);
			}
		}
		if (format == Format.UNKNOWN) {
			throw new ParseException("Unknown file format", 0);
		}
		return true;
	}

	/**
	 * Tells whether this stream is ready to be read. Stream is ready if the
	 * buffer is not empty, or if the underlying buffered reader is ready.
	 * 
	 * @exception IOException If an I/O error occurs
	 */
	public boolean ready() throws IOException {
		return br.ready();
	}

	private Quote fromCGLIntra(String str) throws ParseException {

		String[] parts = str.split(",");

		// 0 ticker
		// 1 dunno
		// 2 date yyyyMMdd,
		// 3 time hhmmdd,
		// 4 open
		// 5 high
		// 6 low
		// 7 close
		// 8 volume
		// 9 open interests

		Symbol symbol = Symbol.valueOfName(parts[0]);
		Date date = DATE_FORMAT_FULL.parse(parts[2] + parts[3]);
		double open = Double.parseDouble(parts[4]);
		double high = Double.parseDouble(parts[5]);
		double low = Double.parseDouble(parts[6]);
		double close = Double.parseDouble(parts[7]);
		long volume = Long.parseLong(parts[8]);

		return new Quote(symbol, date, open, high, low, close, volume);
	}

	private Quote fromMSTDay(String str) throws ParseException {

		String[] parts = str.split(",");

		// 0 ticker
		// 1 date yyyyMMdd,
		// 2 open
		// 3 high
		// 4 low
		// 5 close
		// 6 volume
		// 7 open interests

		Symbol symbol = Symbol.valueOfName(parts[0]);
		Date date = DATE_FORMAT_SHORT.parse(parts[1]);
		double open = Double.parseDouble(parts[2]);
		double high = Double.parseDouble(parts[3]);
		double low = Double.parseDouble(parts[4]);
		double close = Double.parseDouble(parts[5]);
		long volume = Long.parseLong(parts[6]);

		return new Quote(symbol, date, open, high, low, close, volume);
	}

	/**
	 * @return the format
	 */
	protected Format getFormat() {
		return format;
	}

	/**
	 * Will seek the stream and change pointer position just before given date.
	 * This method will return true if date has been found, false otherwise.
	 * Please note that this method will change pointer position, so if you want
	 * to start reading from the point where you have called this method, you
	 * will have to reallocate whole stream. Therefore please use this method
	 * with care!
	 * 
	 * @param date - date to find
	 * @return true in case if date has been found, false otherwise
	 * @throws IOException
	 * @throws ParseException
	 */
	public boolean seek(Date date) throws IOException, ParseException {

		if (!check()) {
			return false;
		}

		String str = "," + DATE_FORMAT_SHORT.format(date) + ",";
		String l = null;

		while (br.ready()) {
			if ((l = br.readLine()).indexOf(str) != -1) {
				line = l;
				return true;
			}
		}

		return false;
	}

	/**
	 * Read and return line from the underlying reader.
	 * 
	 * @return Line from the quotes file
	 * @throws IOException
	 */
	private String getLine() throws IOException {
		String s = null;
		if (line != null) {
			s = line;
			line = null;
		} else {
			num++;
			s = br.readLine();
		}
		return s;
	}
}

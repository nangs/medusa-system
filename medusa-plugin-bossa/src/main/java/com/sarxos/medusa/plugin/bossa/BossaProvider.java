package com.sarxos.medusa.plugin.bossa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sarxos.medusa.data.QuotesIterator;
import com.sarxos.medusa.data.QuotesRegistry;
import com.sarxos.medusa.http.MedusaHttpClient;
import com.sarxos.medusa.market.Paper;
import com.sarxos.medusa.market.Quote;
import com.sarxos.medusa.market.Symbol;
import com.sarxos.medusa.provider.HistoryProvider;
import com.sarxos.medusa.provider.ProviderException;
import com.sarxos.medusa.util.Configuration;
import com.sarxos.medusa.util.DateUtils;


public class BossaProvider implements HistoryProvider {

	/**
	 * Used date format
	 */
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Configuration instance
	 */
	private static final Configuration CFG = Configuration.getInstance();

	/**
	 * URL to file with quotes for 6 last days
	 */
	private static final String LAST_QUOTES_URL = "http://bossa.pl/pub/metastock/mstock/sesjaall/few_last.zip";

	/**
	 * MSTCLG metastock file URL
	 */
	private static final String MSTCGL_URL = "http://bossa.pl/pub/metastock/cgl/mstcgl.zip";

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BossaProvider.class.getSimpleName());

	@Override
	public List<Quote> getLastQuotes(Symbol symbol) throws ProviderException {

		boolean download = true;

		String dir = CFG.getProperty("core", "tmpdir");
		File f = new File(dir + "/few_last.zip");

		if (f.exists()) {
			Date modified = new Date(f.lastModified());
			if (DateUtils.isToday(modified)) {
				download = false;
			}
		}

		if (download) {
			try {
				new MedusaHttpClient().download(LAST_QUOTES_URL, f);
			} catch (HttpException e) {
				LOG.error(e.getMessage(), e);
			}
		}

		List<File> files = new LinkedList<File>();

		ZipEntry entry = null;
		String name = null;

		FileOutputStream fos = null;
		InputStream is = null;

		int i = -1;
		byte[] bytes = new byte[1024];

		try {

			ZipFile zip = new ZipFile(f);

			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {

				entry = entries.nextElement();
				name = entry.getName();

				if (!name.matches("\\d+\\.prn")) {
					continue;
				}

				is = zip.getInputStream(entry);

				f = new File(dir + "/" + name);
				if (!f.exists()) {
					if (!f.createNewFile()) {
						throw new ProviderException("Cannot create file " + f.getName());
					}
				}

				fos = new FileOutputStream(f);
				while ((i = is.read(bytes)) != -1) {
					fos.write(bytes, 0, i);
				}
				fos.close();

				files.add(f);
			}
		} catch (Exception e) {
			throw new ProviderException(e);
		}

		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		String line = null;
		String[] parts = null;

		Date date = null;
		double open = 0;
		double high = 0;
		double low = 0;
		double close = 0;
		long volume = 0;

		List<Quote> quotes = new LinkedList<Quote>();
		Quote quote = null;

		for (File file : files) {
			try {

				fis = new FileInputStream(file);
				isr = new InputStreamReader(fis);
				br = new BufferedReader(isr);

				while (br.ready()) {
					line = br.readLine();
					if (line.startsWith(symbol.getName())) {
						parts = line.split(",");
						break;
					}
				}

				if (parts == null) {

					// no quotes for given day (it is sometimes possible for low
					// volumes)

					if (quote == null) {

						List<Quote> tmp = QuotesRegistry.getInstance().getQuotes(symbol);
						if (tmp.size() > 0) {
							quote = tmp.get(tmp.size() - 1);
						} else {
							// no quotes in the db - omit whole day
							continue;
						}

					} else {
						// quote != null -> insert previous quote
					}

				} else {

					// <TICKER>,<DTYYYYMMDD>,<OPEN>,<HIGH>,<LOW>,<CLOSE>,<VOL>
					// KGHM,20101220,156.70,158.20,155.00,157.90,394584

					if (parts.length < 7) {
						throw new ProviderException(
								"Something is wrong with data - should be 7 elements, " +
								"found " + parts.length + " instead!");
					}
					if (!parts[0].equals(symbol.getName())) {
						throw new ProviderException(
								"Something is wrong with read method - tried to read " + symbol + " " +
								"but read " + parts[0] + " instead!");
					}

					date = DATE_FORMAT.parse(parts[1]);
					open = Double.valueOf(parts[2]);
					high = Double.valueOf(parts[3]);
					low = Double.valueOf(parts[4]);
					close = Double.valueOf(parts[5]);
					volume = Long.valueOf(parts[6]);

					quote = new Quote(date, open, high, low, close, volume);
				}

				quotes.add(quote);

			} catch (Throwable e) {
				throw new ProviderException(e);
			}
		}

		return quotes;
	}

	private void downloadMSTCGL(File f) throws ProviderException {
		try {
			new MedusaHttpClient().download(MSTCGL_URL, f);
		} catch (HttpException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	public List<Quote> getAllQuotes(Symbol symbol) throws ProviderException {

		boolean download = true;

		String dir = CFG.getProperty("core", "tmpdir");
		File f = new File(dir + "/mstcgl.zip");

		if (f.exists()) {
			Date modified = new Date(f.lastModified());
			if (DateUtils.isToday(modified)) {
				download = false;
			}
		}

		if (download) {
			downloadMSTCGL(f);
		}

		File file = null;

		ZipEntry entry = null;
		String name = null;

		FileOutputStream fos = null;
		InputStream is = null;

		try {

			File sessall = new File(dir + "/mstcgl");
			if (!sessall.exists()) {
				if (!sessall.mkdirs()) {
					throw new ProviderException("Cannot create directory " + f.getName());
				}
			}

			ZipFile zip = null;

			int attempts = 0;
			do {
				try {
					zip = new ZipFile(f);
					break;
				} catch (ZipException ze) {
					LOG.error("Cannot open ZIP file " + f.getName());
					downloadMSTCGL(f);
				}
			} while (attempts++ < 5);

			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {

				entry = entries.nextElement();
				name = entry.getName();

				if (!name.matches(symbol.getName() + "\\.mst")) {
					continue;
				}

				is = zip.getInputStream(entry);

				File mstf = new File("data/tmp/mstcgl/" + name);
				FileUtils.touch(mstf);

				fos = FileUtils.openOutputStream(mstf);
				IOUtils.copy(is, fos);
				IOUtils.closeQuietly(fos);
				IOUtils.closeQuietly(is);

				file = mstf;
				break;
			}
		} catch (Exception e) {
			throw new ProviderException(e);
		}

		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		String line = null;
		String[] parts = null;

		Date date = null;
		double open = 0;
		double high = 0;
		double low = 0;
		double close = 0;
		long volume = 0;

		List<Quote> quotes = new LinkedList<Quote>();
		Quote q = null, t = null;

		try {

			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);

			while (br.ready()) {

				line = br.readLine();
				parts = line.split(",");

				// <TICKER>,<DTYYYYMMDD>,<OPEN>,<HIGH>,<LOW>,<CLOSE>,<VOL>
				// KGHM,20101220,156.70,158.20,155.00,157.90,394584

				// headers line
				if (parts[0].startsWith("<TICKER>")) {
					continue;
				}

				if (parts.length < 7) {
					throw new ProviderException(
							"Something is wrong with data - should be 7 elements, " +
							"found " + parts.length + " instead!");
				}
				if (!parts[0].equals(symbol.getName())) {
					throw new ProviderException(
							"Something is wrong with read method - tried to read " + symbol + " " +
							"but read " + parts[0] + " instead!");
				}

				date = DATE_FORMAT.parse(parts[1]);
				open = Double.valueOf(parts[2]);
				high = Double.valueOf(parts[3]);
				low = Double.valueOf(parts[4]);
				close = Double.valueOf(parts[5]);
				volume = Long.valueOf(parts[6]);

				t = new Quote(date, open, high, low, close, volume);
				if (q != null) {
					t.setPrev(q);
					q.setNext(t);
				}

				q = t;

				quotes.add(t);
			}

		} catch (Throwable e) {
			throw new ProviderException(e);
		}

		return quotes;
	}

	private void downloadZIP(File zipf, Paper paper) throws ProviderException {

		Symbol symbol = paper.getSymbol();
		MedusaHttpClient client = new MedusaHttpClient();
		String dirurl = null;

		switch (paper.getGroup()) {
			case FUTURES_CURRENCY:
			case FUTURES_INDEXES:
			case FUTURES_QUOTES:
			case OPTIONS_INDEXES:
			case OPTIONS_MINIWIG:
				dirurl = "fut";
				break;
			default:
				dirurl = "cgl";
				break;
		}

		String url = "http://bossa.pl/pub/intraday/mstock/" + dirurl + "/" + symbol.getName() + ".zip";

		try {
			client.download(url, zipf);
		} catch (HttpException e) {
			throw new ProviderException(e);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("ZIP file for symbol " + symbol + " has been downloaded");
		}
	}

	@Override
	public QuotesIterator<Quote> getIntradayQuotes(Symbol symbol) throws ProviderException {

		String dir = CFG.getProperty("core", "tmpdir", "data/tmp");

		File prn = new File(dir + "/intraday/" + symbol.getName() + ".prn");

		boolean download = true;
		if (prn.exists()) {
			Date modified = new Date(prn.lastModified());
			if (DateUtils.isToday(modified)) {
				download = false;
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug(
					"PRN file for symbol " + symbol + " exists and should" +
					(download ? " " : " not ") +
					"be downloaded"
				);
			}
		} else {
			try {
				FileUtils.touch(prn);
			} catch (IOException e) {
				throw new ProviderException(e);
			}
		}

		if (download) {

			File zipf = new File(dir + "/" + symbol.getName() + ".zip");
			Paper paper = new Paper(symbol);

			downloadZIP(zipf, paper);

			ZipFile zf = null;

			int attempts = 0;
			do {
				try {
					zf = new ZipFile(zipf);
					break;
				} catch (ZipException ze) {
					LOG.error("Cannot open ZIP file " + zipf.getName());
					downloadZIP(zipf, paper);
				} catch (IOException e) {
					throw new ProviderException(e);
				}
			} while (attempts++ < 5);

			boolean unpacked = false;
			Enumeration<? extends ZipEntry> zfe = zf.entries();

			if (LOG.isDebugEnabled()) {
				LOG.debug("Extracting ZIP for symbol " + symbol);
			}

			while (zfe.hasMoreElements()) {

				ZipEntry ze = zfe.nextElement();
				if (!prn.getName().equals(ze.getName())) {
					continue;
				}

				try {

					OutputStream os = FileUtils.openOutputStream(prn);
					InputStream is = zf.getInputStream(ze);
					IOUtils.copy(is, os);
					IOUtils.closeQuietly(is);
					IOUtils.closeQuietly(os);

					unpacked = true;

				} catch (IOException e) {
					throw new ProviderException(e);
				} finally {
					if (!FileUtils.deleteQuietly(zipf)) {
						try {
							FileUtils.forceDeleteOnExit(zipf);
						} catch (IOException e) {
							throw new ProviderException(e);
						}
					}
				}
			}

			if (!unpacked) {
				throw new ProviderException("Cannot extract " + prn.getName() + " file");
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("ZIP for symbol " + symbol + " has been extracted to " + prn.getName());
			}
		}

		try {
			return new QuotesIterator<Quote>(symbol);
		} catch (IOException e) {
			throw new ProviderException(e);
		}
	}

	public static void main(String[] args) throws ProviderException, ParseException {
		BossaProvider b = new BossaProvider();
		QuotesIterator<Quote> qi = b.getIntradayQuotes(Symbol.FW20H11);
		qi.forward(new SimpleDateFormat("yyyy-MM-dd HH").parse("2011-05-12 12"));
		qi.forward(new SimpleDateFormat("yyyy-MM-dd").parse("2011-05-12"));
		while (qi.hasNext()) {
			System.out.println(qi.next());
			break;
		}
		System.out.println(qi.next());
	}
}

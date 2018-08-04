package bnw.abm.intg.filemanager.gz;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public class GzReader {

	/**
	 * Reads gzip file
	 * 
	 * @param gzFile
	 *            Path object to gzip file
	 * @return GZIPInputStream for gzip file
	 */
	public GZIPInputStream readGz(Path gzFile) {
		InputStream fileIs = null;
		BufferedInputStream bufferedIs = null;
		GZIPInputStream gzipIs = null;
		try {
			fileIs = Files.newInputStream(gzFile);
			// Even though GZIPInputStream has a buffer it reads individual
			// bytes when processing the header, better add a buffer in-between
			bufferedIs = new BufferedInputStream(fileIs);
			gzipIs = new GZIPInputStream(bufferedIs);
		} catch (IOException e) {
			closeSafely(gzipIs);
			closeSafely(bufferedIs);
			closeSafely(fileIs);
			throw new UncheckedIOException(e);
		}
		return gzipIs;
	}

	private static void closeSafely(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

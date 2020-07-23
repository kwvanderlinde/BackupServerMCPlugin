package com.kwvanderlinde.mc.backupserver.http;

import com.google.common.collect.ImmutableMap;
import com.kwvanderlinde.mc.backupserver.Plugin;
import com.kwvanderlinde.mc.backupserver.HomogeneousFileVisitor;
import com.kwvanderlinde.mc.backupserver.config.Config;
import fi.iki.elonen.NanoHTTPD;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class FileServer extends NanoHTTPD {
	private final Plugin plugin;

	public FileServer(int port, Plugin plugin) {
		super(port);

		this.plugin = plugin;
	}

	@Override
	public Response serve(IHTTPSession session) {
		final Logger logger = this.plugin.getLogger();
		final Config configuration = this.plugin.getConfiguration();

		if (session.getUri().matches("^/$")) {
			return newFixedLengthResponse("<html><head></head><body>" +
					                              "<a href=\"/backup\">Download a backup</a>" +
					                              "</body></html>");
		}
		else if (session.getUri().matches("^/backup$")) {
			logger.info("Backup requested");

			Instant now = Instant.now();
			DateTimeFormatter formatter = DateTimeFormatter
					.ofPattern("yyyy-MM-dd HH:mm:ss")
					.withZone(ZoneId.of("UTC"));
			// TODO Support local date and local time via ${date} and ${time} ?
			StringSubstitutor substitutor = new StringSubstitutor(ImmutableMap.<String, String>builder()
					                                                      .put("datetime", formatter.format(now))
					                                                      .put("unixtime", Long.toString(now.getEpochSecond()))
					                                                      .build()
			);
			String basename = substitutor.replace(configuration.getBasenameTemplate());
			String filename = basename + ".tar.gz";

			logger.info("> Started writing backup");
			// The response is responsible for closing the data stream, which is why we don't use try-with-resources for `stream`.
			try {
				InputStream stream = this.gzipIt(Paths.get(""), configuration.getFileFilter());
				Response response = newChunkedResponse(Response.Status.OK, "application/gzip", stream);
				response.addHeader("content-disposition", "attachment; filename=\"" + filename +"\"");
				return response;
			}
			catch (Exception e) {
				logger.info("> Failed to send backup");
				logger.severe(e.toString());
			}
		}

		return super.serve(session);
	}

	/**
	 * Starts a thread the reads the Gzip's `directory`, sending the result through the returned `InputStream`.
	 */
	private InputStream gzipIt(Path directory, PathMatcher fileFilter) throws IOException {
		final int pipeSize = this.plugin.getConfiguration().getPipeSize();
		final Logger logger = this.plugin.getLogger();
		PipedInputStream inputStream = new PipedInputStream(pipeSize);  // Default pipe size is 1024 bytes. Is there value in increasing this limit? Perhaps better file transfer speeds?
		// Frustratingly, we must connect to the input stream before anyone tries reading from it. So we must connect outside the write thread.
		PipedOutputStream pipedOutputStream = new PipedOutputStream(inputStream);

		Thread writeThread = new Thread(() -> {
			try (OutputStream piped = pipedOutputStream;
			     OutputStream gzipped = new GzipCompressorOutputStream(piped);
			     TarArchiveOutputStream outputStream = new TarArchiveOutputStream(gzipped)) {

				Files.walkFileTree(directory, new HomogeneousFileVisitor(fileFilter) {
					@Override
					protected void visit(Path path, BasicFileAttributes attrs) throws IOException {
						if (attrs.isOther()) {
							logger.warning(String.format("Unable to backup file '%s' as it is not a directory, regular file or symlink.", path));
							return;
						}
						if ("".equals(path.toString())) {
							// There is no point in creating an entry for the root directory.
							return;
						}

						File f = path.toFile();
						TarArchiveEntry entry;
						if (attrs.isSymbolicLink()) {
							// TarArchiveOutputStream::createArchiveEntry() does not automatically recognize symlinks. We'll have to do it ourselves.
							entry = new TarArchiveEntry(path.toString(), TarConstants.LF_SYMLINK);
							// We'll need to follow the symlink in order to store it. toRealPath() gives back an
							// absolute path so we'll have to relativize it again (against the real path of the server
							// root directory of course).
							Path symlinkName = directory.toRealPath().relativize(path.toRealPath());
							entry.setLinkName(symlinkName.toString());
						}
						else {
							entry = (TarArchiveEntry) outputStream.createArchiveEntry(f, path.toString());
						}

						outputStream.putArchiveEntry(entry);
						if (attrs.isRegularFile()) {
							try (FileInputStream fis = new FileInputStream(f)) {
								try {
									IOUtils.copy(fis, outputStream);
								}
								catch (IOException e) {
									// 99.999% certain this can only happen due to a broken pipe because the read end was closed.
									logger.info("Pipe closed on read end");
									throw new EarlyExitException();
								}
							}
						}
						outputStream.closeArchiveEntry();
					}
				});

				outputStream.finish();
			}
			catch (IOException e) {
				// Unexpected error. Bail.
				throw new RuntimeException(e);
			}
			catch (EarlyExitException e) {
				// Only thrown to get out of walking the directory tree.
			}
		});
		writeThread.start();

		return inputStream;
	}

	private static class EarlyExitException extends RuntimeException {
	}
}

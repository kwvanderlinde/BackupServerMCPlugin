package com.kwvanderlinde.mc.backupserver.config;

import java.net.InetSocketAddress;
import java.nio.file.PathMatcher;

public final class Config {
	private final InetSocketAddress serverAddress;
	private final String basenameTemplate;
	private final PathMatcher fileFilter;
	private final int pipeSize;

	public Config(InetSocketAddress serverAddress, String basenameTemplate, PathMatcher fileFilter, int pipeSize) {
		this.serverAddress = serverAddress;
		this.basenameTemplate = basenameTemplate;
		this.fileFilter = fileFilter;
		this.pipeSize = pipeSize;
	}

	/**
	 * @return The hostname and port that clients should connect to for access to the HTTP server.
	 */
	public InetSocketAddress getServerAddress() {
		return this.serverAddress;
	}

	public String getBasenameTemplate() {
		return this.basenameTemplate;
	}

	public PathMatcher getFileFilter() {
		return this.fileFilter;
	}

	public int getPipeSize() {
		return this.pipeSize;
	}

	@Override
	public String toString() {
		return "Config{" +
				"basenameTemplate='" + basenameTemplate + '\'' +
				", fileFilter=" + fileFilter +
				'}';
	}
}

package com.kwvanderlinde.mc.backupserver;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A `FileVisitor<Path>` that treats files and directories the same.
 */
public abstract class HomogeneousFileVisitor implements java.nio.file.FileVisitor<Path> {
	private final PathMatcher pathMatcher;

	public HomogeneousFileVisitor() {
		this(path -> true);
	}

	public HomogeneousFileVisitor(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	@Override
	public final FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (!this.pathMatcher.matches(dir)) {
			return FileVisitResult.SKIP_SUBTREE;
		}
		this.visit(dir, attrs);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public final FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public final FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		if (this.pathMatcher.matches(file)) {
			visit(file, attrs);
			return FileVisitResult.CONTINUE;
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public final FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	protected abstract void visit(Path path, BasicFileAttributes attrs) throws IOException;
}

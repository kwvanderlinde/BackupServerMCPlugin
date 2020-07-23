package com.kwvanderlinde.mc.backupserver.config;

import org.bukkit.configuration.Configuration;

import java.net.InetSocketAddress;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Parser {
	public Config parse(Configuration configuration) {
		FileSystem fileSystem = FileSystems.getDefault();
		List<String> excludeGlobs = new ArrayList<>();
		List<?> configuredExcludeGlobs = configuration.getList("exclude"); // TODO Use getStringList()? Although, that also loads from the defaults.
		if (configuredExcludeGlobs != null) {
			for (Object configuredExcludeGlob : configuredExcludeGlobs) {
				if (!(configuredExcludeGlob instanceof String)) {
					throw new RuntimeException("Exclude glob should be a string, but I found a " + configuredExcludeGlob.getClass());
				}
				excludeGlobs.add((String) configuredExcludeGlob);
			}
		}
		PathMatcher excluder = path -> {
			for (String glob : excludeGlobs) {
				if (fileSystem.getPathMatcher("glob:" + glob).matches(path)) {
					return false;
				}
			}
			return true;
		};

		String basenameFormat = configuration.getString("basename-template", "{datetime}");

		InetSocketAddress serverAddress = new InetSocketAddress(configuration.getString("http-server-host"),
		                                                        configuration.getInt("http-server-port"));

		int pipeSize = configuration.getInt("pipe-size", 1024);

		return new Config(serverAddress, basenameFormat, excluder, pipeSize);
	}

	// TODO TyeCheckedMap isn't in use right now, but it would be a good idea to apply it to all configuration.

	private static class TypeCheckFailure extends Exception {
		public TypeCheckFailure(String message) {
			super(message);
		}

		public TypeCheckFailure(String message, Throwable cause) {
			super(message, cause);
		}
	}

	private static class TypeCheckedMap {
		private final Map<String, Object> map;

		public TypeCheckedMap(Map<String, Object> map) {
			this.map = map;
		}

		public <T> T getRequired(String key, Class<T> type) throws TypeCheckFailure {
			if (!map.containsKey(key)) {
				throw new TypeCheckFailure(String.format("Map does not contain key '%s'", key));
			}
			Object value = map.get(key);
			if (Number.class.isAssignableFrom(type) || String.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
				try {
					return type.cast(value);
				}
				catch (ClassCastException e) {
					throw new TypeCheckFailure("Incorrect value type", e);
				}
			}

			throw new TypeCheckFailure("Recursive type parsing is not yet supported");
		}


		public <T> T getOptional(String key, Class<T> type, T defaultValue) throws TypeCheckFailure {
			if (!map.containsKey(key)) {
				return defaultValue;
			}
			Object value = map.get(key);
			if (Number.class.isAssignableFrom(type) || String.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)) {
				try {
					return type.cast(value);
				}
				catch (ClassCastException e) {
					throw new TypeCheckFailure("Incorrect value type", e);
				}
			}

			throw new TypeCheckFailure("Recursive type parsing is not yet supported");
		}

		@Override
		public String toString() {
			return "TypeCheckedMap{" +
					"map=" + map +
					'}';
		}
	}
}

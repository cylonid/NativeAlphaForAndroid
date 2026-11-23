package com.cylonid.nativealpha.util;

import android.webkit.URLUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utility {

	private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern
			.compile("attachment; filename=\"(.*)\"; filename\\*=UTF-8''(.*)", Pattern.CASE_INSENSITIVE);

	public static void Assert(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	public static String getFileNameFromDownload(String url, String content_disposition, String mime_type) {
		String file_name = null;
		if (content_disposition != null && !content_disposition.isEmpty()) {
			Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(content_disposition);
			file_name = m.matches() ? m.group(2) : null;
		}
		if (file_name == null) {
			file_name = URLUtil.guessFileName(url, content_disposition, mime_type);
		}

		return file_name;
	}

}

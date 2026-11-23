package com.cylonid.nativealpha;

import com.cylonid.nativealpha.model.WebApp;
import com.cylonid.nativealpha.util.ShortcutIconUtils;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Offline unit tests for HTML/manifest metadata parsing and icon utilities.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {34})
public class UtilUnitTests {

	@Test
	public void getWidthFromHTMLElementString() {
		assertEquals(192, ShortcutIconUtils.getWidthFromIcon("192x192"));
	}

	private ShortcutDialogFragment.MetadataFetchOutcome parseOffline(String baseUrl, Map<String, String> htmlByUrl,
			Map<String, String> manifestByUrl) {
		WebApp webapp = new WebApp(baseUrl, Integer.MAX_VALUE);
		ShortcutDialogFragment frag = ShortcutDialogFragment.newInstance(webapp);
		return frag.parseMetadata(baseUrl, Collections.emptyMap(), url -> {
			String html = htmlByUrl.get(url);
			if (html == null) {
				throw new RuntimeException(new IOException("Offline test: unknown URL " + url));
			}
			return Jsoup.parse(html, url);
		}, manifestUrl -> {
			String body = manifestByUrl.get(manifestUrl);
			if (body == null) {
				throw new RuntimeException(new IOException("Offline test: unknown manifest " + manifestUrl));
			}
			return body;
		});
	}

	@Test
	public void faviconFromWebManifest() {
		String html = "<html><head><link rel=\"manifest\" href=\"/manifest.json\"><title>Site</title></head></html>";
		String manifest = "{\"name\":\"Example PWA\",\"start_url\":\"/app\",\"icons\":[{\"src\":\"/icons/favicon-192.png\",\"sizes\":\"192x192\"},{\"src\":\"/icons/favicon-48.png\",\"sizes\":\"48x48\"}]}";

		ShortcutDialogFragment.MetadataFetchOutcome outcome = parseOffline("https://example.com",
				Collections.singletonMap("https://example.com", html),
				Collections.singletonMap("https://example.com/manifest.json", manifest));

		String favicon = outcome.getResult()[IconFetchResult.FAVICON.index];
		assertEquals("https://example.com/icons/favicon-192.png", favicon);
		assertEquals("Example PWA", outcome.getResult()[IconFetchResult.TITLE.index]);
		assertEquals("https://example.com/app", outcome.getResult()[IconFetchResult.NEW_BASEURL.index]);
	}

	@Test
	public void faviconWithoutManifestPicksLargestIcon() {
		String html = "<html><head><title>News</title>"
				+ "<link rel=\"icon\" sizes=\"32x32\" href=\"/favicon-32x32.png\">"
				+ "<link rel=\"icon\" sizes=\"128x128\" href=\"/favicon-128x128.png\">" + "</head></html>";

		ShortcutDialogFragment.MetadataFetchOutcome outcome = parseOffline("https://example.org",
				Collections.singletonMap("https://example.org", html), Collections.emptyMap());

		assertEquals("https://example.org/favicon-128x128.png", outcome.getResult()[IconFetchResult.FAVICON.index]);
		assertEquals("News", outcome.getResult()[IconFetchResult.TITLE.index]);
	}

	@Test
	public void faviconNullWhenNoIconsDeclared() {
		String html = "<html><head><title>Plain Page</title></head><body>Hello</body></html>";
		ShortcutDialogFragment.MetadataFetchOutcome outcome = parseOffline("http://example.invalid",
				Collections.singletonMap("http://example.invalid", html), Collections.emptyMap());

		assertNull(outcome.getResult()[IconFetchResult.FAVICON.index]);
		assertEquals("Plain Page", outcome.getResult()[IconFetchResult.TITLE.index]);
	}

	@Test
	public void faviconNonExistingSiteHandledGracefully() {
		ShortcutDialogFragment.MetadataFetchOutcome outcome = parseOffline("https://nonexistent.invalid",
				Collections.emptyMap(), Collections.emptyMap());

		assertNull(outcome.getResult()[IconFetchResult.FAVICON.index]);
		assertTrue(outcome.getCandidateIconUrls().isEmpty());
	}

	@Test
	public void metaRefreshRedirectFollowsTargetDocument() {
		String redirectHtml = "<html><head><meta http-equiv=\"refresh\" content=\"0; URL='https://example.com/landing'\"></head></html>";
		String landingHtml = "<html><head><title>Landing</title><link rel=\"apple-touch-icon\" sizes=\"180x180\" href=\"/apple.png\"></head></html>";

		Map<String, String> pages = new java.util.HashMap<>();
		pages.put("https://example.com", redirectHtml);
		pages.put("https://example.com/landing'", landingHtml);
		pages.put("https://example.com/landing", landingHtml);

		ShortcutDialogFragment.MetadataFetchOutcome outcome = parseOffline("https://example.com", pages,
				Collections.emptyMap());

		assertEquals("Landing", outcome.getResult()[IconFetchResult.TITLE.index]);
		assertNotNull(outcome.getResult()[IconFetchResult.FAVICON.index]);
	}
}

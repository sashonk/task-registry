package ru.asocial.task.service.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RssFeedParserTest {

	@Test
	void parsesRss20Items() {
		String xml = """
				<?xml version="1.0"?>
				<rss version="2.0">
				  <channel>
				    <title>Example Feed</title>
				    <item>
				      <title>First</title>
				      <link>https://example.com/1</link>
				      <pubDate>Fri, 04 Sep 2026 10:00:00 GMT</pubDate>
				    </item>
				    <item>
				      <title>Second</title>
				      <link>https://example.com/2</link>
				      <pubDate>Fri, 04 Sep 2026 11:00:00 GMT</pubDate>
				    </item>
				  </channel>
				</rss>
				""";

		RssFeedParser.Feed feed = RssFeedParser.parse(xml, 10);

		assertEquals("Example Feed", feed.title());
		assertEquals(2, feed.items().size());
		assertEquals("First | https://example.com/1 | Fri, 04 Sep 2026 10:00:00 GMT", feed.items().get(0).format());
		assertEquals("Second | https://example.com/2 | Fri, 04 Sep 2026 11:00:00 GMT", feed.items().get(1).format());
	}

	@Test
	void parsesAtomEntriesAndRespectsMaxItems() {
		String xml = """
				<?xml version="1.0" encoding="utf-8"?>
				<feed xmlns="http://www.w3.org/2005/Atom">
				  <title>Atom Feed</title>
				  <entry>
				    <title>Alpha</title>
				    <link href="https://example.com/a"/>
				    <updated>2026-09-04T10:00:00Z</updated>
				  </entry>
				  <entry>
				    <title>Beta</title>
				    <link rel="alternate" href="https://example.com/b"/>
				    <published>2026-09-04T11:00:00Z</published>
				  </entry>
				</feed>
				""";

		RssFeedParser.Feed feed = RssFeedParser.parse(xml, 1);

		assertEquals("Atom Feed", feed.title());
		assertEquals(1, feed.items().size());
		assertEquals("Alpha | https://example.com/a | 2026-09-04T10:00:00Z", feed.items().get(0).format());
	}

	@Test
	void rejectsEmptyFeed() {
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> RssFeedParser.parse("  ", 10));
		assertEquals("RSS feed is empty", exception.getMessage());
	}

	@Test
	void rejectsUnknownFormat() {
		IllegalStateException exception = assertThrows(
				IllegalStateException.class,
				() -> RssFeedParser.parse("<html><body>not a feed</body></html>", 10));
		assertTrue(exception.getMessage().startsWith("Unsupported feed format:"));
	}

	@Test
	void rejectsInvalidXml() {
		assertThrows(IllegalStateException.class, () -> RssFeedParser.parse("<rss><channel>", 10));
	}
}

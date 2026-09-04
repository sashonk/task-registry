package ru.asocial.task.service.task;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class RssFeedParser {

	private static final int MAX_ITEMS_CAP = 50;

	private RssFeedParser() {
	}

	public record Item(String title, String link, String date) {

		public String format() {
			return dash(title) + " | " + dash(link) + " | " + dash(date);
		}

		private static String dash(String value) {
			return value == null || value.isBlank() ? "-" : value.trim();
		}
	}

	public record Feed(String title, List<Item> items) {
	}

	public static Feed parse(String xml, int maxItems) {
		if (xml == null || xml.isBlank()) {
			throw new IllegalStateException("RSS feed is empty");
		}

		int limit = Math.min(Math.max(maxItems, 1), MAX_ITEMS_CAP);
		Document document = parseDocument(xml);
		Element root = document.getDocumentElement();
		if (root == null) {
			throw new IllegalStateException("RSS feed has no root element");
		}

		String name = localName(root);
		if ("rss".equalsIgnoreCase(name) || "rdf".equalsIgnoreCase(name) || hasChild(root, "channel")) {
			return parseRss(root, limit);
		}
		if ("feed".equalsIgnoreCase(name)) {
			return parseAtom(root, limit);
		}
		throw new IllegalStateException("Unsupported feed format: " + name);
	}

	private static Document parseDocument(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setExpandEntityReferences(false);
			factory.setXIncludeAware(false);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		}
		catch (Exception exception) {
			throw new IllegalStateException("Invalid RSS XML: " + exception.getMessage(), exception);
		}
	}

	private static Feed parseRss(Element root, int limit) {
		Element channel = firstChild(root, "channel");
		Element scope = channel != null ? channel : root;
		List<Item> items = new ArrayList<>();
		for (Element item : children(scope, "item")) {
			if (items.size() >= limit) {
				break;
			}
			items.add(new Item(
					firstChildText(item, "title"),
					firstChildText(item, "link"),
					firstNonBlank(firstChildText(item, "pubDate"), firstChildText(item, "date"))));
		}
		return new Feed(firstChildText(scope, "title"), List.copyOf(items));
	}

	private static Feed parseAtom(Element root, int limit) {
		List<Item> items = new ArrayList<>();
		for (Element entry : children(root, "entry")) {
			if (items.size() >= limit) {
				break;
			}
			items.add(new Item(
					firstChildText(entry, "title"),
					atomLink(entry),
					firstNonBlank(firstChildText(entry, "updated"), firstChildText(entry, "published"))));
		}
		return new Feed(firstChildText(root, "title"), List.copyOf(items));
	}

	private static String atomLink(Element entry) {
		String fallback = "";
		for (Element link : children(entry, "link")) {
			String rel = link.getAttribute("rel");
			String href = link.getAttribute("href");
			if (href == null || href.isBlank()) {
				href = text(link);
			}
			if (href.isBlank()) {
				continue;
			}
			if (rel == null || rel.isBlank() || "alternate".equalsIgnoreCase(rel)) {
				return href.trim();
			}
			if (fallback.isBlank()) {
				fallback = href.trim();
			}
		}
		return fallback;
	}

	private static boolean hasChild(Element parent, String localName) {
		return firstChild(parent, localName) != null;
	}

	private static Element firstChild(Element parent, String localName) {
		List<Element> matches = children(parent, localName);
		return matches.isEmpty() ? null : matches.get(0);
	}

	private static List<Element> children(Element parent, String localName) {
		List<Element> matches = new ArrayList<>();
		NodeList nodes = parent.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element element && localName.equalsIgnoreCase(localName(element))) {
				matches.add(element);
			}
		}
		return matches;
	}

	private static String firstChildText(Element parent, String localName) {
		Element child = firstChild(parent, localName);
		return child != null ? text(child) : "";
	}

	private static String text(Element element) {
		String value = element.getTextContent();
		return value != null ? value.trim() : "";
	}

	private static String localName(Element element) {
		String name = element.getLocalName();
		return name != null && !name.isBlank() ? name : element.getTagName();
	}

	private static String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first : (second != null ? second : "");
	}
}

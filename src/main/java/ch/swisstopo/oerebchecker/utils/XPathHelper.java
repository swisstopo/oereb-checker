package ch.swisstopo.oerebchecker.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import software.amazon.awssdk.utils.StringUtils;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XPathHelper {
    protected static final Logger logger = LoggerFactory.getLogger(XPathHelper.class);

    private final XPathFactory factory;
    private final NamespaceContext namespaceContext = new OeREBNamespaceContext();
    private final Map<String, XPathExpression> expressionCache = new ConcurrentHashMap<>();
    private final DocumentBuilderFactory documentBuilderFactory;

    public XPathHelper() {
        this.factory = XPathFactory.newInstance();
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilderFactory.setNamespaceAware(true);
        logger.trace("XPathHelper initialized. NamespaceAware: {}", this.documentBuilderFactory.isNamespaceAware());
    }

    private XPath getThreadSafeXPath() {
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(namespaceContext);
        return xpath;
    }

    public Document parseDocument(InputStream is) throws Exception {
        logger.trace("Starting DOM parsing of input stream.");
        return documentBuilderFactory.newDocumentBuilder().parse(new org.xml.sax.InputSource(is));
    }

    private XPathExpression getCompiledExpression(String expression) throws XPathExpressionException {
        try {
            return expressionCache.computeIfAbsent(expression, expr -> {
                try {
                    logger.trace("XPath cache miss. Compiling expression: {}", expr);
                    return getThreadSafeXPath().compile(expr);
                } catch (XPathExpressionException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof XPathExpressionException) {
                throw (XPathExpressionException) e.getCause();
            }
            throw e;
        }
    }

    public String getString(Object item, String expression) throws XPathExpressionException {
        return getCompiledExpression(expression).evaluate(item);
    }

    public Node getNode(Object item, String expression) throws XPathExpressionException {
        return (Node) getCompiledExpression(expression).evaluate(item, XPathConstants.NODE);
    }

    public NodeList getNodes(Object item, String expression) throws XPathExpressionException {
        return (NodeList) getCompiledExpression(expression).evaluate(item, XPathConstants.NODESET);
    }

    public List<Node> getNodesList(Object item, String expression) throws XPathExpressionException {
        NodeList nodes = getNodes(item, expression);
        List<Node> list = new ArrayList<>();
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                list.add(nodes.item(i));
            }
        }
        return list;
    }

    public List<Node> filterBySuffix(NodeList nodes, String suffix) {
        List<Node> filtered = new ArrayList<>();
        if (nodes == null) return filtered;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (getSafeLocalName(node).endsWith(suffix)) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    public int getCount(Object item, String expression) throws XPathExpressionException {
        NodeList nodes = getNodes(item, expression);
        return nodes != null ? nodes.getLength() : 0;
    }

    public String getPath(Node node) {
        if (node == null) return "";
        Node parent = node.getParentNode();

        if (parent == null || parent.getNodeType() == Node.DOCUMENT_NODE) {
            logger.trace("XPath path resolution reached document root.");
            return "";
        }

        String displayText = "";
        String nodeName = node.getNodeName();
        String localName = getSafeLocalName(node);

        if (localName.endsWith("RealEstate")) {
            Node egrid = getFirstChildNode(node, "EGRID");
            if (egrid != null) {
                displayText = "EGRID:" + egrid.getTextContent();
            }
        } else if (localName.endsWith("RestrictionOnLandownership")) {
            Node typeCode = getFirstChildNode(node, "TypeCode");
            if (typeCode != null) {
                displayText = "TypeCode:" + typeCode.getTextContent();
            }
        } else if (localName.endsWith("OtherLegend") || localName.endsWith("LegendEntry")) {
            Node text = getFirstChildNode(node, "Text");
            if (text != null) {
                Node localisedText = getFirstChildNode(text, "LocalisedText");
                if (localisedText == null) {
                    displayText = "Text:" + text.getTextContent();
                } else {
                    Node langNode = getFirstChildNode(localisedText, "Language");
                    Node textNode = getFirstChildNode(localisedText, "Text");
                    displayText = "Lang:" + langNode.getTextContent() + " Text:" + textNode.getTextContent();
                }
            }
        }

        String pathPart = "/" + nodeName;
        if (StringUtils.isNotBlank(displayText)) {
            pathPart += "[" + displayText + "]";
        }

        logger.trace("Resolved path part: {}", pathPart);
        String parentPath = getPath(parent);
        return parentPath + pathPart;
    }

    public Node getFirstChildNode(Node node, String nodeName) {
        if (node == null) return null;
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (getSafeLocalName(child).endsWith(nodeName)) {
                return child;
            }
        }
        return null;
    }

    public String getSafeLocalName(Node node) {
        if (node == null) return "";
        String localName = node.getLocalName();
        if (localName != null) return localName;

        // Fallback for non-namespace aware parsing: strip prefix manually
        String nodeName = node.getNodeName();
        int idx = nodeName.indexOf(':');
        return idx >= 0 ? nodeName.substring(idx + 1) : nodeName;
    }

    private static class OeREBNamespaceContext implements NamespaceContext {
        private final Map<String, String> namespaces = Map.of(
                "v", "http://schemas.geo.admin.ch/V_D/OeREB/1.0/Versioning",
                "e", "http://schemas.geo.admin.ch/V_D/OeREB/2.0/Extract",
                "ed", "http://schemas.geo.admin.ch/V_D/OeREB/2.0/ExtractData",
                "geo", "http://www.interlis.ch/geometry/1.0"
        );

        @Override
        public String getNamespaceURI(String prefix) {
            return namespaces.get(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }
    }
}
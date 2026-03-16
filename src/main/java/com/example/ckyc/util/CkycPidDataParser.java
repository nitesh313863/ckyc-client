package com.example.ckyc.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CkycPidDataParser {

    private CkycPidDataParser() {
    }

    public static ParsedPidData parse(String decryptedPidData) {
        ParsedPidData parsed = new ParsedPidData();
        if (decryptedPidData == null || decryptedPidData.isBlank()) {
            return parsed;
        }
        try {
            String wrapped = "<ROOT>" + decryptedPidData + "</ROOT>";
            Document doc = XmlHelper.parseFragment(wrapped);
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                Object value = toObject(element, parsed.flat);
                addStructured(parsed.structured, element.getTagName(), value);
            }
            parsed.flat.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isBlank());
            return parsed;
        } catch (Exception ex) {
            return parsed;
        }
    }

    private static Object toObject(Element element, Map<String, String> flat) {
        NodeList childNodes = element.getChildNodes();
        List<Element> childElements = new ArrayList<>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element) node);
            }
        }
        if (childElements.isEmpty()) {
            String value = element.getTextContent();
            if (value != null) {
                value = value.trim();
            }
            if (value != null && !value.isBlank()) {
                flat.putIfAbsent(element.getTagName(), value);
            }
            return value;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (Element child : childElements) {
            Object value = toObject(child, flat);
            addStructured(map, child.getTagName(), value);
        }
        return map;
    }

    private static void addStructured(Map<String, Object> target, String key, Object value) {
        if (!target.containsKey(key)) {
            target.put(key, value);
            return;
        }
        Object existing = target.get(key);
        if (existing instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) existing;
            list.add(value);
            return;
        }
        List<Object> list = new ArrayList<>();
        list.add(existing);
        list.add(value);
        target.put(key, list);
    }

    public static final class ParsedPidData {
        private final Map<String, Object> structured = new LinkedHashMap<>();
        private final Map<String, String> flat = new LinkedHashMap<>();

        public Map<String, Object> getStructured() {
            return structured;
        }

        public Map<String, String> getFlat() {
            return flat;
        }
    }
}

package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowConverter {

    @XmlRootElement(name = "zflow")
    public static class ZFlow {
        @XmlElement(name = "config")
        public List<Config> configs;
        @XmlElement(name = "node")
        public List<Node> nodes;
    }

    @XmlRootElement(name = "config")
    public static class Config {
        @XmlAttribute
        public String name;
        @XmlAttribute
        public String value;
    }

    @XmlRootElement(name = "node")
    public static class Node {
        @XmlAttribute
        public String name;
        @XmlAttribute
        public String id;
        @XmlAttribute
        public String x;
        @XmlAttribute
        public String y;
        @XmlElement(name = "line")
        public List<Line> lines;
    }

    @XmlRootElement(name = "line")
    public static class Line {
        @XmlAttribute
        public String name;
        @XmlAttribute
        public String id;
        @XmlAttribute
        public String target;
    }

    public static class JsonFlow {
        public Map<String, Object> areas;
        public int initNum;
        public Map<String, JsonLine> lines;
        public Map<String, JsonNode> nodes;
        public String title;
    }

    public static class JsonLine {
        public boolean alt;
        public String from;
        public String name;
        public String to;
        public String type;
    }

    public static class JsonNode {
        public boolean alt;
        public int height;
        public int left;
        public String name;
        public int top;
        public String type;
        public int width;
    }

    public static String xmlToJson(String xml) throws Exception {
        JAXBContext context = JAXBContext.newInstance(ZFlow.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ZFlow zflow = (ZFlow) unmarshaller.unmarshal(new StringReader(xml));

        JsonFlow jsonFlow = new JsonFlow();
        jsonFlow.areas = new HashMap<>();
        jsonFlow.initNum = 100;
        jsonFlow.lines = new HashMap<>();
        jsonFlow.nodes = new HashMap<>();
        jsonFlow.title = "newFlow_1";

        for (int i = 0; i < zflow.nodes.size(); i++) {
            Node node = zflow.nodes.get(i);
            JsonNode jsonNode = new JsonNode();
            jsonNode.alt = true;
            jsonNode.height = 24;
            jsonNode.left = Integer.parseInt(node.x);
            jsonNode.name = node.name;
            jsonNode.top = Integer.parseInt(node.y);
            jsonNode.type = "start round";
            jsonNode.width = 24;
            jsonFlow.nodes.put("node_" + (i + 1), jsonNode);

            if (node.lines != null) {
                for (int j = 0; j < node.lines.size(); j++) {
                    Line line = node.lines.get(j);
                    JsonLine jsonLine = new JsonLine();
                    jsonLine.alt = true;
                    jsonLine.from = "node_" + (i + 1);
                    jsonLine.name = line.name;
                    jsonLine.to = "node_" + (Integer.parseInt(line.target.replaceAll("\\D", "")) + 1);
                    jsonLine.type = "sl";
                    jsonFlow.lines.put("line_" + (jsonFlow.lines.size() + 1), jsonLine);
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonFlow);
    }

    public static String jsonToXml(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonFlow jsonFlow = mapper.readValue(json, JsonFlow.class);

        ZFlow zflow = new ZFlow();
        zflow.configs = new ArrayList<>();
        zflow.nodes = new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : jsonFlow.nodes.entrySet()) {
            JsonNode jsonNode = entry.getValue();
            Node node = new Node();
            node.name = jsonNode.name;
            node.id = entry.getKey();
            node.x = String.valueOf(jsonNode.left);
            node.y = String.valueOf(jsonNode.top);
            node.lines = new ArrayList<>();

            for (Map.Entry<String, JsonLine> lineEntry : jsonFlow.lines.entrySet()) {
                JsonLine jsonLine = lineEntry.getValue();
                if (jsonLine.from.equals(entry.getKey())) {
                    Line line = new Line();
                    line.name = jsonLine.name;
                    line.id = lineEntry.getKey();
                    line.target = jsonLine.to.replaceAll("node_", "Node");
                    node.lines.add(line);
                }
            }

            zflow.nodes.add(node);
        }

        JAXBContext context = JAXBContext.newInstance(ZFlow.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter writer = new StringWriter();
        marshaller.marshal(zflow, writer);
        return writer.toString();
    }
    private static String readFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static void main(String[] args) {
        try {
            String xml = readFile("C:\\work\\temp\\test.xml"); // Your XML string here
            String json = xmlToJson(xml);
            System.out.println("XML to JSON:");
            System.out.println(json);

//            String json = readFile("C:\\work\\temp\\test.json");
//            String convertedXml = jsonToXml(json);
//            System.out.println("\nJSON back to XML:");
//            System.out.println(convertedXml);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package com.zgamelogic.discord.services;

import com.zgamelogic.discord.data.Model;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.data.SerializableData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class IronWood {
    private final Map<String, Element> documents;

    public IronWood(@Value("${ironwood.directory:ironwood}") String directory, ResourcePatternResolver resourcePatternResolver) throws IOException {
        documents = new HashMap<>();
        loadDocuments(directory, resourcePatternResolver);
    }

    public <T extends SerializableData> T generate(String document, Model model) {
        Element root = documents.get(document);
        return switch (root.getTagName()) {
            case "embed" -> (T) generateEmbed(root, model);
            case "component" -> (T) generateComponent(root, model);
            case "modal" -> (T) generateModal(root, model);
            default -> {
                log.warn("Unknown IronWood document type: {}", root.getTagName());
                yield null;
            }
        };
    }

    private MessageEmbed generateEmbed(Element root, Model model) {
        EmbedBuilder eb = new EmbedBuilder();
        String colorString = parseInput(root.getAttribute("color"), model);
        if(!colorString.isEmpty())
            eb.setColor(Color.decode(colorString));
        eb.setTitle("", "");
        eb.setAuthor("", "", "");
        eb.setColor(null);
        eb.setFooter("", "");
        eb.setDescription("");
        eb.setThumbnail("");
        eb.setImage("");
        eb.addField("", "", false);
        return eb.build();
    }

    public Component generateComponent(Element root, Model model) { return null; }

    public Modal generateModal(Element root, Model model) {
        String id = parseInput(root.getAttribute("id"), model);
        String title = parseInput(root.getAttribute("title"), model);
        Modal.Builder modal = Modal.create(id, title);
        NodeList children = root.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE) continue;
            String textId = parseInput(((Element)child).getAttribute("id"), model);
            String textLabel = parseInput(((Element)child).getAttribute("label"), model);
            TextInputStyle textStyle = ((Element)child).getAttribute("style").toLowerCase().trim().equals("paragraph") ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT;
            String textRequired = parseInput(((Element)child).getAttribute("required"), model);
            String textMinLength = parseInput(((Element)child).getAttribute("min-length"), model);
            String textMaxLength = parseInput(((Element)child).getAttribute("max-length"), model);
            String textValue = parseInput(((Element)child).getAttribute("value"), model);
            TextInput.Builder textBuilder = TextInput.create(textId, textLabel, textStyle);
            if(!textRequired.isEmpty())
                textBuilder.setRequired(Boolean.parseBoolean(textRequired));
            if(!textMinLength.isEmpty())
                textBuilder.setMinLength(Integer.parseInt(textMinLength));
            if(!textMaxLength.isEmpty())
                textBuilder.setMaxLength(Integer.parseInt(textMaxLength));
            if(!textValue.isEmpty())
                textBuilder.setValue(textValue);
            modal.addComponents(ActionRow.of(textBuilder.build()));
        }
        return modal.build();
    }

    private String parseInput(String input, Model model) {
        if(input == null || input.isEmpty()) return "";
        try {
            Set<String> result = new HashSet<>();
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
            Matcher matcher = pattern.matcher(input);
            while (matcher.find()) result.add(matcher.group(1));
            for (String key : result) {
                input = input.replaceAll("\\$\\{" + Pattern.quote(key) + "}", model.resolveKey(key));
            }
            return input;
        } catch (Exception e) {
            return input;
        }
    }

    private void loadDocuments(String directory, ResourcePatternResolver resourcePatternResolver) throws IOException {
        Arrays.stream(resourcePatternResolver.getResources("classpath:" + directory + "/*")).forEach(resource -> {
            String filename = resource.getFilename();
            if(filename == null) return;
            filename = filename.replace(".xml", "");
            try {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resource.getInputStream());
                doc.getDocumentElement().normalize();
                Element root = doc.getDocumentElement();
                documents.put(filename, root);
                log.info("Registered IronWood document: {}", filename);
            } catch (SAXException | IOException | ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

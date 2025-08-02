package com.zgamelogic.discord.services;

import com.zgamelogic.discord.data.Model;
import jakarta.annotation.PostConstruct;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    @PostConstruct
    private void init(){
        Model m = new Model();
        m.addContext("guy 1", "Ben");
        generate("default", m);
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

    private MessageEmbed generateEmbed(Element root, Model model) { return null; }

    public Component generateComponent(Element root, Model model) { return null; }

    public Modal generateModal(Element root, Model model) {
        String id = parseInput(root.getAttribute("id"), model);
        String title = parseInput(root.getAttribute("title"), model);
        Modal.Builder modal = Modal.create(id, title);
        // TODO keep going ben
//        modal.addComponents(ActionRow.of(TextInput.create("input", "", TextInputStyle.SHORT).build()));
        return modal.build();
    }

    private String parseInput(String input, Model model) {
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

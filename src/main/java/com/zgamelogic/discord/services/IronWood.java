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
import org.w3c.dom.*;
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

    public <T extends SerializableData> T generate(String document, Model model) throws NoSuchFieldException, IllegalAccessException {
        Element root = documents.get(document);
        NodeList forList = root.getElementsByTagName("for");
        for(int i = 0; i < forList.getLength(); i++){
            Element element = (Element) forList.item(i);
            String collectionName = element.getAttribute("values").replace("${", "").replace("}", "");
            Collection<?> collection = model.resolveCollection(collectionName);
            for(int k = 0; k < collection.size(); k++){
                NodeList forChildNodes = element.getChildNodes();
                for(int j = forChildNodes.getLength() - 1; j >= 0; j--) {
                    Node node = forChildNodes.item(j);
                    root.insertBefore(node, element);
                }
            }
            root.removeChild(element);
        }
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

    private void replaceWithIndex(Node node, String keyName, int index){
        if(node.hasChildNodes()){
            for(int i = 0; i < node.getChildNodes().getLength(); i++){
                Node child = node.getChildNodes().item(i);
                replaceWithIndex(child, keyName, index);
            }
        } else {
            // TODO set child node strings to the right thing
            // ${i} - current index -> index
            // $[i].field - resolve index.field -> collection.get(index).field
            // $[i] - resolve index -> collection.get(index)
        }
    }

    private MessageEmbed generateEmbed(Element root, Model model) {
        EmbedBuilder eb = new EmbedBuilder();
        String colorString = parseInput(root.getAttribute("color"), model);
        if(!colorString.isEmpty())
            eb.setColor(Color.decode(colorString));
        Optional.ofNullable(root.getElementsByTagName("title").item(0)).ifPresent(titleNode -> {
            String title = parseInput(titleNode.getTextContent(), model);
            String url = parseInput(((Element)titleNode).getAttribute("url"), model);
            if(!title.isEmpty())
                eb.setTitle(title, url.isEmpty() ? null : url);
        });
        Optional.ofNullable(root.getElementsByTagName("description").item(0)).ifPresent(descriptionNode -> {
            String description = parseInput(descriptionNode.getTextContent(), model);
            if(!description.isEmpty())
                eb.setDescription(description);
        });
        Optional.ofNullable(root.getElementsByTagName("author").item(0)).ifPresent(authorNode -> {
            String author = parseInput(authorNode.getTextContent(), model);
            String url = parseInput(((Element)authorNode).getAttribute("url"), model);
            String iconUrl = parseInput(((Element)authorNode).getAttribute("iconUrl"), model);
            if(!author.isEmpty())
                eb.setAuthor(
                    author,
                    url.isEmpty() ? null : url,
                    iconUrl.isEmpty() ? null : iconUrl
                );
        });
        Optional.ofNullable(root.getElementsByTagName("footer").item(0)).ifPresent(footerNode -> {
            String footer = parseInput(footerNode.getTextContent(), model);
            String iconUrl = parseInput(((Element)footerNode).getAttribute("iconUrl"), model);
            if(!footer.isEmpty())
                eb.setFooter(
                    footer,
                    iconUrl.isEmpty() ? null : iconUrl
                );
        });
        Optional.ofNullable(root.getElementsByTagName("description").item(0)).ifPresent(descNode -> {
            String desc = parseInput(descNode.getTextContent(), model);
            if(!desc.isEmpty())
                eb.setDescription(desc);
        });
        Optional.ofNullable(root.getElementsByTagName("thumbnail").item(0)).ifPresent(thumbnailNode -> {
            String url = parseInput(((Element)thumbnailNode).getAttribute("url"), model);
            if(!url.isEmpty())
                eb.setThumbnail(url);
        });
        Optional.ofNullable(root.getElementsByTagName("image").item(0)).ifPresent(imageNode -> {
            String url = parseInput(((Element)imageNode).getAttribute("url"), model);
            if(!url.isEmpty())
                eb.setImage(url);
        });
        NodeList fields = root.getElementsByTagName("field");
        for(int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String name = parseInput(field.getAttribute("name"), model);
            String value = parseInput(field.getTextContent(), model);
            String inlineString = parseInput(field.getAttribute("inline"), model);
            boolean inline = !inlineString.isEmpty() && Boolean.parseBoolean(inlineString);
            eb.addField(name, value, inline);
        }
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

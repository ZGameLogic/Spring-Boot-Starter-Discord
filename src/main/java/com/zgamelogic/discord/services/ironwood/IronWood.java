package com.zgamelogic.discord.services.ironwood;

import com.zgamelogic.discord.annotations.mappings.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.dv8tion.jda.api.utils.messages.MessagePollBuilder;
import net.dv8tion.jda.api.utils.messages.MessagePollData;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IronWood {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IronWood.class);
    private final Map<String, String> documents;

    public IronWood(@Value("${ironwood.directory:ironwood}") String directory, ResourcePatternResolver resourcePatternResolver) throws IOException {
        documents = new HashMap<>();
        loadDocuments(directory, resourcePatternResolver);
    }

    public void replyToEvent(String documentName, Annotation ann, Method method, Model model, GenericEvent event) {
        documentName = extractDocument(documentName, ann, method);
        if(documentName == null || documentName.isEmpty()) return;
        try {
            SerializableData messageData = generate(documentName, model);
            if(((IReplyCallback) event).isAcknowledged()) {
                switch(messageData) {
                    case MessageEmbed embed -> ((IReplyCallback) event).getHook().sendMessageEmbeds(embed).addFiles(model.getFileUploads()).queue();
                    case MessagePollData pollData -> ((IReplyCallback) event).getHook().sendMessagePoll(pollData).addFiles(model.getFileUploads()).queue();
                    default -> log.warn("Unknown message data type: {}", messageData.getClass().getName());
                }
            } else {
                switch(messageData) {
                    case MessageEmbed embed -> ((IReplyCallback) event).replyEmbeds(embed).addFiles(model.getFileUploads()).queue();
                    case Modal modal -> ((IModalCallback) event).replyModal(modal).queue();
                    case MessagePollData pollData -> ((IReplyCallback) event).replyPoll(pollData).addFiles(model.getFileUploads()).queue();
                    default -> log.warn("Unknown message data type: {}", messageData.getClass().getName());
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractDocument(String documentName, Annotation ann, Method method) {
        if(documentName == null) {
            if(method.isAnnotationPresent(com.zgamelogic.discord.annotations.mappings.Document.class))
                ann = method.getAnnotation(com.zgamelogic.discord.annotations.mappings.Document.class);
            switch(ann) {
                case com.zgamelogic.discord.annotations.mappings.Document mapping -> documentName = mapping.value();
                case ButtonMapping mapping -> documentName = mapping.document();
                case EntitySelectMapping mapping -> documentName = mapping.document();
                case GenericDiscordMapping mapping -> documentName = mapping.document();
                case MessageContextMapping mapping -> documentName = mapping.document();
                case ModalMapping mapping -> documentName = mapping.document();
                case SlashCommandMapping mapping -> documentName = mapping.document();
                case StringSelectMapping mapping -> documentName = mapping.document();
                default -> {}
            }
        }
        return documentName;
    }

    public SerializableData generate(String documentName, Model model) throws ParserConfigurationException, IOException, SAXException {
        String document = documents.get(documentName);
        document = flattenFor(document, model);
        document = parseInput(document, model);
        /*
        Possible ideas to make this better
        resolve emoji references? likes ${em:emoji_name} will turn into the mentionable?
         */
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        return switch (root.getTagName()) {
            case "embed" -> generateEmbed(root);
//            case "component" -> generateComponent(root);
            case "modal" -> generateModal(root);
            case "poll" -> generatePoll(root);
            default -> {
                log.warn("Unknown IronWood document type: {}", root.getTagName());
                yield null;
            }
        };
    }

    public Component generateComponent(Element root) { return null; }

    private MessagePollData generatePoll(Element root) {
        String title = root.getAttribute("title");
        MessagePollBuilder pb = new MessagePollBuilder(title);
        // TODO duration
//        String duration = root.getAttribute("duration");
//        if(!duration.isEmpty()) pb.setDuration(Duration.ofMinutes(30));
        NodeList children = root.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            if(!child.getNodeName().equals("answer")) continue;
            String answer = child.getTextContent();
            pb.addAnswer(answer);
        }
        return pb.build();
    }

    private MessageEmbed generateEmbed(Element root) {
        EmbedBuilder eb = new EmbedBuilder();
        String colorString = root.getAttribute("color");
        if(!colorString.isEmpty())
            eb.setColor(Color.decode(colorString));
        Optional.ofNullable(root.getElementsByTagName("title").item(0)).ifPresent(titleNode -> {
            String title = titleNode.getTextContent();
            String url = ((Element)titleNode).getAttribute("url");
            if(!title.isEmpty())
                eb.setTitle(title, url.isEmpty() ? null : url);
        });
        Optional.ofNullable(root.getElementsByTagName("description").item(0)).ifPresent(descriptionNode -> {
            String description = descriptionNode.getTextContent();
            if(!description.isEmpty())
                eb.setDescription(description);
        });
        Optional.ofNullable(root.getElementsByTagName("author").item(0)).ifPresent(authorNode -> {
            String author = authorNode.getTextContent();
            String url = ((Element)authorNode).getAttribute("url");
            String iconUrl = ((Element)authorNode).getAttribute("iconUrl");
            if(!author.isEmpty())
                eb.setAuthor(
                    author,
                    url.isEmpty() ? null : url,
                    iconUrl.isEmpty() ? null : iconUrl
                );
        });
        Optional.ofNullable(root.getElementsByTagName("footer").item(0)).ifPresent(footerNode -> {
            String footer = footerNode.getTextContent();
            String iconUrl = ((Element)footerNode).getAttribute("iconUrl");
            if(!footer.isEmpty())
                eb.setFooter(
                    footer,
                    iconUrl.isEmpty() ? null : iconUrl
                );
        });
        Optional.ofNullable(root.getElementsByTagName("description").item(0)).ifPresent(descNode -> {
            String desc = descNode.getTextContent();
            if(!desc.isEmpty())
                eb.setDescription(desc);
        });
        Optional.ofNullable(root.getElementsByTagName("thumbnail").item(0)).ifPresent(thumbnailNode -> {
            String url = ((Element)thumbnailNode).getAttribute("url");
            if(!url.isEmpty())
                eb.setThumbnail(url);
        });
        Optional.ofNullable(root.getElementsByTagName("image").item(0)).ifPresent(imageNode -> {
            String url = ((Element)imageNode).getAttribute("url");
            if(!url.isEmpty())
                eb.setImage(url);
        });
        NodeList fields = root.getElementsByTagName("field");
        for(int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String name = field.getAttribute("name");
            String value = field.getTextContent();
            String inlineString = field.getAttribute("inline");
            boolean inline = !inlineString.isEmpty() && Boolean.parseBoolean(inlineString);
            eb.addField(name, value, inline);
        }
        return eb.build();
    }

    public Modal generateModal(Element root) {
        String id = root.getAttribute("id");
        String title = root.getAttribute("title");
        Modal.Builder modal = Modal.create(id, title);
        NodeList children = root.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE) continue;
            String textId = ((Element)child).getAttribute("id");
            String textLabel = ((Element)child).getAttribute("label");
            TextInputStyle textStyle = ((Element)child).getAttribute("style").toLowerCase().trim().equals("paragraph") ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT;
            String textRequired = ((Element)child).getAttribute("required");
            String textMinLength = ((Element)child).getAttribute("min-length");
            String textMaxLength = ((Element)child).getAttribute("max-length");
            String textValue = ((Element)child).getAttribute("value");
            TextInput.Builder textBuilder = TextInput.create(textId, textStyle);
            if(!textRequired.isEmpty())
                textBuilder.setRequired(Boolean.parseBoolean(textRequired));
            if(!textMinLength.isEmpty())
                textBuilder.setMinLength(Integer.parseInt(textMinLength));
            if(!textMaxLength.isEmpty())
                textBuilder.setMaxLength(Integer.parseInt(textMaxLength));
            if(!textValue.isEmpty())
                textBuilder.setValue(textValue);
            modal.addComponents(Label.of(textLabel, textBuilder.build()));
        }
        return modal.build();
    }

    private String parseInput(String input, Model model) {
        if (input == null || input.isEmpty()) return "";
        try {
            Set<String> result = new HashSet<>();
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
            Matcher matcher = pattern.matcher(input);
            while (matcher.find()) result.add(matcher.group(1));
            for (String key : result) {
                input = input.replaceAll("\\$\\{" + Pattern.quote(key) + "}", Matcher.quoteReplacement(model.resolveKey(key)));
            }
            return input;
        } catch (Exception e) {
            return input;
        }
    }

    private String flattenFor(String input, Model model) {
        Pattern forPattern = Pattern.compile("<for\\s+collection=\"([^\"]+)\">(.*?)</for>", Pattern.DOTALL);
        Matcher forMatcher = forPattern.matcher(input);
        StringBuilder result = new StringBuilder();

        while (forMatcher.find()) {
            String collectionName = forMatcher.group(1);
            String forContent = forMatcher.group(2);

            Collection<?> collection = model.resolveCollection(collectionName);
            StringBuilder expanded = new StringBuilder();

            for (int i = 0; i < collection.size(); i++) {
                String itemContent = forContent
                    .replace("$[i]", String.valueOf(i))
                    .replaceAll("\\$\\{i\\.([a-zA-Z0-9_]+)}", "\\$\\{" + collectionName + "[" + i + "].$1}")
                    .replaceAll("\\$\\{i}", "\\$\\{" + collectionName + "[" + i + "]}");
                expanded.append(itemContent);
            }

            forMatcher.appendReplacement(result, Matcher.quoteReplacement(expanded.toString()));
        }
        forMatcher.appendTail(result);
        return result.toString();
    }

    private void loadDocuments(String directory, ResourcePatternResolver resourcePatternResolver) throws IOException {
        try {
            Arrays.stream(resourcePatternResolver.getResources("classpath:" + directory + "/*")).forEach(resource -> {
                String filename = resource.getFilename();
                if (filename == null) return;
                filename = filename.replace(".xml", "");
                try {
                    String document = new String(resource.getInputStream().readAllBytes());
                    documents.put(filename, document);
                    log.info("Registered IronWood document: {}", filename);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (FileNotFoundException e) {
            log.debug("No Ironwood directory found");
        }
    }
}

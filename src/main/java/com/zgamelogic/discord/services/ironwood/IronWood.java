package com.zgamelogic.discord.services.ironwood;

import com.zgamelogic.discord.annotations.mappings.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.checkboxgroup.CheckboxGroup;
import net.dv8tion.jda.api.components.checkboxgroup.CheckboxGroupOption;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.radiogroup.RadioGroup;
import net.dv8tion.jda.api.components.radiogroup.RadioGroupOption;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessagePollBuilder;
import net.dv8tion.jda.api.utils.messages.MessagePollData;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

@Service
public class IronWood {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IronWood.class);
    private final TemplateEngine templateEngine;

    public IronWood(@Value("${ironwood.directory:ironwood}") String directory) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setPrefix(directory + "/");
        resolver.setSuffix(".xml");
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        this.templateEngine = engine;
    }

    public void replyToEvent(String documentName, Annotation ann, Method method, Model model, GenericEvent event) {
        documentName = extractDocument(documentName, ann, method);
        if(documentName == null || documentName.isEmpty()) return;
        try {
            Object messageData = generate(documentName, model);
            if(((IReplyCallback) event).isAcknowledged()) {
                switch(messageData) {
                    case MessageEmbed embed -> ((IReplyCallback) event).getHook().sendMessageEmbeds(embed).addFiles(model.getFileUploads()).queue();
                    case MessagePollData pollData -> ((IReplyCallback) event).getHook().sendMessagePoll(pollData).addFiles(model.getFileUploads()).queue();
                    case Container component -> ((IReplyCallback) event).getHook().sendMessageComponents(component).useComponentsV2().queue();
                    default -> log.warn("Unknown message data type: {}", messageData.getClass().getName());
                }
            } else {
                switch(messageData) {
                    case MessageEmbed embed -> ((IReplyCallback) event).replyEmbeds(embed).addFiles(model.getFileUploads()).queue();
                    case Modal modal -> ((IModalCallback) event).replyModal(modal).queue();
                    case MessagePollData pollData -> ((IReplyCallback) event).replyPoll(pollData).addFiles(model.getFileUploads()).queue();
                    case Container component -> ((IReplyCallback) event).replyComponents(component).useComponentsV2().queue();
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

    public Object generate(String documentName, Model model) throws ParserConfigurationException, IOException, SAXException {
        String document = templateEngine.process(documentName, model.getContext());
        /*
        Possible ideas to make this better
        resolve emoji references? likes ${em:emoji_name} will turn into the mentionable?
         */
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        return switch (root.getTagName()) {
            case "embed" -> generateEmbed(root);
            case "component" -> generateComponent(root);
            case "modal" -> generateModal(root);
            case "poll" -> generatePoll(root);
            default -> {
                log.warn("Unknown IronWood document type: {}", root.getTagName());
                yield null;
            }
        };
    }

    public Container generateComponent(Element root) {
        // TODO implement
        return null;
    }

    private MessagePollData generatePoll(Element root) {
        String title = root.getAttribute("title");
        MessagePollBuilder pb = new MessagePollBuilder(title);
        boolean multiAnswer = Boolean.parseBoolean(root.getAttribute("multiAnswer"));
        pb.setMultiAnswer(multiAnswer);
        String duration = root.getAttribute("duration");
        String unit = root.getAttribute("duration-unit");
        if(!duration.isEmpty()) {
            long dur = Long.parseLong(duration);
            ChronoUnit cUnit = unit.isEmpty() ? ChronoUnit.DAYS : ChronoUnit.valueOf(unit.toUpperCase());
            pb.setDuration(Duration.of(dur, cUnit));
        }
        NodeList children = root.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            if(!child.getNodeName().equals("option")) continue;
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
        // TODO add in file upload
        String id = root.getAttribute("id");
        String title = root.getAttribute("title");
        Modal.Builder modal = Modal.create(id, title);
        NodeList children = root.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE) continue;
            switch (child.getNodeName()) {
                case "display" -> {
                    TextDisplay display = TextDisplay.of(child.getTextContent());
                    modal.addComponents(display);
                }
                case "radio-group" -> {
                    String groupId = ((Element)child).getAttribute("id");
                    String label = ((Element)child).getAttribute("label");
                    String labelDesc = ((Element)child).getAttribute("label-desc");
                    RadioGroup.Builder group = RadioGroup.create(groupId);
                    String requiredString = ((Element)child).getAttribute("required");
                    if(!requiredString.isEmpty()) group.setRequired(Boolean.parseBoolean(requiredString));
                    NodeList options = child.getChildNodes();
                    for(int j = 0; j < options.getLength(); j++) {
                        Node optionNode = options.item(j);
                        if(!optionNode.getNodeName().equals("radio")) continue;
                        String bLabel = ((Element)optionNode).getAttribute("label");
                        String bValue = ((Element)optionNode).getAttribute("value");
                        String bDesc = ((Element)optionNode).getAttribute("description");
                        boolean bDefault = Boolean.parseBoolean(((Element)optionNode).getAttribute("default"));
                        group.addOptions(RadioGroupOption.of(bLabel, bValue, bDesc.isEmpty() ? null : bDesc, bDefault));
                    }
                    modal.addComponents(Label.of(label, labelDesc.isEmpty() ? null : labelDesc, group.build()));
                }
                case "checkbox-group" -> {
                    String groupId = ((Element)child).getAttribute("id");
                    String label = ((Element)child).getAttribute("label");
                    String labelDesc = ((Element)child).getAttribute("label-desc");
                    CheckboxGroup.Builder group = CheckboxGroup.create(groupId);
                    String maxString = ((Element)child).getAttribute("max");
                    String minString = ((Element)child).getAttribute("min");
                    String requiredString = ((Element)child).getAttribute("required");
                    if(!maxString.isEmpty()) group.setMaxValues(Integer.parseInt(maxString));
                    if(!minString.isEmpty()) group.setMinValues(Integer.parseInt(minString));
                    if(!requiredString.isEmpty()) group.setRequired(Boolean.parseBoolean(requiredString));
                    NodeList options = child.getChildNodes();
                    for(int j = 0; j < options.getLength(); j++) {
                        Node optionNode = options.item(j);
                        if(!optionNode.getNodeName().equals("checkbox")) continue;
                        String bLabel = ((Element)optionNode).getAttribute("label");
                        String bValue = ((Element)optionNode).getAttribute("value");
                        String bDesc = ((Element)optionNode).getAttribute("description");
                        boolean checked = Boolean.parseBoolean(((Element)optionNode).getAttribute("checked"));
                        group.addOptions(CheckboxGroupOption.of(bLabel, bValue, bDesc.isEmpty() ? null : bDesc, checked));
                    }
                    modal.addComponents(Label.of(label, labelDesc.isEmpty() ? null : labelDesc, group.build()));
                }
                case "select" -> {
                    String textLabelDesc = ((Element)child).getAttribute("label-desc");
                    String menuLabel = ((Element) child).getAttribute("label");
                    modal.addComponents(Label.of(menuLabel, textLabelDesc.isEmpty() ? null : textLabelDesc, generateStringSelectMenu(child)));
                }
                case "entity-select" -> {
                    String textLabelDesc = ((Element)child).getAttribute("label-desc");
                    String menuLabel = ((Element) child).getAttribute("label");
                    modal.addComponents(Label.of(menuLabel, textLabelDesc.isEmpty() ? null : textLabelDesc, generateEntitySelectMenu(child)));
                }
                case "input" -> {
                    String textId = ((Element)child).getAttribute("id");
                    String textLabel = ((Element)child).getAttribute("label");
                    String textLabelDesc = ((Element)child).getAttribute("label-desc");
                    TextInputStyle textStyle = ((Element)child).getAttribute("style").toLowerCase().trim().equals("paragraph") ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT;
                    String textRequired = ((Element)child).getAttribute("required");
                    String textMinLength = ((Element)child).getAttribute("min-length");
                    String textMaxLength = ((Element)child).getAttribute("max-length");
                    String textValue = ((Element)child).getAttribute("value");
                    String textPlaceholder = ((Element)child).getAttribute("placeholder");
                    TextInput.Builder textBuilder = TextInput.create(textId, textStyle);
                    if(!textRequired.isEmpty())
                        textBuilder.setRequired(Boolean.parseBoolean(textRequired));
                    if(!textMinLength.isEmpty())
                        textBuilder.setMinLength(Integer.parseInt(textMinLength));
                    if(!textMaxLength.isEmpty())
                        textBuilder.setMaxLength(Integer.parseInt(textMaxLength));
                    if(!textValue.isEmpty())
                        textBuilder.setValue(textValue);
                    if(!textPlaceholder.isEmpty())
                        textBuilder.setPlaceholder(textPlaceholder);
                    modal.addComponents(Label.of(textLabel, textLabelDesc.isEmpty() ? null : textLabelDesc, textBuilder.build()));
                }
            }
        }
        return modal.build();
    }

    private StringSelectMenu generateStringSelectMenu(Node node){
        String id = ((Element)node).getAttribute("id");
        StringSelectMenu.Builder menu = StringSelectMenu.create(id);
        String placeholder = ((Element)node).getAttribute("placeholder");
        String maxString = ((Element)node).getAttribute("max");
        String minString = ((Element)node).getAttribute("min");
        String requiredString = ((Element)node).getAttribute("required");
        if(!maxString.isEmpty()) menu.setMaxValues(Integer.parseInt(maxString));
        if(!minString.isEmpty()) menu.setMinValues(Integer.parseInt(minString));
        if(!requiredString.isEmpty()) menu.setRequired(Boolean.parseBoolean(requiredString));
        if(!placeholder.isEmpty()) menu.setPlaceholder(placeholder);
        NodeList children = node.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE) continue;
            if(!child.getNodeName().equals("option")) continue;
            String label = ((Element)child).getAttribute("label");
            String value = ((Element)child).getAttribute("value");
            String description = ((Element)child).getAttribute("description");
            menu.addOption(label, value, description);
        }
        return menu.build();
    }

    private EntitySelectMenu generateEntitySelectMenu(Node node) {
        String id = ((Element)node).getAttribute("id");
        String targetsString = ((Element)node).getAttribute("targets");
        boolean role = targetsString.contains("role");
        boolean user = targetsString.contains("user");
        boolean channel = targetsString.contains("channel");
        String maxString = ((Element)node).getAttribute("max");
        String minString = ((Element)node).getAttribute("min");
        String requiredString = ((Element)node).getAttribute("required");
        List<EntitySelectMenu.SelectTarget> targets = new ArrayList<>();
        if(role) targets.add(EntitySelectMenu.SelectTarget.ROLE);
        if(user) targets.add(EntitySelectMenu.SelectTarget.USER);
        if(channel) targets.add(EntitySelectMenu.SelectTarget.CHANNEL);
        EntitySelectMenu.Builder menu = EntitySelectMenu.create(id, targets);
        if(!maxString.isEmpty()) menu.setMaxValues(Integer.parseInt(maxString));
        if(!minString.isEmpty()) menu.setMinValues(Integer.parseInt(minString));
        if(!requiredString.isEmpty()) menu.setRequired(Boolean.parseBoolean(requiredString));
        return menu.build();
    }
}

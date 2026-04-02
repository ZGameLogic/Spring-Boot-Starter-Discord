package com.zgamelogic.discord.services.ironwood;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
class XmlModal implements XmlDiscord {
    private String id;
    private String title;
    @JsonIgnore
    private List<Object> components = new ArrayList<>();

    @JsonProperty("display")
    public void addDisplay(XmlDisplay d){ components.add(d); }

    @JsonProperty("radio-group")
    public void setRadioGroup(){
        System.out.println("radio group");
    }
    @JsonProperty("checkbox-group")
    public void setCheckboxGroup(){
        System.out.println("checkbox group");
    }
    @JsonProperty("select")
    public void addSelect(){
        System.out.println("select");
    }

    @JsonProperty("entity-select")
    public void addEntitySelect(XmlEntitySelect e){
        System.out.println("entity select");
        components.add(e);
    }

    @JsonProperty("input")
    public void addInput(){
        System.out.println("input");
    }

    @Override
    public Object toComponent() {
        return null;
    }

    @Getter
    @ToString
    static class XmlDisplay {
        @JacksonXmlProperty(isAttribute = true)
        private String text;
        @JacksonXmlText
        private String textContent;

        public String getText() {
            return text != null ? text : textContent;
        }
    }

    @Getter
    @ToString
    static class XmlEntitySelect {
        private String id;
        private String label;
        @JsonProperty("label-desc")
        private String labelDesc;
        private String targets;
        private int max = -1;
        private int min = -1;
        private boolean required = false;
    }
}

package com.zgamelogic.discord.services.ironwood;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
class XmlPoll {
    private String title;
    private boolean multiAnswer = false;
    private long duration = 3;
    @JsonProperty("duration-unit")
    private String durationUnit = "days";
    @JsonProperty("option")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<XmlPollOption> options;

    @ToString
    static class XmlPollOption {
        @JacksonXmlProperty(isAttribute = true)
        private String text;
        @JacksonXmlText
        private String textContent;

        public String getText() {
            return text != null ? text : textContent;
        }
    }
}

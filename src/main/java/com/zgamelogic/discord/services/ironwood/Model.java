package com.zgamelogic.discord.services.ironwood;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.utils.FileUpload;
import org.thymeleaf.context.Context;

import java.util.*;

public class Model {
    private final Context context;

    private final List<FileUpload> fileUploads;
    private final List<ActionRow> actionRows;

    public Model() {
        this.context = new Context();
        this.fileUploads = new ArrayList<>();
        this.actionRows = new ArrayList<>();
    }

    public void addFileUpload(FileUpload fileUpload) {
        fileUploads.add(fileUpload);
    }

    public void addActionRow(ActionRow actionRow) {
        actionRows.add(actionRow);
    }

    public void addContext(String key, Object value) {
        context.setVariable(key, value);
    }

    public Context getContext(){
        return context;
    }

    public List<FileUpload> getFileUploads() {
        return fileUploads;
    }

    public List<ActionRow> getActionRows() {
        return actionRows;
    }
}

// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2009-2011 Google, All Rights reserved
// Copyright © 2011-2017 Massachusetts Institute of Technology, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.jsdesigner;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.Window;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.client.editor.designer.DesignerEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.DesignToolbar.DesignProject;
import com.google.appinventor.client.DesignToolbar.Screen;


public class JSDesignerPanel extends HTMLPanel {
  private FileEditor fileEditor;

  private String fileContent;

  private String designFileContent;
  private String blocksFileContent;

  public JSDesignerPanel() {
    super("<div id=\"app\"></div>");
    //ScriptInjector.fromUrl("main.9de011e5.js").inject();
    ScriptInjector.fromUrl("main.js").inject();
    //ScriptInjector.fromUrl("0.js").inject();
  }

  public void show() {
    Ode.getInstance().switchToJSDesignView();
  }

  public void loadFile(FileEditor fileEditor) {
    this.fileEditor = fileEditor;
    exportJSFunctions();

    // Get the screen 
    DesignProject project = Ode.getInstance().getDesignToolbar().getCurrentProject();
    Screen currentScreen = project.screens.get(project.currentScreen);
    FileEditor designerEditor = currentScreen.designerEditor;
    FileEditor blocksEditor = currentScreen.blocksEditor;

    Window.alert(designerEditor.getFileId());
    Window.alert(blocksEditor.getFileId());

    this.designFileContent = designerEditor.getRawFileContent();
    this.blocksFileContent = blocksEditor.getRawFileContent();

    OdeLog.log(designFileContent);
    OdeLog.log(blocksFileContent);

    this.fileContent = fileEditor.getRawFileContent();

    //openProjectInJSDesigner(fileEditor.getRawFileContent());
  }

  public native void openProjectInJSDesigner(String rawDesignFileContent, String rawBlocksFileContent)/*-{
    console.log(rawDesignFileContent);
    console.log(rawBlocksFileContent);
    var parsedJson = JSON.parse(rawDesignFileContent.replace(/^\#\|\s\$JSON/, "").replace(/\|\#$/, "").replace(/\$Name/g, "name").replace(/\$Type/g, "componentType").replace(/\$Version/g, "version").replace(/\$Components/g, "children"));
    var flattenedArray = [];

    var jsonNodes = [parsedJson.Properties];
    while(jsonNodes.length > 0) {
      var component = Object.assign({}, jsonNodes.shift());
      if (component.children !== undefined) {
        jsonNodes = jsonNodes.concat(component.children);
        component.children = component.children.map(function(child) { return child.Uuid; });
      }
      flattenedArray.push(component);
    }
    $wnd.jsDesignerLoadProject(rawDesignFileContent, rawBlocksFileContent);
  }-*/;

  private void loadProjectFromFile(){
    openProjectInJSDesigner(designFileContent, blocksFileContent); 
  };

  private String getRawFileContent() {
    return fileContent;
  }

  private String getDesignFileContent() {
    return designFileContent;
  }

  private String getBlocksFileContent() {
    return blocksFileContent;
  }

  private void switchToBlocksEditor() {
    //OdeLog.log("Switcing to Blocks Editor");
    Ode.getInstance().getDesignToolbar().publicSwitchToBlocksEditor();
  }

  private void switchToFormEditor() {
    Ode.getInstance().getDesignToolbar().publicSwitchToFormEditor();
  }

  public native void exportJSFunctions()/*-{
    $wnd.jsDesignerToJS = {
      getProjectJSON: $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getProjectJSON()).bind(this))
    };
    $wnd.jsDesignerLoadProjectFromFile = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::loadProjectFromFile()).bind(this));
    //$wnd.jsGetRawFileContent = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getRawFileContent()).bind(this));
    $wnd.jsGetDesignFileContent = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getDesignFileContent()).bind(this));
    $wnd.jsGetBlocksFileContent = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getBlocksFileContent()).bind(this));
    $wnd.jsDesignerSwitchToBlocksEditor = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::switchToBlocksEditor()).bind(this));
    $wnd.jsDesignerSwitchToFormEditor = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::switchToFormEditor()).bind(this));
  }-*/;

  public String getProjectJSON() {
    if (this.fileEditor != null) {
      //return this.fileEditor.getRawFileContent();

      //Changed this since fileEditor is overloaded with both the Designer and BlocksEditors
      return this.designFileContent;
    }
    return "{}";
  }
}

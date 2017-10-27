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


public class JSDesignerPanel extends HTMLPanel {
  private FileEditor fileEditor;

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
    openProjectInJSDesigner(fileEditor.getRawFileContent());
  }

  public native void openProjectInJSDesigner(String rawFileContent)/*-{
    var parsedJson = JSON.parse(rawFileContent.replace(/^\#\|\s\$JSON/, "").replace(/\|\#$/, "").replace(/\$Name/g, "name").replace(/\$Type/g, "componentType").replace(/\$Version/g, "version").replace(/\$Components/g, "children"));
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
    $wnd.jsDesignerLoadProject(rawFileContent);
  }-*/;

  private static void loadProjectFromFile()/*-{
    openProjectInJSDesigner(fileEditor.getRawFileContent());
  }-*/;

  public native void exportJSFunctions()/*-{
    $wnd.jsDesignerToJS = {
      getProjectJSON: $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getProjectJSON()).bind(this))
    };
    $wnd.jsDesignerLoadProjectFromFile = {
      $entry(@com.google.appinventor.client.jsdesigner.JSDesignerPanel::loadProjectFromFile());
    }
  }-*/;

  public String getProjectJSON() {
    if (this.fileEditor != null) {
      return this.fileEditor.getRawFileContent();
    }
    return "{}";
  }
}

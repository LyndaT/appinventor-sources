// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2009-2011 Google, All Rights reserved
// Copyright © 2011-2017 Massachusetts Institute of Technology, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.client.jsdesigner;

import static com.google.appinventor.client.Ode.MESSAGES;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.user.client.Window;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.client.explorer.project.Project;

import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidFormNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidPackageNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;

import com.google.appinventor.client.editor.designer.DesignerEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.DesignToolbar.DesignProject;
import com.google.appinventor.client.DesignToolbar.Screen;


public class JSDesignerPanel extends HTMLPanel {
  private FileEditor fileEditor;

  private String fileContent;

  private String designFileContent;
  private String blocksFileContent;
  private Ode ode;
  private DesignProject project;

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
    ode = Ode.getInstance();
    project = ode.getDesignToolbar().getCurrentProject();
    Screen currentScreen = project.screens.get(project.currentScreen);
    FileEditor designerEditor = currentScreen.designerEditor;
    FileEditor blocksEditor = currentScreen.blocksEditor;

    this.designFileContent = designerEditor.getRawFileContent();
    this.blocksFileContent = blocksEditor.getRawFileContent();

    this.fileContent = fileEditor.getRawFileContent();

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
    //OdeLog.log("Switching to Blocks Editor");
    Ode.getInstance().getDesignToolbar().publicSwitchToBlocksEditor();
  }

  private void switchToFormEditor() {
    Ode.getInstance().getDesignToolbar().publicSwitchToFormEditor();
  }

  private void addScreen(final String newScreenName) {
    Window.alert(project.getScreensString());
    //see addFormAction in AddFormCommand
    final YoungAndroidProjectNode projectRootNode = (YoungAndroidProjectNode) ode.getCurrentYoungAndroidProjectRootNode();
    if (projectRootNode != null) {
      final YoungAndroidPackageNode packageNode = projectRootNode.getPackageNode();
      String qualifiedFormName = packageNode.getPackageName() + '.' + newScreenName;
      final String formFileId = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
      final String blocksFileId = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);

      Window.alert(formFileId);
      Window.alert(blocksFileId);

      OdeAsyncCallback<Long> callback = new OdeAsyncCallback<Long>(
          // failure message
          MESSAGES.addFormError()) {
        @Override
        public void onSuccess(Long modDate) {
          final Ode ode = Ode.getInstance();
          ode.updateModificationDate(projectRootNode.getProjectId(), modDate);

          // Add the new form and blocks nodes to the project
          final Project rootProject = ode.getProjectManager().getProject(projectRootNode);
          rootProject.addNode(packageNode, new YoungAndroidFormNode(formFileId));
          rootProject.addNode(packageNode, new YoungAndroidBlocksNode(blocksFileId));

          ProjectEditor projectEditor = fileEditor.getProjectEditor();
          project.addScreen(newScreenName, projectEditor.getFileEditor(formFileId), projectEditor.getFileEditor(blocksFileId));
          Window.alert(project.getScreensString());

          // At this stage the FileEditor for form and blocks exist

          // Add the screen to the DesignToolbar and select the new form editor. 
          // We need to do this once the form editor and blocks editor have been
          // added to the project editor (after the files are completely loaded).

          // This will be done on the side of JS

          // Call React functions in here 
        }

        @Override
        public void onFailure(Throwable caught) {
          Window.alert("failure");
        }
      };

      // Create the new form on the backend. The backend will create the form (.scm) and blocks
      // (.blk) files.
      ode.getProjectService().addFile(projectRootNode.getProjectId(), formFileId, callback);
    }
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
    $wnd.jsDesignerAddScreen = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::addScreen(Ljava/lang/String;)).bind(this));
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

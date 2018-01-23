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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Command;

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.core.client.Scheduler;

import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidFormNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidPackageNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidSourceNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidYailNode;

import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.editor.designer.DesignerEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.DesignToolbar.DesignProject;
import com.google.appinventor.client.DesignToolbar.Screen;
import com.google.appinventor.shared.rpc.project.FileNode;

import com.google.gwt.json.client.JSONArray;
import java.util.ArrayList;
import java.util.List;


public class JSDesignerPanel extends HTMLPanel {

  // TODO: remove fileEditor and fileContent
  private FileEditor fileEditor;

  private String designFileContent;
  private String blocksFileContent;

  private FileEditor designerEditor;
  private FileEditor blocksEditor;

  private Ode ode;
  private DesignProject project;

  private String screenName;

  public JSDesignerPanel() {
    super("<div id=\"app\"></div>");
    //ScriptInjector.fromUrl("main.9de011e5.js").inject();
    ScriptInjector.fromUrl("main.js").inject();
    //ScriptInjector.fromUrl("0.js").inject();
  }

  public void show() {
    Ode.getInstance().switchToJSDesignView();
  }

  /**
  * Loads the project into the JS Designer
  **/
  public void loadProject(DesignProject project) {
    OdeLog.log("JSDesignerPanel: Loading in project" + project.name);
    this.project = project;
    exportJSFunctions();

    //Set Screen1 to be the default current screen if there is no project.current Screen
    Screen currentScreen = project.screens.get("Screen1");
    if (project.currentScreen != null) {
      currentScreen = project.screens.get(project.currentScreen);
    } 

    //Set fileEditor to be the designerEditor
    this.designerEditor = currentScreen.designerEditor;
    this.blocksEditor = currentScreen.blocksEditor;

    this.designFileContent = designerEditor.getRawFileContent();
    this.blocksFileContent = blocksEditor.getRawFileContent();

    // TODO: Delete after refactoring;
    this.fileEditor = this.designerEditor;

    tryLoadInDesigner(project.name);
  }

  public void loadFile(FileEditor fileEditor) {
    this.fileEditor = fileEditor;
    exportJSFunctions();

    // Get the screen 
    ode = Ode.getInstance();
    project = ode.getDesignToolbar().getCurrentProject();
    Screen currentScreen = project.screens.get(project.currentScreen);


    this.screenName = currentScreen.screenName;
    // Window.alert(project.name + ' ' + currentScreen.screenName);

    this.designerEditor = currentScreen.designerEditor;
    this.blocksEditor = currentScreen.blocksEditor;

    //Do a check here for invariants before we continue...

    this.designFileContent = designerEditor.getRawFileContent();
    this.blocksFileContent = blocksEditor.getRawFileContent();

    // send call to JS to update designer
    // if designer exists: load in designer, else set up designer
    // if wnd.jsDesignerLoadProject (call load in designer)
    tryLoadInDesigner(project.name);
  }

  // private void setDesignFileContent(String content) {
  //   this.designFileContent = content;
  // }

  // private void setBlocksFileContent(String content) {
  //   this.blocksFileContent = content;
  // }

  public native void tryLoadInDesigner(String projectName)/*-{
    if ($wnd.loadProject && $wnd.changeDisplayedProjectName) {
      console.log("Designer & Display Toolbar exists, load in designer");
      $wnd.jsDesignerLoadProjectFromFile();
      $wnd.changeDisplayedProjectName(projectName);
      $wnd.loadDropdownScreens($wnd.jsDesignerGetProjectScreens());
    } else {
      console.log("Set up designer or toolbar required");
    }
  }-*/;

  public native void openProjectInJSDesigner(String rawDesignFileContent, String rawBlocksFileContent, String projectName, String projectId)/*-{
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
    $wnd.loadProject(rawDesignFileContent, rawBlocksFileContent, projectName, projectId);
  }-*/;

  private void loadProjectFromFile(){
    openProjectInJSDesigner(designFileContent, blocksFileContent, this.project.name, this.getProjectId()); 
  };

  /******************************
  * Getters
  ******************************/

  private String getDesignFileContent(final String screenName) {
    Screen selectedScreen = project.screens.get(screenName);
    return selectedScreen.designerEditor.getRawFileContent();
  }

  private String getBlocksFileContent(final String screenName) {
    Screen selectedScreen = project.screens.get(screenName);
    return selectedScreen.blocksEditor.getRawFileContent();
  }

  private String getProjectName() {
    return project.name;
  }

  // Returns a String of the project screens separated by commas.
  private String getProjectScreens() {
    List<String> result = new ArrayList(project.screens.keySet());
    String screenStr = "";
    screenStr = screenStr + result.get(0);
    for (int i = 1; i < result.size(); i++) {
      screenStr = screenStr + "," + result.get(i);
    }
    return screenStr;
  }

  private String getProjectId() {
    return Long.toString(fileEditor.getProjectId());
  }

  // private String getRawFileContent() {
  //   return designFileContent;
  // }

  /******************************
  * Switching editors
  * (Maybe used for saving? Not sure if actually needed)
  ******************************/

  private void switchToBlocksEditor() {
    //OdeLog.log("Switching to Blocks Editor");
    Ode.getInstance().getDesignToolbar().publicSwitchToBlocksEditor();
  }

  private void switchToFormEditor() {
    Ode.getInstance().getDesignToolbar().publicSwitchToFormEditor();
  }

  private void saveDirtyEditorsForScreen(String screenName) {
    Screen selectedScreen = project.screens.get(screenName);
    Ode.getInstance().getEditorManager().scheduleAutoSave(selectedScreen.designerEditor);
    Ode.getInstance().getEditorManager().scheduleAutoSave(selectedScreen.blocksEditor);
  }

  /******************************
  * Adding and removal of screens
  ******************************/
  private void addScreen(final String newScreenName) {
    //Window.alert(project.getScreensString());
    //see addFormAction in AddFormCommand
    final YoungAndroidProjectNode projectRootNode = (YoungAndroidProjectNode) ode.getCurrentYoungAndroidProjectRootNode();
    if (projectRootNode != null) {
      final YoungAndroidPackageNode packageNode = projectRootNode.getPackageNode();
      String qualifiedFormName = packageNode.getPackageName() + '.' + newScreenName;
      final String formFileId = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
      final String blocksFileId = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);

      // Window.alert(formFileId);
      // Window.alert(blocksFileId);
      // Window.alert(Long.toString(projectRootNode.getProjectId()));

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

          final ProjectEditor projectEditor = fileEditor.getProjectEditor();

          project.addScreen(newScreenName, projectEditor.getFileEditor(formFileId), projectEditor.getFileEditor(blocksFileId));

          // At this stage the FileEditor for form and blocks exist

          // Add the screen to the DesignToolbar and select the new form editor. 
          // We need to do this once the form editor and blocks editor have been
          // added to the project editor (after the files are completely loaded).

          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
              FileEditor formEditor = projectEditor.getFileEditor(formFileId);
              FileEditor blocksEditor = projectEditor.getFileEditor(blocksFileId);
              if (formEditor != null && blocksEditor != null && !ode.screensLocked()) {
                String newFormContent = formEditor.getRawFileContent();
                String newBlocksContent = blocksEditor.getRawFileContent();
                // OdeLog.log("ADDED SCREEN SUCCESS JAVA");
                // OdeLog.log(newFormContent);
                // OdeLog.log(newBlocksContent);
                sendAddedScreenSuccess(newScreenName, newFormContent, newBlocksContent);
              } else {
                // The form editor and/or blocks editor is still not there. Try again later.
                // I'm sorry evan
                Scheduler.get().scheduleDeferred(this);
              }
            }
          });
        }

        @Override
        public void onFailure(Throwable caught) {
          Window.alert("failure");
          // Call React functions for failing to adding a screen
          sendAddedScreenFailure();
        }
      };

      // Create the new form on the backend. The backend will create the form (.scm) and blocks
      // (.blk) files.
      ode.getProjectService().addFile(projectRootNode.getProjectId(), formFileId, callback);
    }
  }

  // Assume delete has already been confirmed
  private void removeScreen(final String screenName) {
    OdeLog.log("JSDESIGNER: Removing screens");
    if (!project.screens.containsKey(screenName) || screenName == YoungAndroidSourceNode.SCREEN1_FORM_NAME) {
      return;
    } else {
      Screen selectedScreen = project.screens.get(screenName);
      FileEditor editor = selectedScreen.designerEditor;
      FileNode fileNode = editor.getFileNode();
      if (!(fileNode instanceof YoungAndroidSourceNode)) {
        Window.alert("Screen file node is not instance of YoungAndroidSourceNode");
        return;
      } 
      final YoungAndroidSourceNode node = (YoungAndroidSourceNode) fileNode;
      
      // node could be either a YoungAndroidFormNode or a YoungAndroidBlocksNode.
      // Before we delete the form, we need to close both the form editor and the blocks editor
      // (in the browser).
      final String qualifiedFormName = ((YoungAndroidSourceNode) node).getQualifiedName();
      final String formFileId = YoungAndroidFormNode.getFormFileId(qualifiedFormName);
      final String blocksFileId = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedFormName);
      final String yailFileId = YoungAndroidYailNode.getYailFileId(qualifiedFormName);
      final long projectId = node.getProjectId();
      String fileIds[] = new String[2];
      fileIds[0] = formFileId;
      fileIds[1] = blocksFileId;
      ode.getEditorManager().closeFileEditors(projectId, fileIds);

      // When we tell the project service to delete either the form (.scm) file or the blocks
      // (.bky) file, it will delete both of them, and also the yail (.yail) file.
      ode.getProjectService().deleteFile(ode.getSessionId(), projectId, node.getFileId(),
          new OdeAsyncCallback<Long>(
      // message on failure
          MESSAGES.deleteFileError()) {
        @Override
        public void onSuccess(Long date) {
          // Remove all related nodes (form, blocks, yail) from the project.
          Project rmproject = Ode.getInstance().getProjectManager().getProject(projectId);
          for (ProjectNode sourceNode : node.getProjectRoot().getAllSourceNodes()) {
            if (sourceNode.getFileId().equals(formFileId) ||
                sourceNode.getFileId().equals(blocksFileId) ||
                sourceNode.getFileId().equals(yailFileId)) {
              rmproject.deleteNode(sourceNode);
            }
          }

          project.removeScreen(screenName);
          ode.updateModificationDate(projectId, date);
          //executeNextCommand(node);

          //Window.alert("Screen removed");
          sendRemovedScreenSuccess(screenName);
        }

        @Override
        public void onFailure(Throwable caught) {
          Window.alert("delete screen failure");
          sendRemovedScreenFailure();
        }
      });

    }
  }

  public native void sendAddedScreenSuccess(String screenName, String screenFormContent, String screenBlocksContent)/*-{
    // load the dropdown screens
    if ($wnd.loadDropdownScreens && $wnd.sendAddedScreenSuccess) {
      $wnd.loadDropdownScreens($wnd.jsDesignerGetProjectScreens());
      $wnd.sendAddedScreenSuccess(screenName, screenFormContent, screenBlocksContent);
    } 
  }-*/;

  public native void sendAddedScreenFailure()/*-{
    if ($wnd.sendAddedScreenFailure) {
      $wnd.sendAddedScreenFailure(); 
    } 
  }-*/;

  public native void sendRemovedScreenSuccess(String screenName)/*-{
    console.log("trying to remove screen");
    if ($wnd.sendRemovedScreenSuccess) {
      console.log("sending call to remove screen on JS side")
      $wnd.loadDropdownScreens($wnd.jsDesignerGetProjectScreens());
      $wnd.sendRemovedScreenSuccess(screenName);
    } 
  }-*/;

  public native void sendRemovedScreenFailure()/*-{
    if ($wnd.sendRemoveScreenFailure) {
      $wnd.sendRemovedScreenFailure();
    } 
  }-*/;

  /******************************
  * Exporting to JS Functions
  ******************************/

  public native void exportJSFunctions()/*-{
    $wnd.jsDesignerToJS = {
      getProjectJSON: $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getProjectJSON()).bind(this))
    };
    $wnd.jsDesignerLoadProjectFromFile = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::loadProjectFromFile()).bind(this));

    $wnd.jsGetDesignFileContent = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getDesignFileContent(Ljava/lang/String;)).bind(this));
    $wnd.jsGetBlocksFileContent = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getBlocksFileContent(Ljava/lang/String;)).bind(this));
    $wnd.jsDesignerGetProjectName = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getProjectName()).bind(this));
    $wnd.jsDesignerGetProjectScreens = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getProjectScreens()).bind(this));
    $wnd.jsDesignerGetCurrentProjectId = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::getProjectId()).bind(this));

    $wnd.jsDesignerSwitchToBlocksEditor = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::switchToBlocksEditor()).bind(this));
    $wnd.jsDesignerSwitchToFormEditor = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::switchToFormEditor()).bind(this));
    $wnd.jsDesignerSaveDirtyEditorsForScreen = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::saveDirtyEditorsForScreen(Ljava/lang/String;)).bind(this));

    $wnd.jsDesignerAddScreen = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::addScreen(Ljava/lang/String;)).bind(this));
    $wnd.jsDesignerRemoveScreen = $entry((this.@com.google.appinventor.client.jsdesigner.JSDesignerPanel::removeScreen(Ljava/lang/String;)).bind(this));
  }-*/;

  public String getProjectJSON() {
    if (this.fileEditor != null) {
      return this.designFileContent;
    }
    return "{}";
  }
}

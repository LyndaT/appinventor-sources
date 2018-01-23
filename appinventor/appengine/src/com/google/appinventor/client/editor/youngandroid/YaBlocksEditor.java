// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2009-2011 Google, All Rights reserved
// Copyright © 2011-2017 Massachusetts Institute of Technology, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.editor.blocks.BlocklyPanel;
import com.google.appinventor.client.editor.blocks.BlocksCategory;
import com.google.appinventor.client.editor.blocks.BlocksCodeGenerationException;
import com.google.appinventor.client.editor.blocks.BlocksCodeGenerationTarget;
import com.google.appinventor.client.editor.blocks.BlocksEditor;
import com.google.appinventor.client.editor.blocks.BlocksLanguage;
import com.google.appinventor.client.editor.simple.SimpleComponentDatabase;
import com.google.appinventor.client.editor.simple.components.MockForm;
import com.google.appinventor.shared.rpc.project.FileDescriptorWithContent;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.gwt.core.client.JavaScriptObject;

import com.google.appinventor.client.output.OdeLog;

/**
 * Editor for Young Android Blocks (.blk) files.
 *
 * @author lizlooney@google.com (Liz Looney)
 * @author sharon@google.com (Sharon Perl) added Blockly functionality
 * @author ewpatton@mit.edu (Evan W. Patton) refactored for additional blocks editor types
 */
public final class YaBlocksEditor extends BlocksEditor<YoungAndroidBlocksNode, YaFormEditor> {

  private static final BlocksLanguage YAIL =
      new BlocksLanguage("Yail",
          new BlocksCategory("Control", IMAGES.control()),
          new BlocksCategory("Logic", IMAGES.logic()),
          new BlocksCategory("Math", IMAGES.math()),
          new BlocksCategory("Text", IMAGES.text()),
          new BlocksCategory("Lists", IMAGES.lists()),
          new BlocksCategory("Colors", IMAGES.colors()),
          new BlocksCategory("Variables", IMAGES.variables()),
          new BlocksCategory("Procedures", IMAGES.procedures()));

  YaBlocksEditor(YaProjectEditor projectEditor, YoungAndroidBlocksNode blocksNode) {
    super(projectEditor, blocksNode, YAIL, BlocksCodeGenerationTarget.YAIL,
        SimpleComponentDatabase.getInstance(blocksNode.getProjectId()));
  }

  // FileEditor methods

  @Override
  public void onShow() {
    super.onShow();
    sendComponentData();  // Send Blockly the component information for generating Yail
  }

  @Override
  public void onWorkspaceChange(BlocklyPanel panel, JavaScriptObject event) {
    super.onWorkspaceChange(panel, event);
    sendComponentData();
  }

  public synchronized void sendComponentData() {
    try {
      blocksArea.sendComponentData(designer.encodeFormAsJsonString(true),
        packageNameFromPath(getFileId()));
    } catch (BlocksCodeGenerationException e) {
      e.printStackTrace();
    }
  }

  // Do whatever is needed to save Blockly state when our project is about to be
  // detached from the parent document. Note that this is not for saving the blocks file itself.
  // We use EditorManager.scheduleAutoSave for that.
  public void prepareForUnload() {
    blocksArea.saveComponentsAndBlocks();
//    blocksArea.saveBackpackContents();
  }

  public FileDescriptorWithContent getYail() throws BlocksCodeGenerationException {
    return new FileDescriptorWithContent(getProjectId(), yailFileName(),
        blocksArea.getCode(designer.encodeFormAsJsonString(true),
            packageNameFromPath(getFileId())));
  }

  /**
   * Converts a source file path (e.g.,
   * src/com/gmail/username/project1/Form.extension) into a package
   * name (e.g., com.gmail.username.project1.Form)
   * @param path the path to convert.
   * @return a dot separated package name.
   */
  private static String packageNameFromPath(String path) {
    path = path.replaceFirst("src/", "");
    int extensionIndex = path.lastIndexOf(".");
    if (extensionIndex != -1) {
      path = path.substring(0, extensionIndex);
    }
    return path.replaceAll("/", ".");
  }

  public MockForm getForm() {
    YaProjectEditor yaProjectEditor = (YaProjectEditor) projectEditor;
    YaFormEditor myFormEditor = yaProjectEditor.getFormFileEditor(blocksNode.getFormName());
    if (myFormEditor != null) {
      return myFormEditor.getForm();
    } else {
      return null;
    }
  }

  private String yailFileName() {
    String fileId = getFileId();
    return fileId.replace(YoungAndroidSourceAnalyzer.BLOCKLY_SOURCE_EXTENSION,
        YoungAndroidSourceAnalyzer.YAIL_FILE_EXTENSION);
  }

  /*
   * Start up the Repl (call into the Blockly.ReplMgr via the BlocklyPanel.
   */
  @Override
  public void startRepl(boolean alreadyRunning, boolean forEmulator, boolean forUsb) {
    blocksArea.startRepl(alreadyRunning, forEmulator, forUsb);
  }

  /*
   * Perform a Hard Reset of the Emulator
   */
  public void hardReset() {
    blocksArea.hardReset();
  }

  /*
   * Trigger a Companion Update
   */
  @Override
  public void updateCompanion() {
    blocksArea.updateCompanion();
  }
}

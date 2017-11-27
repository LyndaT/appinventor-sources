// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.

package com.google.appinventor.client.editor.iot;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.boxes.PaletteBox;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.blocks.BlocksEditor;
import com.google.appinventor.client.editor.designer.DesignerEditor;
import com.google.appinventor.client.editor.iot.palette.IotPalettePanel;
import com.google.appinventor.client.editor.simple.SimpleNonVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.components.MockMicrocontroller;
import com.google.appinventor.client.editor.simple.palette.DropTargetProvider;
import com.google.appinventor.client.properties.json.ClientJsonParser;
import com.google.appinventor.client.widgets.dnd.DropTarget;
import com.google.appinventor.client.widgets.properties.EditableProperties;
import com.google.appinventor.shared.properties.json.JSONObject;
import com.google.appinventor.shared.rpc.project.iot.IotMicrocontrollerNode;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Command;

import java.util.List;

/**
 * Design editor for IOT sketches.
 *
 * @author ewpatton@mit.edu (Evan W. Patton)
 */
public class IotMicrocontrollerEditor extends DesignerEditor<IotMicrocontrollerNode, MockMicrocontroller, IotPalettePanel, IotDeviceDatabase> {

  private String preUpgradeJson = null;

  private JSONArray authUrl = null;

  public IotMicrocontrollerEditor(ProjectEditor projectEditor, IotMicrocontrollerNode sourceNode) {
    super(projectEditor, sourceNode, IotDeviceDatabase.getInstance(sourceNode.getProjectId()),
        new IotVisibleComponentsPanel(new SimpleNonVisibleComponentsPanel<MockMicrocontroller>()));

    palettePanel = new IotPalettePanel(this);
    palettePanel.loadComponents(new DropTargetProvider() {
      @Override
      public DropTarget[] getDropTargets() {
        List<DropTarget> dropTargets = root.getDropTargetsWithin();
        dropTargets.add(getVisibleComponentsPanel());
        dropTargets.add(getNonVisibleComponentsPanel());
        return dropTargets.toArray(new DropTarget[dropTargets.size()]);
      }
    });
    palettePanel.setSize("100%", "100%");
    componentDatabaseChangeListeners.add(palettePanel);
  }

  @Override
  public void onHide() {
    if (Ode.getInstance().getCurrentFileEditor() == this) {
      super.onHide();
      unloadDesigner();
    }
  }

  @Override
  public String getRawFileContent() {
    return encodeMicrocontrollerAsJsonString();
  }

  @Override
  public String getJson() {
    return preUpgradeJson;
  }

  @Override
  public MockMicrocontroller newRootObject() {
    return new MockMicrocontroller(this);
  }

  @Override
  protected void upgradeFile(FileContentHolder fileContentHolder, Command afterUpgradeCompleted) {
    preUpgradeJson = fileContentHolder.getFileContent();
    if (afterUpgradeCompleted != null) {
      afterUpgradeCompleted.execute();
    }
  }

  @Override
  protected void onFileLoaded(String content) {
    try {
      JSONObject properties = new ClientJsonParser().parse(content).asObject();
      root = (MockMicrocontroller) createMockComponent(properties.getProperties().get("Properties").asObject(),
          null, MockMicrocontroller.TYPE);

      nonVisibleComponentsPanel.setRoot(root);
      visibleComponentsPanel.setRoot(root);

      root.select();
    } catch(JSONException e) {
      throw new IllegalStateException("Invalid JSON for Sketch", e);
    } finally {
      super.onFileLoaded(content);
    }
  }

  protected void loadDesigner() {
    root.refresh();
    selectedComponent = root.getSelectedComponent();

    PaletteBox paletteBox = PaletteBox.getPaletteBox();
    paletteBox.setContent(palettePanel);

    BlocksEditor<?, ?> blocksEditor = (BlocksEditor<?, ?>) projectEditor.getFileEditor(sourceNode.getEntityName(), BlocksEditor.EDITOR_TYPE);
    if (blocksEditor != null) {
      root.addDesignerChangeListener(this);
      root.addDesignerChangeListener(blocksEditor);
      super.loadDesigner();
    } else {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          loadDesigner();
        }
      });
    }
  }

  protected String encodeMicrocontrollerAsJsonString() {
    com.google.gwt.json.client.JSONObject json = new com.google.gwt.json.client.JSONObject();
    if (authUrl != null) {
      json.put("authURL", authUrl);
    }
    json.put("IotVersion", new JSONNumber(1));
    json.put("Source", new JSONString(MockMicrocontroller.TYPE));
    json.put("Properties", encodeComponentProperties(root));
    return json.toString();
  }

  private com.google.gwt.json.client.JSONObject encodeComponentProperties(MockComponent component) {
    com.google.gwt.json.client.JSONObject result = new com.google.gwt.json.client.JSONObject();
    EditableProperties properties = component.getProperties();
    final String type = component.getType();
    result.put("$Name", new JSONString(properties.getPropertyValue("Name")));
    result.put("$Type", new JSONString(type));
    result.put("$Version", new JSONString(Integer.toString(componentDatabase.getComponentVersion(type))));

    com.google.gwt.json.client.JSONObject jsonProperties = properties.encodeAsJson(false, false);
    if (jsonProperties.size() > 0) {
      for (String key : jsonProperties.keySet()) {
        result.put(key, jsonProperties.get(key));
      }
    }

    List<MockComponent> children = component.getChildren();
    if (!children.isEmpty()) {
      JSONArray childrenJson = new JSONArray();
      int i = 0;
      for (MockComponent child : children) {
        childrenJson.set(i++, encodeComponentProperties(child));
      }
      result.put("$Components", childrenJson);
    }
    return result;
  }

}

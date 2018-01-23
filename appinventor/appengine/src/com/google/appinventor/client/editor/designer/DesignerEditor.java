// -*- mode: java; c-basic-offset: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.

package com.google.appinventor.client.editor.designer;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.boxes.AssetListBox;
import com.google.appinventor.client.boxes.PaletteBox;
import com.google.appinventor.client.boxes.PropertiesBox;
import com.google.appinventor.client.boxes.SourceStructureBox;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.blocks.BlocksEditor;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.editor.simple.SimpleNonVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.SimpleVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.components.MockContainer;
import com.google.appinventor.client.editor.simple.palette.SimplePalettePanel;
import com.google.appinventor.client.explorer.SourceStructureExplorer;
import com.google.appinventor.shared.properties.json.JSONObject;
import com.google.appinventor.shared.properties.json.JSONValue;
import com.google.appinventor.shared.simple.ComponentDatabaseChangeListener;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.widgets.properties.EditableProperties;
import com.google.appinventor.client.widgets.properties.PropertiesPanel;
import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.shared.rpc.project.SourceNode;
import com.google.appinventor.shared.simple.ComponentDatabaseInterface;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * DesignerEditor is an ancestor of all designer editors in App Inventor.
 *
 * @author markf@google.com (Mark Friedman)
 * @author lizlooney@google.com (Liz Looney)
 * @author ewpatton@mit.edu (Evan W. Patton)
 */
public abstract class DesignerEditor<S extends SourceNode, T extends DesignerRootComponent,
    U extends SimplePalettePanel, V extends ComponentDatabaseInterface>
    extends SimpleEditor implements DesignerChangeListener, ComponentDatabaseChangeListener {

  protected static class FileContentHolder {
    private String content;

    FileContentHolder(String content) {
      this.content = content;
    }

    public void setFileContent(String content) {
      this.content = content;
    }

    public String getFileContent() {
      return content;
    }
  }

  public static final String EDITOR_TYPE = DesignerEditor.class.getSimpleName();

  protected final List<ComponentDatabaseChangeListener> componentDatabaseChangeListeners
      = new ArrayList<>();

  protected final S sourceNode;

  protected final V componentDatabase;

  // References to other panels that we need to control.
  private final SourceStructureExplorer sourceStructureExplorer;

  private final PropertiesPanel designProperties;

  // Flag to indicate when loading the file is completed. This is needed because building the mock
  // form from the file properties fires events that need to be ignored, otherwise the file will be
  // marked as being modified.
  private boolean loadComplete = false;

  protected MockComponent selectedComponent;

  protected T root;

  // Panels that are used as the content of the palette and properties boxes.
  protected U palettePanel;

  // UI elements
  protected final SimpleVisibleComponentsPanel<T> visibleComponentsPanel;
  protected final SimpleNonVisibleComponentsPanel<T> nonVisibleComponentsPanel;

  public DesignerEditor(ProjectEditor projectEditor, S sourceNode,
                        V componentDatabase,
                        SimpleVisibleComponentsPanel<T> visibleComponentsPanel) {
    super(projectEditor, sourceNode);

    this.sourceNode = sourceNode;
    this.componentDatabase = componentDatabase;

    // Get reference to the source structure explorer
    sourceStructureExplorer =
        SourceStructureBox.getSourceStructureBox().getSourceStructureExplorer();

    // Create UI elements for the designer panels.
    this.visibleComponentsPanel = visibleComponentsPanel;
    nonVisibleComponentsPanel = visibleComponentsPanel.getNonVisibleComponentsPanel();
    componentDatabaseChangeListeners.add(nonVisibleComponentsPanel);
    componentDatabaseChangeListeners.add(visibleComponentsPanel);
    DockPanel componentsPanel = new DockPanel();
    componentsPanel.setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    componentsPanel.add(visibleComponentsPanel, DockPanel.NORTH);
    componentsPanel.add(nonVisibleComponentsPanel, DockPanel.SOUTH);
    componentsPanel.setSize("100%", "100%");

    // Create designProperties, which will be used as the content of the PropertiesBox.
    designProperties = new PropertiesPanel();
    designProperties.setSize("100%", "100%");

    root = null;
    selectedComponent = null;

    initWidget(componentsPanel);
    setSize("100%", "100%");
  }

  @Override
  public void onShow() {
    super.onShow();
    loadDesigner();
  }

  public abstract String getJson();
  protected abstract T newRootObject();

  protected abstract void upgradeFile(FileContentHolder fileContentHolder,
                                      final Command afterUpgradeCompleted);

  public T getRoot() {
    return root;
  }

  protected void onStructureChange() {
    Ode.getInstance().getEditorManager().scheduleAutoSave(this);

    // Update source structure panel
    sourceStructureExplorer.updateTree(root.buildComponentsTree(),
        root.getSelectedComponent().getSourceStructureExplorerItem());
  }

  protected void loadDesigner() {
    // Update the source structure explorer with the tree of this form's components.
    sourceStructureExplorer.updateTree(root.buildComponentsTree(),
        selectedComponent.getSourceStructureExplorerItem());
    SourceStructureBox.getSourceStructureBox().setVisible(true);

    // Show the assets box.
    AssetListBox assetListBox = AssetListBox.getAssetListBox();
    assetListBox.setVisible(true);

    // Set the properties box's content.
    PropertiesBox propertiesBox = PropertiesBox.getPropertiesBox();
    propertiesBox.setContent(designProperties);
    updatePropertiesPanel(selectedComponent);
    propertiesBox.setVisible(true);
  }

  /**
   * Clears the palette, source structure explorer, and properties panel.
   */
  protected void unloadDesigner() {
    // The form can still potentially change if the blocks editor is displayed
    // so don't remove the formChangeListener.

    // Clear the palette box.
    PaletteBox paletteBox = PaletteBox.getPaletteBox();
    paletteBox.clear();

    // Clear and hide the source structure explorer.
    sourceStructureExplorer.clearTree();
    SourceStructureBox.getSourceStructureBox().setVisible(false);

    // Hide the assets box.
    AssetListBox assetListBox = AssetListBox.getAssetListBox();
    assetListBox.setVisible(false);

    // Clear and hide the properties box.
    PropertiesBox propertiesBox = PropertiesBox.getPropertiesBox();
    propertiesBox.clear();
    propertiesBox.setVisible(false);
  }

  // ComponentDatabaseChangeListener implementation
  @Override
  public void onComponentTypeAdded(List<String> componentTypes) {
    componentDatabase.removeComponentDatabaseListener(this);
    for (ComponentDatabaseChangeListener listener : componentDatabaseChangeListeners) {
      listener.onComponentTypeAdded(componentTypes);
    }
    //Update the Properties Panel
    updatePropertiesPanel(root.getSelectedComponent());
  }

  @Override
  public boolean beforeComponentTypeRemoved(List<String> componentTypes) {
    boolean result = true;
    for (ComponentDatabaseChangeListener listener : componentDatabaseChangeListeners) {
      result = result && listener.beforeComponentTypeRemoved(componentTypes);
    }
    return result;
  }

  @Override
  public void onComponentTypeRemoved(Map<String, String> componentTypes) {
    componentDatabase.removeComponentDatabaseListener(this);
    for (ComponentDatabaseChangeListener cdbChangeListener : componentDatabaseChangeListeners) {
      cdbChangeListener.onComponentTypeRemoved(componentTypes);
    }
  }

  @Override
  public void onResetDatabase() {
    componentDatabase.removeComponentDatabaseListener(this);
    for (ComponentDatabaseChangeListener listener : componentDatabaseChangeListeners) {
      listener.onResetDatabase();
    }
  }

  // DesignerChangeListener implementation
  @Override
  public void onComponentPropertyChanged(MockComponent component, String propertyName, String propertyValue) {
    if (loadComplete) {
      // If the property isn't actually persisted to the .scm file, we don't need to do anything.
      if (component.isPropertyPersisted(propertyName)) {
        Ode.getInstance().getEditorManager().scheduleAutoSave(this);
      }
    } else {
      OdeLog.elog("onComponentPropertyChanged called when loadComplete is false");
    }
  }

  @Override
  public void onComponentRemoved(MockComponent component, boolean permanentlyDeleted) {
    if (loadComplete) {
      if (permanentlyDeleted) {
        onStructureChange();
      }
    } else {
      OdeLog.elog("onComponentRemoved called when loadComplete is false");
    }
  }

  @Override
  public void onComponentAdded(MockComponent component) {
    if (loadComplete) {
      onStructureChange();
    } else {
      OdeLog.elog("onComponentAdded called when loadComplete is false");
    }
  }

  @Override
  public void onComponentRenamed(MockComponent component, String oldName) {
    if (loadComplete) {
      onStructureChange();
      updatePropertiesPanel(component);
    } else {
      OdeLog.elog("onComponentRenamed called when loadComplete is false");
    }
  }

  @Override
  public void onComponentSelectionChange(MockComponent component, boolean selected) {
    if (loadComplete) {
      if (selected) {
        // Select the item in the source structure explorer.
        sourceStructureExplorer.selectItem(component.getSourceStructureExplorerItem());

        // Show the component properties in the properties panel.
        updatePropertiesPanel(component);
      } else {
        // Unselect the item in the source structure explorer.
        sourceStructureExplorer.unselectItem(component.getSourceStructureExplorerItem());
      }
    } else {
      OdeLog.elog("onComponentSelectionChange called when loadComplete is false");
    }

  }

  // SimpleEditor implementation
  @Override
  public boolean isLoadComplete() {
    return loadComplete;
  }

  @Override
  public Map<String, MockComponent> getComponents() {
    Map<String, MockComponent> map = Maps.newHashMap();
    if (loadComplete) {
      populateComponentsMap(root.asMockComponent(), map);
    }
    return map;
  }

  @Override
  public List<String> getComponentNames() {
    return new ArrayList<>(getComponents().keySet());
  }

  @Override
  public SimplePalettePanel getComponentPalettePanel() {
    return palettePanel;
  }

  @Override
  public SimpleNonVisibleComponentsPanel getNonVisibleComponentsPanel() {
    return nonVisibleComponentsPanel;
  }

  @Override
  public SimpleVisibleComponentsPanel getVisibleComponentsPanel() {
    return visibleComponentsPanel;
  }

  @Override
  public boolean isScreen1() {
    return false;
  }

  // FileEditor implementation
  @Override
  public void loadFile(final Command afterFileLoaded) {
    final long projectId = getProjectId();
    final String fileId = getFileId();
    OdeAsyncCallback<ChecksumedLoadFile> callback = new OdeAsyncCallback<ChecksumedLoadFile>(MESSAGES.loadError()) {
      @Override
      public void onSuccess(ChecksumedLoadFile result) {
        String contents;
        try {
          contents = result.getContent();
        } catch (ChecksumedFileException e) {
          this.onFailure(e);
          return;
        }
        final FileContentHolder fileContentHolder = new FileContentHolder(contents);
        OdeLog.log(contents);
        upgradeFile(fileContentHolder, new Command() {
          @Override
          public void execute() {
            OdeLog.log("FileEditor calling onFileLoaded in YAFormEditor");
            onFileLoaded(fileContentHolder.getFileContent());
            if (afterFileLoaded != null) {
              afterFileLoaded.execute();
            }
          }
        });
      }
      @Override
      public void onFailure(Throwable caught) {
        if (caught instanceof ChecksumedFileException) {
          Ode.getInstance().recordCorruptProject(projectId, fileId, caught.getMessage());
        }
        super.onFailure(caught);
      }
    };
    Ode.getInstance().getProjectService().load2(projectId, fileId, callback);
  }

  @Override
  public String getTabText() {
    return sourceNode.getEntityName();
  }

  @Override
  public void onSave() {
  }

  @Override
  public final String getEditorType() {
    return EDITOR_TYPE;
  }

  // private implementation
  /*
   * Show the given component's properties in the properties panel.
   */
  private void updatePropertiesPanel(MockComponent component) {
    designProperties.setProperties(component.getProperties());
    // need to update the caption after the setProperties call, since
    // setProperties clears the caption!
    designProperties.setPropertiesCaption(component.getName());
  }

  private void populateComponentsMap(MockComponent component, Map<String, MockComponent> map) {
    EditableProperties properties = component.getProperties();
    map.put(properties.getPropertyValue("Name"), component);
    List<MockComponent> children = component.getChildren();
    for (MockComponent child : children) {
      populateComponentsMap(child, map);
    }
  }

  protected void onFileLoaded(String content) {
    // Set loadCompleted to true.
    // From now on, all change events will be taken seriously.
    loadComplete = true;
  }

  public V getComponentDatabase() {
    return componentDatabase;
  }

  /*
   * Parses the JSON properties and creates the component structure. This method is called
   * recursively for nested components. For the initial invocation parent shall be null.
   */
  protected MockComponent createMockComponent(JSONObject propertiesObject, MockContainer parent,
                                              String rootType) {
    Map<String, JSONValue> properties = propertiesObject.getProperties();

    // Component name and type
    String componentType = properties.get("$Type").asString().getString();

    // Instantiate a mock component for the visual designer
    MockComponent mockComponent;
    if (componentType.equals(rootType)) {
      Preconditions.checkArgument(parent == null);

      // Instantiate new root component
      mockComponent = (MockComponent) newRootObject();

      //This is for old project which doesn't have the AppName property
      if ("Form".equals(rootType) && !properties.keySet().contains("AppName")) {
        String fileId = getFileId();
        String projectName = fileId.split("/")[3];
        mockComponent.changeProperty("AppName", projectName);
      }
    } else {
      mockComponent = palettePanel.createMockComponent(componentType);

      // Add the component to its parent component (and if it is non-visible, add it to the
      // nonVisibleComponent panel).
      parent.addComponent(mockComponent);
      if (!mockComponent.isVisibleComponent()) {
        nonVisibleComponentsPanel.addComponent(mockComponent);
      }
    }

    // Set the name of the component (on instantiation components are assigned a generated name)
    String componentName = properties.get("$Name").asString().getString();
    mockComponent.changeProperty("Name", componentName);

    // Set component properties
    for (String name : properties.keySet()) {
      if (name.charAt(0) != '$') { // Ignore special properties (name, type and nested components)
        mockComponent.changeProperty(name, properties.get(name).asString().getString());
      }
    }

    // Add component type to the blocks editor
    BlocksEditor<?, ?> blockEditor = (BlocksEditor<?, ?>) projectEditor.getFileEditor(sourceNode.getEntityName(), BlocksEditor.EDITOR_TYPE);
    if (blockEditor != null) {
      blockEditor.addComponent(mockComponent.getType(), mockComponent.getName(),
          mockComponent.getUuid());
    }

    // Add nested components
    if (properties.containsKey("$Components")) {
      for (JSONValue nestedComponent : properties.get("$Components").asArray().getElements()) {
        createMockComponent(nestedComponent.asObject(), (MockContainer) mockComponent, rootType);
      }
    }

    return mockComponent;
  }
}

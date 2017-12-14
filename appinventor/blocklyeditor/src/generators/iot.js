// -*- mode: javascript; js-indent-level: 2; -*-
// Copyright © 2017 Massachusetts Institute of Technology, All rights reserved.

'use strict';

goog.provide('AI.Blockly.Iot');
goog.require('Blockly.Generator');

AI.Blockly.Iot = new Blockly.Generator('IOT');

/**
 * Generate bytecode for the embedded device companion.
 *
 * @param {String} sketchJson JSON string describing the contents of the microcontroller sketch.
 * @param {String} packageName ignored -- this is kept to maintain symmetry with
 *     {@link Blockly.Yail.getFormYail}.
 * @param {boolean} forRepl true if the code is being generated for the REPL (always true for IOT)
 * @param {Blockly.WorkspaceSvg} workspace Workspace to use for generating code.
 * @returns {Array.<number>} Byte values to program the microcontroller's VM.
 */
AI.Blockly.Iot.getIotByteCode = function(sketchJson, packageName, forRepl, workspace) {
  var sketch = JSON.parse(sketchJson);
  var state = new AI.Blockly.Iot.CompilerState();
  AI.Blockly.Iot.createSymbolTables(workspace, sketch, state);
  AI.Blockly.Iot.compileInitializer(sketch, state);
  AI.Blockly.Iot.compileEventHandlers(sketch, state);
  AI.Blockly.Iot.compileUserProcedures(sketch, state);
  AI.Blockly.Iot.link(state);
  AI.Blockly.Iot.createProgram(sketch, state);
  return state.program;
};

AI.Blockly.Iot.CompilerState = function() {
  this.globalSymbols = {};
  this.procSymbols = {};
  this.eventSymbols = {};
  this.unlinkedReferences = {};
  this.deviceList = [];
  this.program = [];
  this.userProcs = 0;
  this.strings = 0;
  this.persistentGlobals = 0;
  this.devices = 0;
  this.initializer = null;
};

AI.Blockly.Iot.createSymbolTables = function(workspace, sketch, state) {
  var blocks = workspace.getTopBlocks();
  for (var i = 0; i < blocks.length; i++) {
    var progBlock = null;
    switch (blocks[i].type) {
      case 'global_definition':
        progBlock = new AI.Blockly.Iot.GlobalDefinigion(blocks[i]);
        state.globalSymbols[progBlock.name] = progBlock;
        break;
      case 'procedure_defreturn':
      case 'procedure_defnoreturn':
        progBlock = new AI.Blockly.Iot.ProcedureBlock(blocks[i]);
        state.procSymbols[progBlock.name] = progBlock;
        state.userProcs++;
        break;
      case 'component_event':
        if (blocks[i].typeName === 'Sketch' && blocks[i].eventName === 'Initialize') {
          // consumed by the initialization block
          state.initialization = new AI.Blockly.Iot.InitializationBlock(block);
          continue;
        }
        progBlock = new AI.Blockly.Iot.EventHandlerBlock(blocks[i]);
        state.eventSymbols[progBlock.name] = progBlock;
        break;
    }
  }
  return symbols;
};

AI.Blockly.Iot.compileInitializer = function(sketch, state) {
  state.initializer = new AI.Blockly.Iot.InitializerBlock(sketch, state.globalSymbols);
  state.initializer.compile(sketch, state);
};

AI.Blockly.Iot.compileEventHandlers = function(sketch, state) {
  for (var k in state.eventSymbols) {
    if (state.eventSymbols.hasOwnProperty(k)) {
      state.eventSymbols[k].compile(state);
    }
  }
};

AI.Blockly.Iot.compileUserProcedures = function(sketch, state) {

};

AI.Blockly.Iot.GlobalDefinition = function(defBlock) {
  this.defBlock = defBlock;
  this.name = 'g$' + defBlock.getFieldValue('NAME');
};

AI.Blockly.Iot.InitializationBlock = function(defBlock) {
  this.defBlock = defBlock;
  this.contents = [];
};

AI.Blockly.Iot.InitializationBlock.prototype.compile = function(sketch, state) {
};

AI.Blockly.Iot.ProcedureBlock = function(defBlock) {
  this.defBlock = defBlock;
  this.contents = [];
  this.name = 'p$' + defBlock.getFieldValue('NAME');
};

AI.Blockly.Iot.ProcedureBlock.prototype.compile = function(sketch, state) {

};

AI.Blockly.Iot.EventHandlerBlock = function(defBlock) {
  this.defBlock = defBlock;
  this.contents = [];
  this.name = defBlock.instanceName + '$' + defBlock.eventName;
};

AI.Blockly.Iot.EventHandlerBlock.prototype.compile = function(sketch, state) {

};

AI.Blockly.Iot.link = function(state) {

};

AI.Blockly.Iot.createProgram = function(state) {
  var i;
  // write the header
  state.program.push(state.userProcs, state.strings, state.persistentGlobals, state.devices);
  var progLen = state.initializer.contents.length;
  for (i in state.eventSymbols) {
    if (state.eventSymbols.hasOwnProperty(i)) {
      progLen += state.eventSymbols[i].contents.length;
    }
  }
  for (i in state.procSymbols) {
    if (state.procSymbols.hasOwnProperty(i)) {
      progLen += state.procSymbols[i].contents.length;
    }
  }
  state.program.push(progLen & 0xFF, (progLen >> 8) & 0xFF);
};

AI.Blockly.Iot.scrub_ = function(block, code) {
  return code;
}

// -*- mode: javascript; js-indent-level: 2; -*-
// Copyright © MIT, All rights reserved
/**
 * @license
 * @fileoverview math.js translates the Blockly math blocks into opcodes for the App Inventor Iot
 * Embedded Companion.
 * @author Evan W. Patton (ewpatton@mit.edu)
 */

'use strict';

goog.provide('AI.Blockly.Iot.math');

AI.Blockly.Iot.ORDER_ATOMIC = 0;
AI.Blockly.Iot.ORDER_GROUPING = 1;
AI.Blockly.Iot.ORDER_UNARY = 2;
AI.Blockly.Iot.ORDER_MULTIPLY = 3;
AI.Blockly.Iot.ORDER_ADD = 4;
AI.Blockly.Iot.ORDER_SHIFT = 5;
AI.Blockly.Iot.ORDER_COMPARISON = 6;
AI.Blockly.Iot.ORDER_EQUALITY = 7;
AI.Blockly.Iot.ORDER_BITAND = 8;
AI.Blockly.Iot.ORDER_BITXOR = 9;
AI.Blockly.Iot.ORDER_BITOR = 10;
AI.Blockly.Iot.ORDER_AND = 11;
AI.Blockly.Iot.ORDER_OR = 12;
AI.Blockly.Iot.ORDER_TERNARY = 13;
AI.Blockly.Iot.ORDER_ASSIGNMENT = 14;
AI.Blockly.Iot.ORDER_COMMA = 15;
AI.Blockly.Iot.ORDER_NONE = 99;

AI.Blockly.Iot['math_number'] = function() {
  var num = this.getFieldValue('NUM');
  var parsedNum = window.parseFloat(num);
  if (num.indexOf('.') >= 0) {
    // floating point
    if (parsedNum == 0.0) {
      return [[0x0B], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 1.0) {
      return [[0x0C], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 2.0) {
      return [[0x0D], AI.Blockly.Iot.ORDER_ATOMIC];
    }
  } else {
    if (parsedNum == -1) {
      return [[0x02], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 0) {
      return [[0x03], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 1) {
      return [[0x04], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 2) {
      return [[0x05], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 3) {
      return [[0x06], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 4) {
      return [[0x07], AI.Blockly.Iot.ORDER_ATOMIC];
    } else if (parsedNum == 5) {
      return [[0x08], AI.Blockly.Iot.ORDER_ATOMIC];
    }
  }
};

AI.Blockly.Iot['math_compare'] = function() {
  return ['compare', AI.Blockly.Iot.ORDER_NONE];
};

AI.Blockly.Iot['math_arithmetic'] = function(mode) {
  var tuple = AI.Blockly.Iot.math_arithmetic.OPERATORS[mode],
      operator = tuple[0],
      order = tuple[1],
      code = [],
      argument0, argument1;
  argument0 = AI.Blockly.Iot.valueToCode(this, 'A', order) || [[0x03], AI.Blockly.Iot.ORDER_ATOMIC];
  argument1 = AI.Blockly.Iot.valueToCode(this, 'B', order) || [[0x03], AI.Blockly.Iot.ORDER_ATOMIC];
  argument0.pop();
  argument1.pop();
  code.push(argument0, argument1, operator);
  return [code, order];
};

AI.Blockly.Iot.math_arithmetic.OPERATORS = {
  ADD: [0x60, AI.Blockly.Iot.ORDER_ATOMIC],
  SUBTRACT: [0x64, AI.Blockly.Iot.ORDER_ATOMIC],
  MULTIPLY: [0x68, AI.Blockly.Iot.ORDER_ATOMIC],
  DIVIDE: [0x6C, AI.Blockly.Iot.ORDER_ATOMIC],
  POWER: []
};

AI.Blockly.Iot['math_arithmetic_list'] = function(mode) {
  var tuple = AI.Blockly.Iot.math_arithmetic.OPERATORS[mode],
      operator = tuple[0],
      order = tuple[1],
      code = [],
      i;
  for (i = 0; i < this.itemCount_; i++) {
    var value = AI.Blockly.Iot.valueToCode(this, 'NUM' + i, order);
    Array.prototype.push.apply(code, value);
  }
  for (i = 0; i < this.itemCount_ - 1; i++) {
    code.push(operator);
  }
  return [code, order];
};

AI.Blockly.Iot['math_add'] = function() {
  return AI.Blockly.Iot['math_arithmetic_list'].call(this, 'ADD');
};

AI.Blockly.Iot['math_subtract'] = function() {
  return AI.Blockly.Iot['math_arithmetic'].call(this, 'SUBTRACT');
};

AI.Blockly.Iot['math_multiply'] = function() {
  return AI.Blockly.Iot['math_arithmetic_list'].call(this, 'MULTIPLY');
};

AI.Blockly.Iot['math_divide'] = function() {
  return AI.Blockly.Iot['math_arithmetic'].call(this, 'DIVIDE');
};

AI.Blockly.Iot['math_power'] = function() {
  return AI.Blockly.Iot['math_arithmetic'].call(this, 'POWER');
};

// ---------------------------------------------------------------------------------
// P ( U S H )   B ( U T T O N )   S ( W I T C H )   G ( R O U P )
//
// Copyright (C) 2023-Present Wesley M. Conner
//
// LICENSE
// Licensed under the Apache License, Version 2.0 (aka Apache-2.0, the
// "License"), see http://www.apache.org/licenses/LICENSE-2.0. You may
// not use this file except in compliance with the License. Unless
// required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
// implied.
// ---------------------------------------------------------------------------------
#include wesmc.lUtils
#include wesmc.lPBSG
import com.hubitat.app.DeviceWrapper as DevW
import com.hubitat.hub.domain.Event as Event
import groovy.json.JsonOutput    // See wesmc.lUtils
import groovy.json.JsonSlurper   // See wesmc.lUtils

metadata {
  definition(
    name: 'PBSG',
    namespace: 'wesmc',
    author: 'Wesley M. Conner',
    importUrl: 'PENDING',
    singleThreaded: 'true'
  ) {
    // The attributes implied by capabilities are communicated/persisted
    // exclusively using sendEvent() - where the value is the attribute
    // that is being altered.
    capability 'Initialize'      // Commands: initialize()
    capability 'Configuration'   // Commands: configure()
    capability 'PushableButton'  // Attributes:
                                 //   - numberOfButtons: number
                                 //   - pushed: number
                                 // Commands: push(number)
    capability 'Refresh'         // Commands: refresh()
    command 'activate', [[
      name: 'button',
      type: 'string',
      description: 'The button name to activate'
    ]]
    command 'deactivate', [[
      name: 'button',
      type: 'string',
      description: 'The button name to deactivate'
    ]]
    command 'activateLastActive', [[
    ]]
    // The folliwing attributes are comunicated/persisted exclusively using
    // sendEvent() - where the value is the attribute that is being altered.
    attribute 'pbsg', 'map'
    attribute 'active', 'string'
    // Available for future use.
    //   - updateDataValue()
    //   - See https://docs2.hubitat.com/en/developer/device-object
    //   - Currently NOT used
  }
  preferences {
    input( name: 'buttons',
      title: "${b('Button Names')} (space delimited)",
      type: 'text',
      required: true
    )
    input( name: 'dflt',
      title: b('Default Button'),
      type: 'enum',
      multiple: false,
      options: (settings?.buttons?.tokenize(' ') ?: []) << 'not_applicable',
      defaultValue: 'not_applicable',
      required: true
    )
    input( name: 'instType',
      title: b('Type of PBSG'),
      type: 'text',
      defaultValue: 'pbsg',
      required: true
    )
    input( name: 'logLevel',
      title: b('Logging Threshold ≥'),
      type: 'enum',
      multiple: false,
      options: ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'],
      defaultValue: 'INFO',
      required: true
    )
  }
}

////
//// DEVICE LIFECYCLE METHODS
////

void installed() {
  // This method is called when a bare device is first constructed.
  // Settings ARE NOT likely to meet sufficiency requirements.
  logInfo('installed', 'Called, taking no action')
}

void uninstalled() {
  // This method is called on device tear down.
  logInfo('uninstalled', 'Called, taking no action')
}

void initialize() {
  // This method is called on hub startup (per capability "Initialize").
  ifSufficientSettingsConfigure('initialize()')
}

void refresh() {
  // This method is called (per capability "Refresh") when a refresh is
  // requested (manually or programmatically).
  ifSufficientSettingsConfigure('refresh()')
}

////
//// SETTINGS AND DERIVED STATE/ATTRIBUTES
////

void ifSufficientSettingsConfigure(String callingFn, Map candidate = null) {
  // If a candidate map is provided:
  //   - <candidate.X: value> us used in lieu of <settings.X: value>
  //   - If per-field sufficiency requirements are met:
  //     settings.X is updated to be candidate.X
  // Any failed sufficiency tests are reported in the Hubitat Log
  // Configuration is conditional on ALL sufficiency tests passing.
  logInfo('ifSufficientSettingsConfigure#A', "candidate: ${candidate}")
  ArrayList deficiency = []
  String buttonsStr = candidate?.buttons ?: settings.buttons
  ArrayList buttons = buttonsStr.tokenize(' ')
  String markDirty = buttonsStr
  logInfo('ifSufficientSettingsConfigure#A1', ['',
    "buttonStr: ${buttonStr}",
    "buttons: ${buttons}",
    "markDirty: ${markDirty}"
  ].join('<br/>'))
  markDirty.replaceAll(/[\W_&&[^_ ]]/, '▮')
  if (buttonsStr != markDirty) {
    deficiency << "${b('buttons')} has invalid chars (${markDirty})."
  //logInfo('ifSufficientSettingsConfigure#A2', "deficiency: ${deficiency}")
  } else if (buttonsStr == null || buttons.size() == 0) {
    deficiency << "No ${b('buttons')} were found."
  //logInfo('ifSufficientSettingsConfigure#A3', "deficiency: ${deficiency}")
  } else if (buttons.size() < 2) {
    deficiency << "The button count (${buttons}) is < 2."
  //logInfo('ifSufficientSettingsConfigure#A4', "deficiency: ${deficiency}")
  } else {
    // buttonsStr (from settings or candidate) meets requirements
    //settings.buttons = buttonsStr
    settings.buttons = buttons.join(' ')
  logInfo('ifSufficientSettingsConfigure#A5', "settings.buttons: ${settings.buttons}")
    // Also, retain the ArrayList in state
    atomicState.buttons = buttons
  logInfo('ifSufficientSettingsConfigure#A6', "atomicState.buttons: ${atomicState.buttons}")
  }
  logInfo('ifSufficientSettingsConfigure#B', "deficiency: ${deficiency}")
  String dflt = candidate?.dflt ?: settings.dflt
  if (dflt && !buttons?.contains(dflt)) {
    deficiency << "The default button (${dflt}) is not in buttons (${buttons})."
  } else {
    // dflt (from settings or candidate) meets requirements
    settings.dflt = dflt
  }
  logInfo('ifSufficientSettingsConfigure#C', "deficiency: ${deficiency}")
  String instType = candidate?.instType ?: settings.instType
  if (!instType) {
    deficiency << "The ${b('instType')} is null."
  } else {
    // instType (from settings or candidate) meets requirements
    settings.instType = instType
  }
  logInfo('ifSufficientSettingsConfigure#D', "deficiency: ${deficiency}")
  String logLevel = candidate?.logLevel ?: settings.logLevel
  ArrayList allowedLevels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR']
  if (!allowedLevels.contains(logLevel)) {
    deficiency << "The logLevel (${logLevel}) is not found in ${allowedLevels}."
  } else {
    // logLevel (from settings or candidate) meets requirements
    settings.logLevel = logLevel
  }
  if (deficiency.size() > 0) {
    logError('ifSufficientSettingsConfigure', ["FAILED for ${callingFn}",
      deficiency.join('<br/>')
    ].join('<br/>'))
  } else {
    configure()
  }
}

void update_SettingsDerivatives() {
  // STEP 1 - Adjust state fields that only depend on settings.
  atomicState.buttons = settings.buttons.tokenize(' ')
  logInfo('update_SettingsDerivatives#A', "atomicState.buttons: ${atomicState.buttons}")

  // STEP 2 - Update changed attributes that only depend on settings.
  // ==> numberOfButtons
  Integer last_numberOfButtons = device.currentValue('numberOfButtons')
  Integer curr_numberOfButtons = atomicState.buttons.size()
  logInfo('update_SettingsDerivatives#B', "numberOfButtons last: ${last_numberOfButtons}, curr:${curr_numberOfButtons}")
  if (last_numberOfButtons != curr_numberOfButtons) {
    sendEvent(
      name: 'numberOfButtons',
      isStateChange: true,
      value: curr_numberOfButtons,
      unit: '#',
      descriptionText: "numberOfButtons ${last_numberOfButtons} → ${curr_numberOfButtons}"
    )
  }
  // ==> buttonsToPostions
  Map last_b2P = device.currentValue('buttonsToPostions')
  Map curr_b2P = atomicState.buttons.withIndex().collectEntries { b, i ->
    [b, i+1]
  }
  logInfo('update_SettingsDerivatives#C', "buttonsToPostions last: ${last_b2P}, curr:${curr_b2P}")
  if (last_b2P != curr_b2P) {
    sendEvent(
      name: 'buttonsToPositions',
      isStateChange: true,
      value: curr_b2P,
      unit: '#',
      descriptionText: "buttonsToPositions: ${curr_b2P}"
    )
  }
  logInfo('update_SettingsDerivatives#C', "pbsg Map: [a: 'apple', b: 'banana', c: 'cantelope']")
  sendEvent(
    name: 'pbsg',
    isStateChange: true,
    value: [a: 'apple', b: 'banana', c: 'cantelope'],
    unit: null,
    descriptionText: "Testing Map as a value"
  )
  logInfo('update_SettingsDerivatives#D', 'At exit')
}

void updated() {
  // PROCESS MANUAL SETTINGS CHANGE
  //   - This method is called when a human uses the Hubitat GUI's Device
  //     drilldown page to edit preferences (aka settings) AND presses
  //     'Save Preferences'.
  //   - Any issues are reported via the Hubitat logs.
  //   - The configure() method is callsed iff sufficient settings exist.
  ifSufficientSettingsConfigure('updated()')
}

void parse(String jsonConfig) {
  // PROCESS PROGRAMMATIC SETTINGS CHANGE
  //   - This method is exposed to clients and provides a programmatic
  //     mechanism for udpdating preferences (aka settings). Individual
  //     settings are updated iff per-field validation tests pass.
  //   - Any issues are reported via the Hubitat logs.
  //   - The configure() method is called iff sufficient settings exist.
  // Expected JSON
  //   A serialized JSON Map with four (OPTIONAL) keys:
  //     toJson([
  //        buttons: <String>,     // Adjusts settings.buttons
  //           dflt: <String>,     // Adjusts settings.dflt
  //       instType: <String>,     // Adjusts settings.instType
  //       logLevel: <enum>        // Adjusts settings.logLevel
  //     ])
  logInfo('parse', ["Processing provided JSON:", '=====', jsonConfig,
    '====='].join('<br/>'))
  Map parms = fromJson(jsonConfig)
  // The following method replaces settings.X with parms.X (on a field
  // by field basis) when per-field sufficiency requirements are met.
  ifSufficientSettingsConfigure('parse()', parms)
}

String currentSettingsHtml() {
  return [
    b('SETTINGS:'),
    settings.collect { k, v -> "${b(k)}: ${v}" }.join(', ')
  ].join('<br/>')
}

////
//// PBSG METHODS AND RELATED STATE/ATTRIBUTES
////












Map getCurr_pushed() {
  Map b2p = device.currentValue('buttonsToPositions')
  Map pbsg = device.currentValue('pbsg')
  return b2p?."${pbsg.active}"
}

Map getCurr_buttonsToPositions() {
}

Map getCurr_pbsg() {
}





void sendEvent_pushed() {
  // TBD: Only issue if active has changed
  sendEvent([
    name: 'pushed',
    isStateChange: true,
    value: TBD,
    unit: null,
    descriptionText: ''
  ])
}

void sendEvent_buttonsToPositions() {
  // TBD: Only issue if Map has changed
  sendEvent([
    name: 'buttonsToPositions',
    isStateChange: true,
    value: TBD,
    unit: null,
    descriptionText: ''
  ])
}

void sendEvent_pbsg() {
  // TBD: Only issue if pbsg has changed
  sendEvent([
    name: 'pbsg',
    isStateChange: true,
    value: [
      active: null,
      lifo: []
    ],
    unit: null,
    descriptionText: ''
  ])
}

String currentAttributesHtml() {
  return [b('CURRENT ATTRIBUTES:'),
    "${b('numberOfButtons:')} ${getCurr_numberOfButtons()}",
    "${b('pushed:')} ${getCurr_pushed()}",
    "${b('buttonsToPositions:')} ${getCurr_buttonsToPositions()}",
    "${b('pbsg:')} ${getCurr_pbsg()}"
  ].join('<br/>')
}

////
//// STATE METHODS
////

String currentStateHtml() {
  return [b('CURRENT STATE:'),
  ].join('<br/>')
}

Map getButtonsToPosition(ArrayList buttons) {
  buttons.withIndex().collectEntries { b, i -> [b, i] }
}

void configure() {
  // This function in normally called by ifSufficientSettingsConfigure()
  // It is possible to bypass the sufficiency tests and call configure
  // directly due to the presence of capability 'Configuration'.
  update_SettingsDerivatives()
  logError('configure', 'YOU ARE HERE - NEED TO CONVERT TO NEW MODEL')
  Map pbsg = [
    // Seed PBSG with configuration (and derived) data
    buttons: buttons,                                 // ArrayList
    dflt: settings.dflt,                              // String
    isntType: settings.instType,                      // String
    logLevel: settings.logLevel,                      // String
    numberOfButtons: buttons.size(),                  // Integer
    // Add initially-empty state fields
    active: null,                                     // String
    lifo: [],                                         // ArrayList
    pushed: null,                                     // Integer
    buttonToPosition: getButtonsToPosition(buttons),  // Map
    display: null                                     // String
  ]
  ArrayList expectedChildDnis = []
  pbsg.buttons.each { button ->
logInfo('configure#A', "button: ${button}")
    DevW vsw = getOrCreateVswWithToggle(device.getLabel(), button)
    expectedChildDnis << vsw.getDeviceNetworkId()
logInfo('configure#B', "vsw: ${vsw}")
    pbsg.lifo.push(button)
logInfo('configure#C', "lifo: ${pbsg.lifo}")
    if (switchState(vsw) == 'on') {
      // Move the button from the LIFO to active
      pbsg_ActivateButton(pbsg, button)
logInfo('configure#D', "active: ${pbsg.active}, lifo: ${pbsg.lifo}")
    }
    if (!pbsg.active) {
      pbsg_EnforceDefault(pbsg)
logInfo('configure#E', "active: ${pbsg.active}, lifo: ${pbsg.lifo}")
    }
  }
  ArrayList currentChildDnis = device.getChildDevices().collect { d ->
    d.getDeviceNetworkId()
  }
  ArrayList orphanedDevies = currentChildDnis.minus(expectedChildDnis)
  logInfo('configure', ['',
    "currentChildDnis: ${currentChildDnis}",
    "expectedChildDnis: ${expectedChildDnis}",
    "orphanedDevies: ${orphanedDevies}"
  ])
  putPbsgState(pbsg)  // Save to atomicState and generate PBSG events.
  logWarn('configure', 'TBD - Prune any unused child VSWs')
}

Map getCore() {
  // "Check out" core fields and operate on them as a unit.
  return atomicState.core
}

void putCore(Map core) {
  // Detect changes in core fields and if changed:
  //   - "Check in" core fields
  //   - Reconcile changes to child VSWs
  //   - Issue appropriate sendEvent()
  if (!core.equals(atomicState.core)) {
    Boolean activeChange = core.active != atomicState.core.active
    atomicState.core = core
    pbsg_ReconcileVswsToState(pbsg)
    String briefDescription = "active: ${pbsg.active}, button: ${currentButton}"
    sendPbsgEvent(briefDescription)
    if (activeChange) { sendPushableEvent(briefDescription) }
  }
}

void activate(String button) {
  // Intended for a one-shot change (vs ganging with other methods)
  pbsg = getPbsgState(device.getLabel())
  pbsg_ActivateButton(pbsg, button)
  logInfo('activate', "After adjustment, ${showSettings()}")
  putPbsgState(pbsg)
}

boolean pbsg_ActivateButton(Map pbsg, String button) {
  // Intended for ganged operations (PBSG get/put occurs externally)
  // Assumed: pbsg != null
  // Returns true on a PBSG state change
  boolean result = false
  if (pbsg.active == button) {
    logWarn('pbsg_ActivateButton', "${b(button)} was already activated")
  } else {
    if (pbsg.active) {
      pbsg.lifo.push(pbsg.active)
      result = true
    }
    if (pbsg.lifo.contains(button)) {
      pbsg.active = button
      pbsg.lifo.removeAll([button])
      result = true
    } else {
      logError('pbsg_ActivateButton', "Unable to find button ${b(button)}")
      // The PBSG's state is NOT changed.
    }
  }
  return result
}

void deactivate(String button) {
  // Intended for a one-shot change (vs ganging with other methods)
  pbsg = getPbsgState(device.getLabel())
  pbsg_DeactivateButton(pbsg, button)
  logInfo('deactivate', "After adjustment, ${showSettings()}")
  putPbsgState(pbsg)
}

boolean pbsg_DeactivateButton(Map pbsg, String button) {
  // Intended for ganged operations (PBSG get/put occurs externally)
  // Assumed: pbsg != null
  // Returns true on a PBSG state change
  boolean result = false
  if (button == pbsg.dflt) {
    logWarn(
      'pbsg_DeactivateButton',
      "Ignoring the requested deactivation of default button ${b(button)}"
    )
  } else if (pbsg.active == button) {
    if (pbsg.dflt) {
      // Swap currentlt active button with default button
      logTrace('pbsg_Deactivate', "Activating default ${b(pbsg.dflt)}")
      pbsg_ActivateButton(pbsg, pbsg.dflt)
    } else {
      // Deactivate active button only
      pbsg.lifo.push(pbsg.active)
    }
    result = true
  } else if (pbsg.lifo.contains(button)) {
    logWarn('pbsg_DeactivateButton', "${b(button)} was already deactivated")
  } else {
    logError('pbsg_DeactivateButton', "${b(button)} was not found")
  }
  return result
}


void activateLastActive() {
  // Intended for a one-shot change (vs ganging with other methods)
  pbsg = getPbsgState(device.getLabel())
  pbsg_ActivateLastActive(pbsg)
  logInfo('activateLastActive', "After adjustment, ${showSettings()}")
  putPbsgState(pbsg)
}

boolean pbsg_ActivateLastActive(Map pbsg) {
  // Intended for ganged operations (PBSG get/put occurs externally)
  // Assumed: pbsg != null
  // -----------------------------------------------------------------
  // I M P O R T A N T   (circa Q2'24)
  //   The Groovy ListArray implementation operates in reverse order
  //     - push(item) APPENDS the item to [] instead of PREPENDING it
  //     - pop() retrieves the last item pushed, at 'lifo.size() - 1'
  //       rather than position '0' per current Groovy docs.
  // -----------------------------------------------------------------
  Integer latestPushIndex = pbsg.lifo.size() - 1
  return pbsg_ActivateButton(pbsg, pbsg.lifo[latestPushIndex])
}

boolean pbsg_EnforceDefault(Map pbsg) {
  // Assumed: pbsg != null
  // Returns true on a PBSG state change
  boolean result = false
  if (pbsg && !pbsg.active && pbsg.dflt) {
    logInfo('pbsg_EnforceDefault', "Activating default ${b(pbsg.dflt)}")
    result = pbsg_ActivateButton(pbsg, pbsg.dflt)
  }
  return result
}

// Process Methods on behalf of Child Devices

void vswWithToggleOn(DevW d) {
  String button = vswToButtonName(d)
  activateButton(button)
}

void vswWithToggleOff(DevW d) {
  String button = vswToButtonName(d)
  deactivateButton(button)
}

void vswWithTogglePush(DevW d) {
  String button = vswToButtonName(d)
  (active == button)
    ? deactivateButton(button)
    : activateButton(button)
}

// Direct Child State Changes

void turnOnVsw(String button) {
  DevW d = fetchVswWithToggle(button)
  d.parse([
    name: 'switch',
    value: 'on',
    descriptionText: "${d.name} was turned on",
    isStateChange: true
  ])
}

void turnOffVsw(String button) {
  DevW d = fetchVswWithToggle(button)
  d.parse([
    name: 'switch',
    value: 'off',
    descriptionText: "${d.name} was turned off",
    isStateChange: true
  ])
}

// Methods Expected for Advertised Capabilities

void push(Integer buttonNumber) {
  String button = atomicState.bNumberToBName[buttonNumber]
  logInfo('push', "Received ${button} (#${buttonNumber})")
  activateButton(button) && syncVswsAndIssueCallback()
}

////
//// TO BE CONVERTED
////

String pbsg_ButtonState(Map pbsg, String button) {
  // Assumed: pbsg != null
  // The supplied PBSG is used without making any changes to its state
  if (button != null) {
    String tag = (button == pbsg.dflt) ? '*' : ''
    String summary = "${tag}<b>${button}</b> "
    DevW vsw = getChildDevice("${pbsg.name}_${button}")
    String swState = switchState(vsw)
      ?: logError('pbsg_ButtonState', "switchState() failed for button (${button}).")
    if (swState == 'on') {
      summary += '(<b>on</b>)'
    } else if (swState == 'off') {
      summary += '(<em>off</em>)'
    } else {
      logError('pbsg_ButtonState', "Encountered swState: >${swState}<")
      summary += '(--)'
    }
  } else {
    logError('pbsg_ButtonState', 'button arg is NULL')
  }
}

String pbsg_State(Map pbsg) {
  // Assumed: pbsg != null
  // The supplied PBSG is used without making any changes to its state
  //   IMPORTANT:
  //     LIFO push() and pop() are supported, *BUT* pushed items are
  //     appended (NOTE PREPENDED); so, the list needs to be reverse
  //     to look like a FIFO.
  String result
  if (pbsg) {
    result = "${b(pbsg.name)}: "
    result += (pbsg.active) ? "${pbsg_ButtonState(pbsg, pbsg.active)} " : ''
    result += '← ['
    result += pbsg.lifo?.reverse().collect { button -> pbsg_ButtonState(pbsg, button) }.join(', ')
    result += ']'
  } else {
    logError('pbsg_State', 'Received null PBSG parameter.')
  }
  return result
}

//// UNUSED / UNSUPPORTED

void parse(ArrayList actions) {
  // This method is reserved for interaction with FUTURE parent devices.
  logWarn('parse', ['parse(ArrayList) ignored:',
    actions.join('<br/>')
  ].join('<br/>'))
}


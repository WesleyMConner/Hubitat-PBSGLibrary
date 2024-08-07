/* D E M O   P B S G
 *
 * Copyright (C) 2023-Present Wesley M. Conner
 *
 * LICENSE
 * Licensed under the Apache License, Version 2.0 (aka Apache-2.0, the
 * "License"), see http://www.apache.org/licenses/LICENSE-2.0. You may
 * not use this file except in compliance with the License. Unless
 * required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 */

// Wmc.WmcUtilsLib_1.0.0
//   - The imports below support the methods in this library.
//   - Groovy Linter generates NglParseError due to the Hubitat #include.
#include Wmc.WmcUtilsLib_1.0.0
import com.hubitat.app.ChildDeviceWrapper as ChildDevW
import com.hubitat.app.DeviceWrapper as DevW
import com.hubitat.app.InstalledAppWrapper as InstAppW
import com.hubitat.hub.domain.Event as Event
import groovy.json.JsonOutput as JsonOutput
import groovy.json.JsonSlurper as JsonSlurper
import groovy.transform.Field
import java.lang.Math as Math
import java.lang.Object as Object
import java.util.concurrent.ConcurrentHashMap

definition (
  name: 'Demo-PBSG',
  namespace: 'Wmc',
  author: 'Wesley M. Conner',
  description: 'Demonstrate PushButtonSwitchGroup (PBSG) Functionality',
  singleInstance: true,
  iconUrl: '',
  iconX2Url: ''
)

// DEMONSTRATION EVENT HANDLERS

void handle_numberOfButtons(Event e) {
  logWarn('handle_numberOfButtons', "${eventDetails(e)}")
  if (e.name == 'numberOfButtons') {
    Integer val = e.value.toInteger()
    logInfo(
      'handle_numberOfButtons',
      "${hued(e)}: ${e.descriptionText}"
    )
  } else {
    logError('handle_numberOfButtons', "Unexpected event: ${eventDetails(e)}")
  }
}

void handle_pushed(Event e) {
  if (e.name == 'pushed') {
    Integer val = e.value.toInteger()
    logInfo('handle_pushed', ["${hued(e)}: ${e.descriptionText}",
      "${e.name} = ${b(val)}"
    ])
  } else {
    logError('handle_pushed', "Unexpected event: ${eventDetails(e)}")
  }
}

void handle_jsonPbsg(Event e) {
  if (e.name == 'jsonPbsg') {
    Map pbsg = parseJson(e.value)
    logInfo('handle_jsonPbsg', [ "${hued(e)}: ${e.descriptionText}",
      (getChildDevice(e.displayName).pbsg_StateHtml(pbsg)),
      bMap(pbsg)
    ])
  } else {
    logError('handle_jsonPbsg', "Unexpected event: ${eventDetails(e)}")
  }
}

void handle_active(Event e) {
  if (e.name == 'active') {
    logInfo('handle_active', "${hued(e)}: ${e.descriptionText}")
  } else {
    logError('handle_active', "Unexpected event: ${eventDetails(e)}")
  }
}

//// SOLICIT DATA FOR ONE OR MORE PBSG INSTANCES

Boolean isDynamic() {
  return (device?.getName()) ? false : true
}

void solicitPbsgNames() {
  input(
    name: 'pbsgNames',
    title: [
      h3('Enter one or more PBSG Names'),
      '- Separate names with spaces',
      "- Avoid special characters in names ('_' is allowed)",
      '- The names will be used to create PBSG devices'
    ].join('<br/>'),
    // defaultValue: 'weekdays',  // USED FOR ITERATIVE TESTING !
    type: 'text',
    required: true,
    submitOnChange: true
  )

  paragraph alert([b('IMPORTANT:'),
    "Use ⏎ to save your PBSG name(s).",
    "Then (${b('BEFORE')} clicking ${b('Next')}):",
    bullet2('Populate PBSG button names.'),
    bullet2("Select the PBSG's default button.")
  ].join('<br/>'))
}

ArrayList getPbsgNames() {
  String namesStr = settings.pbsgNames
  // Drop non-word (\W) chars except for underscores and spaces.
  namesStr?.replaceAll(/[\W_&&[^_ ]]/, '')
  // Return one array entry for every non-null, space-delimited word.
  return namesStr?.tokenize(' ')
}

void solicitButtonNames(String prefix) {
  String header = "${b('Button Names')} (space delimited)"
  if (prefix) { header << "for ${prefix}" }
  input(
    name: "${prefix}buttons",
    title: h3(header),
    // defaultValue: 'mon tue wed thu fri',  // USED FOR ITERATIVE TESTING !
    type: 'text',
    required: true,
    submitOnChange: dynamic
  )
}

ArrayList getButtonNames(String prefix) {
  // Tokenize the space-delimited buttons to produce the ArrayList
  String buttonsString = settings?."${prefix}buttons"
  // Drop non-word (\W) chars except for underscores and spaces.
  buttonsString?.replaceAll(/[\W_&&[^_ ]]/, '')
  // Return one array entry for every non-null, space-delimited word.
  return buttonsString?.tokenize(' ')
}

void solicitDefaultButton(String prefix) {
  String header = 'Default Button'
  if (prefix) { header << "for ${prefix}" }
  input(
    name: "${prefix}dflt",
    title: h3(header),
    type: 'enum',
    multiple: false,
    options: [*getButtonNames(prefix), 'not_applicable'],
    defaultValue: defaultButton(prefix) ?: 'not_applicable',
    //defaultValue: 'thu',  // USED FOR ITERATIVE TESTING !
    submitOnChange: isDynamic(),
    required: true
  )
}

String defaultButton(String prefix) {
  return settings?."${prefix}dflt"
}

void solicitPbsgConfig(String pbsgName) {
  // OVERVIEW
  //   - This method is called from within an App page section{...}
  //   - The client first populates the names of the PBSG buttons.
  //   - Once the list of buttons exists, the user selects a
  //     default (dflt) button or 'not_applicable'
  //   - All exposed fields can be edited until data entry is "Done".
  paragraph(h1("Provide '${b(pbsgName)}' Configuration Data"))
  ArrayList buttonList = getButtonNames("${pbsgName}_")
  Integer buttonCnt = buttonList?.size()
  // Provide feedback to the client (dynamically).
  ArrayList feedback = []
  switch (buttonCnt) {
    case 1:
      feedback << h1('Oops only one button was detected')
      feedback << "${b('Buttons')}: ${buttonList}, ${b('Button Count')}: ${buttonCnt}"
      // Deliberately fall through to the next case!
    case null:
      feedback << h2("Create at least two buttons for PBSG ${b(pbsgName)}")
      feedback << i("Enter button names ${b('delimited with spaces ')}")
      break
    default:
      feedback << h2("Button names for PBSG ${b(pbsgName)}")
      feedback << i('button names are delimited with spaces')
  }
  paragraph(h2(feedback.join('<br/>')))
  solicitButtonNames("${pbsgName}_")
  if (buttonCnt > 2) {
    solicitDefaultButton("${pbsgName}_")
  }
}

// SOLICIT PBSG ACTIONS AND BUILD AD HOC PBSG TEST SEQUENCES

void solicitNextAction(String pbsgName) {
  String header = 'Add a Test Action'
  if (pbsgName) { header << "for ${pbsgName}" }
  input(
    name: "${pbsgName}_nextAction",
    title: h3(header),
    type: 'enum',
    options: getTestActions(pbsgName),
    required: false,
    width: 3,
    submitOnChange: isDynamic()
  )
}

void removeNextAction(String pbsgName) {
  // Clear this setting when it is important to solicit fresh data entry
  // from a client without defaulting to the client's prior data entry.
  app.removeSetting("${pbsgName}_nextAction")
}

String getNextAction(String pbsgName) {
  return settings?."${pbsgName}_nextAction"
}

void solicitTestSequence(String testSequenceJson, String pbsgName) {
  String header = 'Save the Test Sequence'
  if (prefix) { header << "for ${pbsgName}" }
  input(
    name: "${pbsgName}_testSequence",
    title: h3(header),
    type: 'textarea',
    defaultValue: testSequenceJson,
    /*
    defaultValue: toJson([  // USED FOR ITERATIVE TESTING !
      "mon_Activate",
      "wed_Activate",
      "wed_Deactivate",
      "fri_Activate",
      "fri_Deactivate",
      "thu_Deactivate",
      "fri_Activate",
      "5_VswPush",
      "5_VswPush",
      "tue_Activate",
      "wed_Deactivate",
      "wed_Deactivate",
      "fri_Activate",
      "5_VswPush",
      "1_VswPush",
      "tue_Activate",
      "tue_Deactivate"
    ]),
    */
    required: true,
    width: 9,
    submitOnChange: isDynamic()
  )
}

void removeTestSequence(String pbsgName = null) {
  // Clear this setting when it is important to solicit fresh data entry
  // from a client without defaulting to the client's prior data entry.
  app.removeSetting("${pbsgName}_testSequence")
}

ArrayList getTestSequence(String pbsgName) {
  ArrayList result = []
  if (settings) {
    String settingsKey = "${pbsgName}_testSequence"
    String value = settings."${settingsKey}"
    if (value) {
      result = parseJson(value)
    } else {
      logWarn('getTestSequence', "No setting ${settingsKey} yet")
    }
  } else {
    logWarn('getTestSequence', 'Preferences (settings) are not yet available.')
  }
  return result
}

// SUPPORT FOR AD HOC PBSG TESTING

ArrayList getTestActions(String pbsgName) {
  // Develop Available Test Sequence Options
  ArrayList buttons = getButtonNames("${pbsgName}_")
  Map buttonToNumber = buttons.withIndex().collectEntries{ b, i ->
    [(b), i+1]
  }
  ArrayList testActions = new ArrayList()
  buttons.each{ b ->
    testActions << "${b}_Activate"
    testActions << "${b}_Deactivate"
    testActions << "${buttonToNumber[b]}_VswPush"
  }
  return testActions
}

void buildTestSequence(String pbsgName, ArrayList testOptions) {
  ArrayList testSeqList = getTestSequence(pbsgName) ?: []
  // Add the most recent 'next action' to the 'test sequence'.
  String nextAction = getNextAction(pbsgName)
  if (nextAction) {
    testSeqList.add(nextAction)  // Add the latest item to the ArrayList
    removeNextAction(pbsgName)     // Force fresh data soliciation
    removeTestSequence(pbsgName)   // Force fresh data soliciation
  }
  // Update the JSON version of the ArrayList
  String testSequenceJson = toJson(testSeqList)
  // Solicit a new Next Action
  solicitNextAction(pbsgName)
  // Present the latest Test Squence Json (for settings preservation)
  solicitTestSequence(testSequenceJson, pbsgName)
}

// DEFINE THREE DEMO DATA COLLECTION PAGES

preferences {
  page(name: 'page1_CreatePBSGs', nextPage: 'page2_TestActions')
  page(name: 'page2_TestActions', nextPage: 'page3_TestSequences')
  page(name: 'page3_TestSequences')
}

Map page1_CreatePBSGs() {
  return dynamicPage(
    name: 'page1_CreatePBSGs',
    title: h1('Page 1 of 3 - Define PBSG Instance(s)'),
    uninstall: true
  ) {
    section {
      solicitPbsgNames()
      ArrayList pbsgNames = getPbsgNames()
      pbsgNames.each{ pbsgName -> solicitPbsgConfig(pbsgName) }
    }
  }
}

Map page2_TestActions() {
  return dynamicPage(
    name: 'page2_TestActions',
    title: [
      h1('Page 2 of 3 - List per-PBSG available Test Actions'),
      h2('VswWithToggle devices support on(), off() AND push() to toggle')
    ].join('<br/>'),
    uninstall: true
  ) {
    section {
      ArrayList pbsgNames = settings.pbsgNames?.tokenize(' ')
      paragraph([
        'On the next page ...',
        "  You will create an ad hoc ${b('Test Sequence')} per PBSG",
        "    - A ${b('Test Sequence')} includes one or more ${b('Test Actions')}",
        "    - ${b('Test Actions')} can be repeated in a ${b('Test Sequence')}",
        '    - Press "Next" to continue'
      ].join('<br/>'))
      pbsgNames.each{ pbsgName ->
        ArrayList testActions = getTestActions(pbsgName)
        paragraph(h2("${b('Test Actions')} for the ${b(pbsgName)}"))
        paragraph("  - ${testActions.join('<br/>  - ')}")
      }
    }
  }
}

Map page3_TestSequences() {
  return dynamicPage(
    name: 'page3_TestSequences',
    title: h1('Page 3 of 3 - Create per-PBSG Test Sequences'),
    install: true,
    uninstall: true
  ) {
    section {
      paragraph([
        h2("To build a ${b('Test Sequence')}:"),
        "  - Select a ${b('Test Action')}",
        "  - Use ⏎ to append the ${b('Test Action')} to the ${b('Test Sequence')}",
        "  - Repeat until the ${b('Test Sequence')} is complete",
        '  - Press "Done" to build the PBSGs and run the Test Sequences',
        '  - Watch Hubitat logs for Test Actions and PBSG results'
      ].join('<br/>'))
      // Develop Available Test Sequence Options
      ArrayList pbsgNames = settings.pbsgNames?.tokenize(' ')
      pbsgNames.each{ pbsgName ->
        ArrayList testActions = getTestActions(pbsgName)
        buildTestSequence(pbsgName, testActions)
      }
      paragraph(
        "Test results appear in the Hubitat log after ${b('Done')} is pressed."
      )
    }
  }
}

void installed() {
  // Called when a bare device is first constructed.
  unsubscribe()
  exercisePbsg()
}

void updated() {
  // Called when a human uses the Hubitat GUI's Device drilldown page to edit
  // preferences (aka settings) AND presses 'Save Preferences'.  unsubscribe()
  exercisePbsg()
}

void uninstalled() {
  // Called on device tear down.
}

ChildDevW getOrCreatePBSG(String pbsgName) {
  String dni = pbsgName   // Should NOT contain special chars except for '_'
  String name = dni.replaceAll('_', ' ') // Delim ' ' avoids special chars
  // Device Network Identifiers DO NOT include white space.
  // Device Names (exposed to Alexa ...) DO NOT include special characters.
  ChildDevW d = getChildDevice(dni)
  if (d) {
    logTrace('getOrCreatePBSG', "Using existing ${hued(d)}")
  } else {
    d = addChildDevice(
      'Wmc',   // namespace
      'PBSG',    // typeName
      dni,       // Device Network Identifier (<pbsgName>_<button>)
      [
        isComponent: true,   // Lifecycle is tied to parent
        name: name,          // "PBSG <pbsgName>"
        label: name          // "PBSG <pbsgName>"
      ]
    )
    logWarn('getOrCreatePBSG', "Created new ${hued(d)}")
  }
  return d
}

void subscribeHandler(ChildDevW issuer, String attribute) {
  String hdlr = "handle_${attribute}"
  logTrace(
    'subscribeHandler',
    "Subscribing ${b(hdlr)} to ${hued(issuer)} events."
  )
  subscribe(issuer, attribute, hdlr, ['filterEvents': true])
}

void initialize() {
  // Called on hub startup (per capability "Initialize").
  exercisePbsg()
}

void exercisePbsg() {
  setLogLevel('TRACE')  // Use 'INFO' to reduce logging.
  // For each pbsgName:
  //   - Process the user-provided 'settings' into a PBSG configuration Map.
  //   - The PBSG configuration Map is preserved as a top-level Map in state
  //   - The PBSG configuration Map is subsequently expanded to become a
  //     a PBSG instance.
  //   - Top-level Maps in state are efficiently processed with the
  //     atomicState() and getCCS().updateMapValue(...).
  ArrayList pbsgNames = settings.pbsgNames?.tokenize(' ')
  // Build the PBSG and run
  pbsgNames.each { pbsgName ->
    // Per target PBSG
    //   - Create an empty PBSG instance
    //   - Leverage the config() method to convey the structure.
    //   - Issue available test actions to exercise the PBSG.
    //   - Watch relevant Handlers to review resulting PBSG changes.
    if (pbsgName) {
      ChildDevW pbsg = getOrCreatePBSG(pbsgName)
      subscribeHandler(pbsg, 'numberOfButtons')
      subscribeHandler(pbsg, 'pushed')
      subscribeHandler(pbsg, 'jsonPbsg')
      subscribeHandler(pbsg, 'active')
      // Assemble solicited configure data (for the current PBSG) and use it to
      // configure the current PBSG via 'pbsg.parse(String json)'.
      logInfo('exercisePbsg', "Configure PBSG ${b(pbsgName)}")
      String prefix = "${pbsgName}_"
      Map prefs = [
        buttons: getButtonNames(prefix).join(' '),
        dflt: defaultButton(prefix),
        instType: 'pbsg',
        logLevel: 'TRACE'  // 'INFO'
      ]
      String jsonPrefs = toJson(prefs)
      pbsg.config(jsonPrefs, 'Demo-PBSG configuring PBSG using JSON')
      // BUILD AND RUN THE SOLICITED TEST SEQUENCES.
      ArrayList testSeqList = getTestSequence(pbsgName)
      logTrace(
        'exercisePbsg',
        "PBSG ${b(pbsgName)} Test Sequence, ${bList(testSeqList)}"
      )
      Integer actionCnt = testSeqList.size()
      // Call the appropriate PBSG method per Test Action.
      testSeqList?.eachWithIndex{ testAction, index ->
        String actionLabel = "${hued(pbsg)} action ${index + 1} of ${actionCnt}"
        String ref = "Demo-PBSG ${pbsgName} action ${index + 1} of ${actionCnt}"
        ArrayList tokenizedAction = testAction.tokenize('_')
        if (tokenizedAction.size() == 2) {
          String target = tokenizedAction[0]
          String action = tokenizedAction[1]
          logInfo('exercisePbsg', "${i(actionLabel)}: ${b(action)} ${b(target)}")
          String reference = "${index}: ${testAction}"
          switch (action) {
            case 'Activate':
              pbsg.activate(target, ref)
              break
            case 'Deactivate':
              pbsg.deactivate(target, ref)
              break
            case 'VswOn':
              pbsg.simulateVswOn(target, ref)
              break
            case 'VswOff':
              pbsg.simulateVswOff(target, ref)
              break
            case 'VswPush':
              Integer buttonNumber = safeParseInt(target)  // 1..N not 0 based.
              ArrayList buttonsList = getButtonNames(prefix)
              String button = buttonsList[buttonNumber - 1]
              pbsg.push(buttonNumber, ref)
              break
            default:
              logError(
                'exercisePbsg',
                "Unknown action: ${action} on button: ${button}"
              )
          }
        } else {
          logError(
            'exercisePbsg',
            "For ${i(actionLabel)} tokenize('_') failed for >${testAction}<.")
        }
      }
    } else {
      logError('exercisePbsg', 'Argument "pbsgName" was null')
    }
  }
  // LOCATE AND DELETE ANY ORPHANED CHILD DEVICES
  ArrayList childDNIs = getChildDevices().collect{d -> d.getDeviceNetworkId()}
  ArrayList expectedDNIs = []
  pbsgNames.each { pbsgName -> expectedDNIs.add(pbsgName) }
  ArrayList orphanedDNIs = childDNIs.minus(expectedDNIs)
  logTrace('exercisePbsg', ['',
    "   ${b('childDNIs')}: ${childDNIs}",
    "${b('expectedDNIs')}: ${expectedDNIs}",
    "${b('orphanedDNIs')}: ${orphanedDNIs}"
  ])
  orphanedDNIs.each{ dni ->
    logWarn('exercisePbsg', "Deleting orphaned device w/ DNI: ${b(dni)}")
    deleteChildDevice(dni)
  }
}

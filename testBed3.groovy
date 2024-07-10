// ---------------------------------------------------------------------------------
// T E S T B E D 3
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
// For reference:
//   Unicode 2190 ← LEFTWARDS ARROW
//   Unicode 2192 → RIGHTWARDS ARROW

// The Groovy Linter generates NglParseError on Hubitat #include !!!
#include WesMC.lUtils
import com.hubitat.app.ChildDeviceWrapper as ChildDevW
import com.hubitat.app.DeviceWrapper as DevW
import com.hubitat.app.InstalledAppWrapper as InstAppW
import com.hubitat.hub.domain.Event as Event
import groovy.json.JsonOutput as JsonOutput
import groovy.json.JsonSlurper as JsonSlurper
import java.lang.Math as Math
import java.lang.Object as Object

import groovy.transform.Field
import java.util.concurrent.SynchronousQueue
import java.time.Instant
import java.time.Duration

@Field static SynchronousQueue<Map> q = [:]

definition (
  name: 'TestBed3',
  namespace: 'WesMC',
  author: 'Wesley M. Conner',
  description: 'TestBed3',
  singleInstance: true,
  iconUrl: '',
  iconX2Url: ''
)

preferences {
  page(name: 'TestBed3')
}

Map TestBed3() {
  return dynamicPage(
    name: 'TestBed3',
    title: 'TestBed3',
    install: true,
    uninstall: true
  ) {
    app.updateLabel("TestBed3 (${app.id})")
    section{
      paragraph h1('ISO8601 Date Tests')
      Instant tOut1 = java.time.Instant.now()
      Instant tIn1 = Instant.parse('2024-07-05T17:37:50.371291Z')
      Long durationMs1 = Duration.between(tIn1, tOut1).toMillis();
      paragraph "in: ${tIn1}, out: ${tOut1}, duration: ${durationMs1} ms"
      paragraph "duration: ${durationMs1/1000} s"

      Instant tIn2 = java.time.Instant.now()
      pauseExecution(150)
      Instant tOut2 = java.time.Instant.now()
      Long durationMs2 = Duration.between(tIn2, tOut2).toMillis();
      paragraph "in: ${tIn2}, out: ${tOut2}, duration: ${durationMs2} ms"
      paragraph "duration: ${durationMs2/1000} s"

      paragraph h1('Click Done to begin Thread Test')
      paragraph i('Review test output in Hubitat logs.')
    }
  }
}

void producer(Map parms) {
  logInfo('producer', "parms: ${bMap(parms)}")
  ArrayList log = ['']
  // logInfo('producer',
  //   "${parms.producer} has range: ${parms.range} (${getObjectClassName(parms.range)})"
  // )
  ArrayList cmds = parms.range.collect { e ->
    [name: parms.name, value: "${e}", ref: "${java.time.Instant.now()}"]
  }
  //logInfo('producer', "cmds: ${cmds}")
  cmds.each{ command ->
    pauseExecution(parms.pause)
    q.put(command)
    log << command
  }
  logInfo('producer', log)
}

void producer1(Map parms) {
  parms << [producer: 'producer1']
  producer(parms)
}

void producer2(Map parms) {
  parms << [producer: 'producer2']
  producer(parms)
}

void producer3(Map parms) {
  parms << [producer: 'producer3']
  producer(parms)
}

void consumer(Map parms) {
  logInfo('consumer', "parms: ${bMap(parms)}")
  ArrayList log = ['']
  ArrayList range = 1..75  // Tactically, limit looping to 75
  range.each { e ->
    Map cmd = q.take()
    Instant tOut = java.time.Instant.now()
    Instant tIn = Instant.parse(cmd.ref)
    Long qDuration = Duration.between(tIn, tOut).toMillis();
    log << [ ageInMs: qDuration, *:cmd ]
  }
  logInfo('consumer', log)
}
void installed() {
  logInfo('installed', 'Creating queue ...')
  q = new SynchronousQueue<Map>()
  logInfo('installed', 'Queue created.')
  runInMillis(1000, 'consumer', [data: [ref: "Single Consumer"]])
  logInfo('installed', 'Consumer thread requested.')
  Map args1 = [ range: 1..30, name: 'alpha', pause: 150 ]
  Map args2 = [ range: 31..60, name: 'beta', pause: 200 ]
  Map args3 = [ range: 61..75, name: 'gamma', pause: 175 ]
  runInMillis(1000, 'producer1', [data: args1])
  runInMillis(1000, 'producer2', [data: args2])
  runInMillis(1000, 'producer3', [data: args3])
  logInfo('installed', 'Producer thread requested')
}

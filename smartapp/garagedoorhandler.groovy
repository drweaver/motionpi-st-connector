/**
 *
 *  SmartThings bridge for garage-control <https://github.com/drweaver/garage-control>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


preferences {
        input("ip", "string", title:"IP Address", description: "192.168.0.55", defaultValue: "192.168.0.55" ,required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", description: "3003", defaultValue: "3003" , required: true, displayDuringSetup: true)
        input("mac", "string", title:"Mac Address", description: "74867ADB74E6", defaultValue: "74867ADB74E6" , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "Garage Door", namespace: "shaneweaver", author: "Shane Weaver") {
		
        capability "Contact Sensor"
        capability "Motion Sensor"
        capability "Refresh"
        capability "Door Control"
        
        attribute "frontdoor_1", "string"
        attribute "frontdoor_2", "string"
        attribute "sidedoor", "string"
        
        attribute "connection", "string"
        
        command "frontdoor_1Operate"
	}

	tiles(scale: 2) {
        
        multiAttributeTile(name:"garageState", type:"generic", width:6, height:4) {
    		tileAttribute("device.contact", key: "PRIMARY_CONTROL") {
      			attributeState "open", label: '${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
      			attributeState "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
    		}
  		}

        standardTile("frontdoor_1State", "device.frontdoor_1", width: 2, height: 2) {
			state "closed", label:"Front 1", icon:"st.contact.contact.closed", backgroundColor:"#79b821", action: "frontdoor_1Operate"
			state "open", label:"Front 1", icon:"st.contact.contact.open", backgroundColor:"#ffa81e", action: "frontdoor_1Operate"
		}
        
        standardTile("frontdoor_2State", "device.frontdoor_2", width: 2, height: 2) {
			state "closed", label:"Front 2", icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			state "open", label:"Front 2", icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
		}
        
        standardTile("sidedoorState", "device.sidedoor", width: 2, height: 2) {
			state "closed", label:"Side", icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			state "open", label:"Side", icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
		}
        
        standardTile("motion", "device.motion", width: 2, height: 2) {
			state "inactive", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
			state "active", label:'${name}', icon:"st.motion.motion.active", backgroundColor:"#00A0DC"
		}
        
        standardTile("connectionState", "device.connection", width: 2, height: 2) {
        	state "online", label: "Online", icon:"st.Health & Wellness.health9",backgroundColor: "#ffffff"
            state "offline", label: "Offline", icon:"st.Health & Wellness.health9",backgroundColor: "#cccccc"
            state "unavailable", label: "Unavailable", icon:"st.Health & Wellness.health9",backgroundColor: "#cccccc"
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
          	state "default", action:"refresh", label: "Refresh", displayName: "Refresh", icon: "st.secondary.refresh-icon"
        }

        main "garageState"
        details(["garageState", "frontdoor_1State", "frontdoor_2State", "sidedoorState", "motion", "connectionState", "refresh"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
    log.debug "description: $description"
    
	try {
    	if( description?.endsWith("body:") ) {
        	log.debug "Empty body, ending parse"
            return;
        }
        def map = [:]
        def descMap = parseDescriptionAsMap(description)
        log.debug "descMap: ${descMap}"

        def body = new String(descMap["body"].decodeBase64())
        log.debug "body: ${body}"

        def slurper = new JsonSlurper()
        def result = slurper.parseText(body)

        log.debug "result: ${result}"

        def events = [];

        if( result?.motion != null && result.motion != device.currentValue("motion") )
            events.add( createEvent(name: "motion", value: result.motion ));
        if( result?.status != null && result.status != device.currentValue("status") )
            events.add( createEvent(name: "status", value: result.status ));
        if( result?.state != null ) {
            def anyOpen = false;
            result.state.each { doorId, state ->
                if( device.currentValue(doorId) != state )
                    events.add( createEvent(name: doorId, value: state) );
                // if( doorId == "frontdoor_1" && device.currentValue("door") != state )
                //     events.add( createEvent(name: "door", value: state ));
                if( state != "closed" )
                    anyOpen = true;
            }
            def contact = anyOpen ? "open" : "closed";
            if( contact != device.currentValue("contact") )
                events.add( createEvent(name: "contact", value: contact ) );
        }

        return events;

    } catch( Exception ex) {
        log.debug "exception in parse"
    }
 	
}

// handle commands
def frontdoor_1Operate() {
	operate("frontdoor_1");
}

def open() {
    if( device.currentValue("frontdoor_1") != "open" )
        frontdoor_1Operate();
}

def close() { 
    if( device.currentValue("frontdoor_1") != "closed" ) 
        frontdoor_1Operate();
}

def refresh() {
	log.debug "Executing 'refresh'"
    subscribeAction()
}

def operate(doorId){
	log.debug "Operate was pressed for ${doorId}"
    postAction("operate", doorId)
}

def installed() {
	log.debug "Executing 'installed'"
	updated();
}

def updated() {
	log.debug "Executing 'updated'"
    unschedule()
    runEvery1Hour(refresh)
    runIn(2, refresh)
    device.deviceNetworkId = mac;
}




// ------------------------------------------------------------------

private postAction(action, doorId){
  //setDeviceNetworkId(ip,port)  
  
  def hubAction = new physicalgraph.device.HubAction(
    method: "POST",
    headers: getHeader(),
    body: ["doorId": doorId,"action": action]
  )
  log.debug("Executing hubAction on " + getHostAddress())
  sendHubCommand(hubAction)
}

private subscribeAction(callbackPath="") {
  //setDeviceNetworkId(ip,port)
  log.debug("deviceNetworkId set to "+device.deviceNetworkId);
  
    def address = getCallBackAddress()
    def ip = getHostAddress()

    def result = new physicalgraph.device.HubAction(
        method: "SUBSCRIBE",
        headers: [
            HOST: ip,
            CALLBACK: "<http://${address}/notify$callbackPath>",
            NT: "upnp:event",
            TIMEOUT: "Second-28800"
        ]
    )
    log.debug("Executing hubAction for subscribe event on " + getHostAddress())
	sendHubCommand(result)
}


// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}


def toAscii(s){
        StringBuilder sb = new StringBuilder();
        String ascString = null;
        long asciiInt;
                for (int i = 0; i < s.length(); i++){
                    sb.append((int)s.charAt(i));
                    sb.append("|");
                    char c = s.charAt(i);
                }
                ascString = sb.toString();
                asciiInt = Long.parseLong(ascString);
                return asciiInt;
    }

private encodeCredentials(username, password){
	log.debug "Encoding credentials"
	def userpassascii = "${username}:${password}"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    //log.debug "ASCII credentials are ${userpassascii}"
    //log.debug "Credentials are ${userpass}"
    return userpass
}

private getHeader(){
	log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    // headers.put("Authorization", userpass)
    log.debug "Headers are ${headers}"
    return headers
}

private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

private setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip).toUpperCase()
  	def porthex = convertPortToHex(port).toUpperCase()
  	device.deviceNetworkId = "$iphex:$porthex".toUpperCase()
  	log.debug "Device Network Id set to ${device.deviceNetworkId}"
}

private getHostAddress() {
	return "${ip}:${port}"
}

// gets the address of the Hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}

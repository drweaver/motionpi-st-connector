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
        input("doorId", "string", title:"Door Id", description: "Door Id", defaultValue: "door", required:true, displayDuringSetup: true)
        input("auth", "string", title:"Auth Code", description: "auth code", defaultValue: "abc123" , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "Garage Door", namespace: "shaneweaver", author: "Shane Weaver") {
		capability "Refresh"
        capability "Button"
        capability "Contact Sensor"
        
        attribute "frontdoor_1", "string"
        attribute "frontdoor_2", "string"
        attribute "sidedoor", "string"
        
        command "operate"
	}

	simulator {
		// TODO
	}

	tiles {
        standardTile("garageState", "device.contact", width: 3, height: 2) {
			state "closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			state "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
		}

        standardTile("frontdoor_1State", "device.frontdoor_1", width: 1, height: 1) {
			state "closed", label:"Front 1", icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			state "open", label:"Front 1", icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
		}
        
        standardTile("frontdoor_2State", "device.frontdoor_2", width: 1, height: 1) {
			state "closed", label:"Front 2", icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			state "open", label:"Front 2", icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
		}
        
        standardTile("sidedoorState", "device.sidedoor", width: 1, height: 1) {
			state "closed", label:"Side", icon:"st.contact.contact.closed", backgroundColor:"#79b821"
			state "open", label:"Side", icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
		}
        
        // standardTile("operate", "device.button", inactiveLabel: false, decoration: "flat") {
        //	state "default", action:"operate", label: "Operate", displayName: "Operate"
        // }

        // standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
        //   	state "default", action:"refresh", label: "Refresh", displayName: "Refresh"
        //  }

        main "garageState"
        details(["garageState", "frontdoor_1State", "frontdoor_2State", "sidedoorState"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
    log.debug "description: $description"
    
	try {
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    log.debug "descMap: ${descMap}"
    
    def body = new String(descMap["body"].decodeBase64())
    log.debug "body: ${body}"
    
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    
    log.debug "result: ${result}"

    if( result.containsKey("status") ) {
        log.debug "creating event for doorId: ${result.doorId} status: ${result.status}"
        def evt = createEvent(name: result.doorId, value: result.status)
        log.debug "creating event for garage status: ${result.garage}"
        def garage = createEvent(name: "contact", value: result.garage)
        return [evt,garage];
    }
    } catch( Exception ex) {
      log.debug "exception in parse"
    }
 	
}

// handle commands
def poll() {
	log.debug "Executing 'poll'"
    subscribeAction()
}

def refresh() {
	// sendEvent(name: "switch", value: "off")
	log.debug "Executing 'refresh'"
    poll()
}

def operate(){
	log.debug "Operate was pressed"
    //postAction("operate")
    subscribeAction()
}

// Get door status
def getStatus() {
    postAction("status")
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

private postAction(action){
  //setDeviceNetworkId(ip,port)  
  
  def hubAction = new physicalgraph.device.HubAction(
    method: "POST",
    // path: uri,
    headers: getHeader(),
    body: ["auth": auth,"doorId": doorId,"action": action]
  )//,delayAction(1000), refresh()]
  log.debug("Executing hubAction on " + getHostAddress())
  //log.debug hubAction
  sendHubCommand(hubAction)
  //hubAction    
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
/**
 *
 *  SmartThings bridge for motion pi 
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
	definition (name: "Pi Motion Sensor", namespace: "shaneweaver", author: "Shane Weaver") {
		
        capability "Motion Sensor"
        }
	tiles(scale: 2) {
              
        standardTile("motion", "device.motion", width: 2, height: 2) {
			state "inactive", label:'${name}', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
			state "active", label:'${name}', icon:"st.motion.motion.active", backgroundColor:"#00A0DC"
		}
        
        main "motion"
        details("motion")
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

        if( result.containsKey("motion") ) {
            log.debug "creating event for status: ${result.motion}"
            //Expect motion sensor to be providing an update every 5 seconds if motion is active
            // if we don't get the inactive message, at least fail graciously 
            if( result.motion == "active") runIn( 31, setMotionInactive );
            return createEvent(name: "motion", value: result.motion);
        }


        return events;

    } catch( Exception ex) {
        log.debug "exception in parse"
    }
 	
}

// handle commands

def refresh() {
	log.debug "Executing 'refresh'"
    subscribeAction()
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

def setMotionInactive() {
	log.debug "Executing 'setMotionInactive'"
    sendEvent(name: "motion", value: "inactive");
}


// ------------------------------------------------------------------

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

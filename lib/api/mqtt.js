'user strict'

const mqtt = require('mqtt');
const log = require('../local/log');

const topicBase = "home/garage/";
const url = process.env.MQTT_URL;
const opts = { username: process.env.MQTT_USERNAME, password: process.env.MQTT_PASSWORD };

// Create a client connection
var client = mqtt.connect(url, opts);

var status = {};

var callback = null;

client.on('connect', function() { // When connected
    log.info('MQTT: Successfully connected');
    // subscribe to topic
    client.subscribe(topicBase+'+');
});

client.on('close', function() {
   log.warn('MQTT: Connection closed');
});

// when a message arrives, do something with it
client.on('message', function(topic, message, packet) {

    // if( packet.retain ) {
    //     log.info('Ignoring stale message: '+topic+' '+message);
    //     return;
    // }

    log.info("MQTT message: " + topic + " " + message);
    var deviceName = topic.split(/\//)[2];

    if( deviceName == 'operate' || deviceName == 'status' ) return;

    //cache the data;
    status[deviceName] = message.toString();

    if( callback !== null ) {
        callback(deviceName, status);
    }

});

module.exports = {
    getStatus: function(deviceName) {
        return status[deviceName] || 'unknown';
    },
    operate: function(deviceName) {
        client.publish(topicBase+'operate', deviceName);
    },
    setCallback: function(c) {
        callback = c;
        return;
    },
    getStatusAll: function() {
        return status;
    }
};
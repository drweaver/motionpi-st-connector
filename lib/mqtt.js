'user strict'

const mqtt = require('mqtt');
const log = require('./log');

const motionDevice = process.env.MOTION_DEVICE;
const topicBase = "home/" + motionDevice + "/motion";
const url = process.env.MQTT_URL;
const opts = { username: process.env.MQTT_USERNAME, password: process.env.MQTT_PASSWORD };

// Create a client connection
var client = mqtt.connect(url, opts);

var data = { motion: "inactive" };

var callback = null;

client.on('connect', function() { // When connected
    log.info('MQTT: Successfully connected');
    // subscribe to topic
    client.subscribe(topicBase);
});

client.on('close', function() {
   log.warn('MQTT: Connection closed');
});

// when a message arrives, do something with it
client.on('message', function(topic, message, packet) {

    log.info("MQTT message: " + topic + " " + message);
    var path = topic.split(/\//);
    var device = path[1];

    if( device != motionDevice )
      return;

    data.motion = message.toString();

    if( callback !== null ) {
        log.info("Invoking callback, motion = "+data.motion);
        callback(data);
    }

});

module.exports = {
    setCallback: function(c) {
        callback = c;
    },
    getData: ()=>{
        return data;
    }
};

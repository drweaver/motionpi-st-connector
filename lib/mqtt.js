'user strict'

const mqtt = require('mqtt');
const log = require('./log');

const topicBase = "home/garage/";
const url = process.env.MQTT_URL;
const opts = { username: process.env.MQTT_USERNAME, password: process.env.MQTT_PASSWORD };

// Create a client connection
var client = mqtt.connect(url, opts);

var data = { motion: "inactive", state: {}, status: "unknown" };

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

    log.info("MQTT message: " + topic + " " + message);
    var path = topic.split(/\//)[2];

    switch (path) {
        case 'operate':
            return;
        case 'status':
            data.status = message == 'OK' ? 'online' : 'offline' ;
            break;
        case 'motion':
            data.motion = message.toString();
            break;
        default: // assume doorId
            data.state[path] = message.toString();
            break;
    }

    if( callback !== null ) {
        callback(data);
    }

});

module.exports = {
    operate: function(deviceName) {
        client.publish(topicBase+'operate', deviceName);
    },
    setCallback: function(c) {
        callback = c;
    },
    getData: ()=>{
        return data;
    }
};

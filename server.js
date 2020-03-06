'use strict';

const express = require('express');
const bodyParser = require('body-parser');
const mqtt = require('./lib/mqtt');
const request = require('request');
const log = require('./lib/log');
const _ = require('underscore');

const app = module.exports = express();
app.use(bodyParser.json());

app.subscribe('/', function(req, response) {
    log.info('SUBSCRIBE called');
    log.info("HOST: " + req.get("HOST"));
    log.info("CALLBACK: " + req.get("CALLBACK"));
    let uri = req.get("CALLBACK");
    uri = uri.substring(1,uri.length-1);
    log.info("URI = " + uri);
    response.status(202).send();
    mqtt.setCallback( (d)=>{subscribeCallback(uri,d)} );
    //send updated status for all devices
    subscribeCallback(uri,mqtt.getData());
});

function subscribeCallback(uri, data) {
    log.info("Sending callback to " + uri);
    request.post({ uri: uri, json: true, body: data}, function (error, response, body) {
        if( error ) log.info('error:', error); 
        log.info('statusCode:', response && response.statusCode); 
        log.info('body:', body); 
      });
}

app.listen(5003);
log.info('Open: http://127.0.0.1:5003');

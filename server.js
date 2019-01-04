'use strict';

const express = require('express');
const bodyParser = require('body-parser');
const mqtt = require('./lib/api/mqtt');
const request = require('request');
const log = require('./lib/local/log');

const app = module.exports = express();
app.use(bodyParser.json());
app.post('/', function(req, response) {
    let evt = req.body;
    switch (evt.action) {

        case 'status': {
            log.trace(`REQUEST: ${JSON.stringify(evt, null, 2)}`);
            log.response(response, {status: mqtt.getStatus(evt.doorId), doorId: evt.doorId});
            break;
        }

        case 'operate': {
            log.trace(`REQUEST: ${JSON.stringify(evt, null, 2)}`);
            mqtt.operate(evt.doorId);
            log.response(response, {status: mqtt.getStatus(evt.doorId), doorId: evt.doorId});
            break;
        }

        default: {
            console.log(`Action ${evt.action} not supported`);
        }
    }
});

app.subscribe('/', function(req, response) {
    log.info('SUBSCRIBE called');

    //TODO: Check Auth

    log.info("HOST: " + req.get("HOST"));
    log.info("CALLBACK: " + req.get("CALLBACK"));
    let uri = req.get("CALLBACK");
    uri = uri.substring(1,uri.length-1);
    log.info("URI = " + uri);
    response.status(202).send();
    mqtt.setCallback( function(device, statusAll) {
        let garage = "closed";
        Object.values(statusAll).forEach(value => {
            if( value !== "closed" ) {
                garage = "open";
            }
          });
        let body = {doorId: device, status: statusAll[device], garage: garage };
        log.info("Sending callback for device " + device + " to " + uri);
        request.post({ uri: uri, json: true, body: body}, function (error, response, body) {
            if( error ) log.info('error:', error); // Print the error if one occurred
            log.info('statusCode:', response && response.statusCode); // Print the response status code if a response was received
          });
    });
    //send updated status for all devices
    let garage = "closed";
    var statusAll = mqtt.getStatusAll();
    Object.values(statusAll).forEach(value => {
        if( value !== "closed" ) {
            garage = "open";
        }
      });
      Object.entries(statusAll).forEach(entry => {
        let key = entry[0];
        let value = entry[1];
        let body = {doorId: key, status: value, garage: garage };
        log.info("Sending callback for device " + key + " to " + uri);
        request.post({ uri: uri, json: true, body: body}, function (error, response, body) {
            if(error) log.info('error:', error); // Print the error if one occurred
            log.info('statusCode:', response && response.statusCode); // Print the response status code if a response was received
            log.info('body:', body); // Print the HTML for the Google homepage.
          });
      });
});

app.listen(3003);
log.info('Open: http://127.0.0.1:3003');

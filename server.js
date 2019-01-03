'use strict';

const express = require('express');
const bodyParser = require('body-parser');
const mqtt = require('./lib/api/mqtt');
const util = require('util')
const request = require('request');

//const db = require('./lib/local/db');
const log = require('./lib/local/log');

const AUTH = '12345';

const app = module.exports = express();
app.use(bodyParser.json());
app.post('/', function(req, response) {
    callbackHandler(req, response)
});

app.subscribe('/', function(req, response) {
    log.info('SUBSCRIBE called');
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
            log.info('error:', error); // Print the error if one occurred
            log.info('statusCode:', response && response.statusCode); // Print the response status code if a response was received
            log.info('body:', body); // Print the HTML for the Google homepage.
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
            log.info('error:', error); // Print the error if one occurred
            log.info('statusCode:', response && response.statusCode); // Print the response status code if a response was received
            log.info('body:', body); // Print the HTML for the Google homepage.
          });
      });
});

function callbackHandler(req, response) {
    console.log(util.inspect(req.body, {showHidden: false, depth: null}))
    log.debug(req.body);
    if (req.body && req.body.auth === AUTH ) {
        handleCallback(req, response);
    } else {
        log.error("Unauthorized");
        response.status(401).send("Forbidden");
    }
}

function handleCallback(req, response) {
    let evt = req.body;
    switch (evt.action) {

        // PING happens during app creation. Respond with challenge to verify app
        case 'status': {
            log.trace(`REQUEST: ${JSON.stringify(evt, null, 2)}`);
            log.response(response, {status: mqtt.getStatus(evt.doorId), doorId: evt.doorId});
            break;
        }

        // CONFIGURATION is once with INITIALIZE and then for each PAGE
        case 'operate': {
            log.trace(`REQUEST: ${JSON.stringify(evt, null, 2)}`);
            mqtt.operate(evt.doorId);
            log.response(response, {status: mqtt.getStatus(evt.doorId), doorId: evt.doorId});
            break;
        }

        default: {
            console.log(`Lifecycle ${evt.lifecycle} not supported`);
        }
    }
}

app.listen(3003);
log.info('Open: http://127.0.0.1:3003');

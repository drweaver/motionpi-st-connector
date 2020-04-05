from gpiozero import MotionSensor
import time
import os

device = os.environ['MOTION_DEVICE']
print("MOTION_DEVICE = " + device)
topic = "home/"+device+"/motion"

last_motion = None

import paho.mqtt.client as mqtt

def on_connect(client, userdata, flags, rc):
  print("rc: " + str(rc))

def on_publish(client, obj, mid):
  print("mid: " + str(mid))

mqttc = mqtt.Client()
# Assign event callbacks
#mqttc.on_message = on_message
mqttc.on_connect = on_connect
mqttc.on_publish = on_publish
#mqttc.on_subscribe = on_subscribe

mqttc.will_set(topic, payload="inactive", qos=1)

mqttc.connect("mqtt")
mqttc.loop_start()


def when_motion():
  global last_motion
  if last_motion == None: mqttc.publish(topic, payload="active", qos=1)
  last_motion = time.time()
  print("last_motion = " + str(last_motion))

def when_no_motion():
  mqttc.publish(topic, payload="inactive", qos=1)

pir = MotionSensor(4)
pir.when_motion = when_motion
#pir.when_no_motion = when_no_motion

when_no_motion()

while True:
  if last_motion != None and time.time() - last_motion > 30:
    when_no_motion()
    last_motion = None;
  time.sleep(1)

#while True:
#  pir.wait_for_motion()
#  localtime = time.localtime()
#  result = time.strftime("%I:%M:%S %p", localtime)
#  print("Motion Detected at " + result)
#  mqttc.publish("home/kitchen/motion", str(int(time.time())))
#  time.sleep(1)

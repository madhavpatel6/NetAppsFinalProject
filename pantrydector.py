import time
import json
import RPi.GPIO as GPIO
from bluetooth import *
import time


def btoothSend():
    msg='{"msg": "open"}'
    sock=BluetoothSocket(RFCOMM)
    port=2
    try:
        sock.connect(('B8:27:EB:F5:49:CC', port))

        sock.send(msg)
        sock.close()
    except btcommon.BluetoothError as error:
        time.sleep(1)
        btoothSend()
    
def pDector():
        GPIO.setwarnings(False)
	GPIO.setmode(GPIO.BCM)
	
	GPIO.setup(4, GPIO.OUT)
	GPIO.setup(17, GPIO.OUT)
	GPIO.setup(27, GPIO.OUT)
	
	GPIO.setup(12, GPIO.IN, pull_up_down=GPIO.PUD_UP)
	
	GPIO.output(4, GPIO.LOW)
	GPIO.output(17, GPIO.LOW)
	GPIO.output(27, GPIO.LOW)

	
	trigger = True
	
	while 1:
		if GPIO.input(12):
			GPIO.output(4, GPIO.HIGH)
			GPIO.output(17, GPIO.LOW)
			if trigger:
                                btoothSend()
                                trigger = False
                        
		else:
                        GPIO.output(4, GPIO.LOW)
                        GPIO.output(17, GPIO.HIGH)
                        trigger = True
			
        
pDector()

    

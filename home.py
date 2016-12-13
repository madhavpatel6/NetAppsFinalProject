from databasehelper import DatabaseHelper
import pika
import pickle
import json
import requests
from bluetooth import *


class MobileHelper:
    def __init__(self):
        self.db = DatabaseHelper()
        self.recipes_end_point = 'https://spoonacular-recipe-food-nutrition-v1.p.mashape.com/recipes/findByIngredients'
        self.analyzed_recipe_end_point = 'https://spoonacular-recipe-food-nutrition-v1.p.mashape.com/recipes/'

    def on_request(self, ch, method, props, body):
        response = []  # initialize response passed back to the bridge

        print('Message Received', body.decode('utf-8'))
        message = json.loads(body.decode('utf-8'))
        if 'type' not in message:
            response.append({'status':'BAD', 'error':'Invalid syntax. Message must contain a type and data key.'})
        elif message['type'] == 'recipes':
            response = self.get_recipes(5, False)
        elif message['type'] == 'remove':
            if 'data' in message:
                for item in message['data']:
                    self.db.remove_item(str(item['name']), int(item['quantity']))
                response = self.db.get_all_items()
            # call bluetooth function
                self.bluetoothSend(response)
            return
        elif message['type'] == 'add':
            if 'data' in message:
                for item in message['data']:
                    self.db.add_item(str(item['name']), int(item['quantity']))
                response = self.db.get_all_items()
        elif message['type'] == 'price':
            response = self.get_price(str(message['data']))
        elif message['type'] == 'pantry':
            response = self.db.get_all_items()

        ch.basic_publish(exchange='',
                         routing_key=str(props.reply_to),
                         properties=pika.BasicProperties(correlation_id=props.correlation_id),
                         body=pickle.dumps(response))

    def get_recipes(self, number, fill_ingredients):
        pantry = self.db.get_all_items()
        pantry_str = ''
        for item in pantry:
            pantry_str += str(item['name']) + ','
        print(pantry_str)
        payload = {'fillIngredients': bool(fill_ingredients), 'ingredients': pantry_str,
                   'limitLicense': False, 'number': int(number), 'ranking': 2}
        response = requests.get(self.recipes_end_point,
                                headers={"X-Mashape-Key": "DpzHfS6foEmshErpzeQcagXsunIip1DIy2Gjsn65PREQIIVopo",
                                         "Accept": "application/json"}, params=payload)
        print(response.url)
        print(json.dumps(response.json(), indent=4, sort_keys=True))
        return response

    def get_recipe(self, id):
        url = self.analyzed_recipe_end_point + str(id) + '/analyzedInstructions'
        payload = {'stepBreakdown':True}
        response = requests.get(url,
                                headers={"X-Mashape-Key": "DpzHfS6foEmshErpzeQcagXsunIip1DIy2Gjsn65PREQIIVopo",
                                         "Accept": "application/json"}, params=payload)
        print(response.url)
        print(json.dumps(response.json(), indent=4, sort_keys=True))

    def get_price(self, name):
        response = requests.get('http://api.walmartlabs.com/v1/search?apiKey=bsgcpte3pz8wxqaspmnjrs5n&query={' + str(
            name) + '}&format=json')
        print(json.dumps(response.json()['items'], indent=4, sort_keys=True))
        return {'price': response.json()['items'][0]['salePrice']}

    def bluetoothSend(self, items):
        sock = BluetoothSocket(RFCOMM)
        port = 1
        sock.connect(('B8:27:EB:F5:49:CC', port))
        for item in items:
            sock.send(json.dumpbs(item))
        sock.send('{"end": 0}')
        sock.close()

'''def main():
    mh = MobileHelper()
    mh.db.remove_all_items()
    mh.db.add_item('onions', 5)
    mh.db.add_item('potatoes', 7)
    mh.db.add_item('bell peppers', 7)
    mh.db.remove_item('potatoes', 4)
    print(mh.db.get_all_items())
    mh.get_recipes(5, False)
    #mh.get_recipe(574687)'''


def main():
    mh = MobileHelper()
    mh.db.remove_all_items()
    mh.db.add_item('onions', 5)
    mh.db.add_item('potatoes', 7)
    mh.db.add_item('bell peppers', 7)
    mh.db.remove_item('potatoes', 4)
    connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
    # Establish a channel
    channel = connection.channel()
    # Delete the local queue to clear it
    channel.queue_delete(queue='home_queue')
    # Declare the local
    channel.queue_declare(queue='home_queue')
    # Setup a consume for the local queue
    channel.basic_consume(mh.on_request, queue='home_queue')

    print(" [x] Waiting for message")
    # Start consuming
    channel.start_consuming()


if __name__ == "__main__":
    main()
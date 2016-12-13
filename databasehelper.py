from pymongo import MongoClient
import pymongo
import sys


class DatabaseHelper:
    def __init__(self):
        self.client = MongoClient()
        self.db = self.client.database
        self.collection = self.db.pantry_collection

    def add_to_database(self, msg):
        try:
            print("Attempting to store", msg, "into pantry database.")
            id = self.collection.insert_one(msg).inserted_id
            print("Successfully inserted message into Repository database ", id)
        except pymongo.errors.ServerSelectionTimeoutError:
            sys.exit('Unable to add message to database')

    def delete_from_database(self, msg):
        print("Attempting to delete", msg, "from pantry database.")
        result = self.collection.delete_many(msg)
        print("Successfully deleted ", result.deleted_count)

    def get_from_database(self, msg):
        ret = list(self.collection.find(msg))
        if len(ret) == 0:
            return None
        return ret[0]

    def print_all_posts(self):
        print("Printing all posts")
        for post in self.collection.find():
            print(post)
        print("------------------")

    def get_all_items(self):
        ret = []
        for post in self.collection.find():
            del post['_id']
            ret.append(post)
        return ret

    def add_item(self, name, value):
        print("Adding " + str(value) + " '" + name + "' to pantry")
        old = self.get_from_database({'name': str(name)})
        if old is None:
            print("Adding item to database")
            self.add_to_database({'name': str(name), 'quantity': int(value)})
        else:
            self.collection.update_one({'_id': old['_id']}, {'$inc': {'quantity': value}})

    def remove_all_items(self):
        print("Removing all items from pantry")
        self.collection.delete_many({})

    def remove_item(self, name, value):
        print("Removing " + str(value) + " '" + name + "' from pantry")
        old = self.get_from_database({'name': str(name)})
        if old is None:
            print("No entry exist for '" + name + "'")
        elif old['quantity'] - value <= 0:
            print("Removing last quantity from pantry")
            self.delete_from_database({'name': str(name)})
        else:
            self.collection.update_one({'_id': old['_id']}, {'$inc': {'quantity': -value}})
            print("Successfully decremented the quantity of '" + name + "'")

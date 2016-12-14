import sys
from PyQt5.QtCore import *
from PyQt5.QtGui import *
from PyQt5.QtWidgets import *
from bluetooth import *
import json
import pika
import pickle

jsonData = [{"name":"Test Apples", "quantity":100}]

# Bridge Client Class
class BridgeClient(object):
    # Initialize a RabbitMQ connection to the repository
    def __init__(self):
        crds = pika.PlainCredentials('guest', 'guest')
        # Create a connection to the repository
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host='172.30.121.246', credentials=crds))
        # Create a channel with the connection
        self.channel = self.connection.channel()
        # Declare the bridge queue
        self.channel.queue_declare('home_queue')

    # Close the connection to the repository
    def __del__(self):
        self.connection.close()

    # Calls into the repository by putting a message into the repository_queue
    def call(self, n):
        # Try to send the message to the repository via RabbitMQ      
        self.channel.basic_publish(exchange='', routing_key='home_queue', body=str(n))
		
		

class HomePiThread(QThread):
    updateSignal = pyqtSignal(list)

    def __init__(self):
        super().__init__()
        
    def run(self):        
        while True:
            # Establish a connection
            server_sock = BluetoothSocket(RFCOMM)
            server_sock.bind(("", 1))
            
            # Wait for a connection
            server_sock.listen(1)
            client_sock, address = server_sock.accept()

            pantry = []
            # Receive from bluetooth
            while True:
                # Receive from the bridge
                data = client_sock.recv(1024)
                temp = json.loads(data.decode("utf-8"))
                #Process data
                if 'end' in temp:
                    break
                pantry.append(temp)
                
            self.updateSignal.emit(pantry)
                
            # Close the connection
            client_sock.close()
            server_sock.close()
        
        self.terminate()
  
  
class DetectorPiThread(QThread):
    removeSignal = pyqtSignal(dict)

    def __init__(self):
        super().__init__()
        
    def run(self):
        while True:
            # Establish a SOCKET
            server_sock = BluetoothSocket(RFCOMM)
            server_sock.bind(("", 2))
            
            # Wait for a connection
            server_sock.listen(2)
            client_sock, address = server_sock.accept()

            # Receive from the bridge
            data = client_sock.recv(1024)
            #Process data
            self.removeSignal.emit(json.loads(data.decode("utf-8")))
                
            # Close the connection
            client_sock.close()
            server_sock.close()
        
        self.terminate()

class ViewItem (QWidget):
    def __init__ (self, parent = None):
        super(ViewItem, self).__init__(parent)
        self.BoxLayout = QHBoxLayout()
        self.item    = QLabel()
        self.amount  = QLabel()
        self.BoxLayout.addWidget(self.item)
        self.BoxLayout.addWidget(self.amount)
        self.setLayout(self.BoxLayout)

    def setItem (self, text):
        self.item.setText(text)

    def setAmount (self, text):
        self.amount.setText(text)
        
class EditItem (QWidget):
    def __init__ (self, parent = None):
        super(EditItem, self).__init__(parent)
        self.BoxLayout = QHBoxLayout()
        self.item    = QLabel()
        self.amount  = QSpinBox()
        self.BoxLayout.addWidget(self.item)
        self.BoxLayout.addWidget(self.amount)
        self.setLayout(self.BoxLayout)

    def setItem (self, text):
        self.item.setText(text)

    def getItem (self):
        return self.item.text()

    def getAmount (self):
        return self.amount.value()

class MainPrgm (QMainWindow):
    def __init__ (self):
        super(MainPrgm, self).__init__()
        
        # Start Threads and connect signals
        self.homeThread = HomePiThread()
        self.homeThread.updateSignal.connect(self.updateToPantry)
        self.homeThread.start()
        
        self.detectThread = DetectorPiThread()
        self.detectThread.removeSignal.connect(self.removeFromPantry)
        self.detectThread.start()
        
        # Create QListWidget
        self.list = QListWidget(self)
        for index in jsonData:
            # Create ViewItem
            newItem = ViewItem()
            newItem.setItem(index["name"])
            newItem.setAmount(str(index["quantity"]))
            # Create QListWidgetItem
            listItem = QListWidgetItem(self.list)
            # Set size hint
            listItem.setSizeHint(newItem.sizeHint())
            # Add QListWidgetItem into QListWidget
            self.list.addItem(listItem)
            self.list.setItemWidget(listItem, newItem)
        
        
        self.MainLayout = QVBoxLayout()
        self.title = QLabel('Digital Pantry Monitor')
        self.MainLayout.addWidget(self.title)
        
        self.TagLayout = QHBoxLayout()
        self.name = QLabel('Name')
        self.amnt = QLabel('Amount')
        self.TagLayout.addWidget(self.name)
        self.TagLayout.addWidget(self.amnt)
        self.MainLayout.addLayout(self.TagLayout)
        
        self.scroll = QScrollArea()
        self.scroll.setWidget(self.list)
        self.MainLayout.addWidget(self.scroll)
        
        self.center = QWidget()
        self.center.setLayout(self.MainLayout)
        self.setCentralWidget(self.center)
        self.setWindowTitle('Digital Pantry');

    @pyqtSlot(list)    
    def updateToPantry(self, items):
        global jsonData
        print(items)
        jsonData = items

        self.list.clear()
        for index in jsonData:
            # Create ViewItem
            newItem = ViewItem()
            newItem.setItem(index["name"])
            newItem.setAmount(str(index["quantity"]))
            # Create QListWidgetItem
            listItem = QListWidgetItem(self.list)
            # Set size hint
            listItem.setSizeHint(newItem.sizeHint())
            # Add QListWidgetItem into QListWidget
            self.list.addItem(listItem)
            self.list.setItemWidget(listItem, newItem)

    @pyqtSlot(dict)
    def removeFromPantry(self, msg):
        global jsonData
        print(msg)
        dialog.updateDialog()
        dialog.show()
        
        
class RemoveDialog (QDialog):
    def __init__ (self):
        super(RemoveDialog, self).__init__()
        # Create QListWidget
        self.list = QListWidget(self)
        for index in jsonData:
            # Create ViewItem
            newItem = EditItem()
            newItem.setItem(index["name"])
            # Create QListWidgetItem
            listItem = QListWidgetItem(self.list)
            # Set size hint
            listItem.setSizeHint(newItem.sizeHint())
            # Add QListWidgetItem into QListWidget
            self.list.addItem(listItem)
            self.list.setItemWidget(listItem, newItem)
        
        
        self.MainLayout = QVBoxLayout()
        self.title = QLabel('What have you removed?')
        self.MainLayout.addWidget(self.title)
        
        self.TagLayout = QHBoxLayout()
        self.name = QLabel('Name')
        self.amnt = QLabel('Amount')
        self.TagLayout.addWidget(self.name)
        self.TagLayout.addWidget(self.amnt)
        self.MainLayout.addLayout(self.TagLayout)
        
        self.scroll = QScrollArea()
        self.scroll.setWidget(self.list)
        self.MainLayout.addWidget(self.scroll)
        
        self.ButtonLayout = QHBoxLayout()
        self.submit = QPushButton('Submit')
        self.ButtonLayout.addWidget(self.submit)
        self.ButtonLayout.setAlignment(Qt.AlignRight)
        self.MainLayout.addLayout(self.ButtonLayout)
        
        self.submit.clicked.connect(self.submitChanges)
        
        self.setLayout(self.MainLayout)
        self.setWindowTitle('Digital Pantry');

    def updateDialog(self):
        global jsonData
        # Create QListWidget
        self.list.clear()
        for index in jsonData:
            # Create ViewItem
            newItem = EditItem()
            newItem.setItem(index["name"])
            # Create QListWidgetItem
            listItem = QListWidgetItem(self.list)
            # Set size hint
            listItem.setSizeHint(newItem.sizeHint())
            # Add QListWidgetItem into QListWidget
            self.list.addItem(listItem)
            self.list.setItemWidget(listItem, newItem)
        
        
    def submitChanges(self):
        # Send changes through rabbitMQ
        response = []
        for i in range(0, self.list.count()):
            item = self.list.item(i)
            response.append({'name':self.list.itemWidget(item).getItem(), 'quantity':self.list.itemWidget(item).getAmount()})

        rabmq = {'type':'remove', 'data':response}
        bridge.call(json.dumps(rabmq))
        print(rabmq)
        self.close()
        

app = QApplication([])
bridge = BridgeClient()
dialog = RemoveDialog()
window = MainPrgm()
window.show()
sys.exit(app.exec_())

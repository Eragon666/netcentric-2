from threading import Thread
import Tkinter as tk
from Tkinter import *
from bluetooth import *
import tkMessageBox
import random
from time import *

canvas_width = 600
canvas_height = 600

x = 3
y = 3

#Listen to bluetooth connection boolean
listen = 1;

#To store the multiple threads in
threads = []

global root
root=tk.Tk()
root.resizable(width=FALSE, height=FALSE)

global quit

global server_sock
server_sock=BluetoothSocket(RFCOMM)
server_sock.bind(("",PORT_ANY))
server_sock.listen(1)

global client_sock

port = server_sock.getsockname()[1]

uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

advertise_service( server_sock, "SampleServer",
                    service_id = uuid,
                    service_classes = [ uuid, SERIAL_PORT_CLASS ],
                    profiles = [ SERIAL_PORT_PROFILE ],
#                   protocols = [ OBEX_UUID ] 
                    )



def listenBluetooth():
    currentLocation = "[0, 0]"
    direction = "None"
    confirmation = "False"

    ReservedLocations = ["[0, 1]", "[0, 2]", "[0, 3]", "[4, 1]", "[4, 2]", "[4, 3]", "[1, 0]", "[2, 0]", "[3, 0]", "[1, 4]", "[2, 4]", "[3, 4]"]

    def Confirm(currentLocation, direction):
        # Also needs a global array with all the reserved locations.
        # Currently named: 'ReservedLocations' */
        # Initialize 'newPosition' based on parameters.
        newPostion = ""
        test = currentLocation.replace("Locatie: ", "")
        currentX = int(test[1])
        currentY = int(test[4])
        if direction == "North":
            newPosition = "[" + str(currentX) + ", " + str(currentY + 1) + "]"
        elif direction == "East":
            newPosition = "[" + str(currentX + 1) + ", " + str(currentY) + "]"
        elif direction == "South":
            newPosition = "[" + str(currentX) + ", " + str(currentY - 1) + "]"
        elif direction == "West":
            newPosition = "[" + str(currentX - 1) + ", " + str(currentY) + "]"
            
        if newPosition in ReservedLocations:
            return "False"
        
        return "True"
    
    global root
    print "Waiting for connection"
    global quit
    global client_sock, client_info
    client_sock, client_info = server_sock.accept()
    #thread1 = Thread(target = guiMain, args=client_sock)
    #thread1.start()
    #threads.append(thread1)    
    print("Accepted connection from ", client_info)
    while True:
        print quit
        if (quit):
            client_sock.close()
            break
        elif (listen):
            try:
                data = client_sock.recv(1024)
                if len(data) == 0:
                    pass
                else:
                    if "QRdata: " in data:
                        QRdata = data.replace("QRdata: ", "")
                        currentLocation = QRdata.replace("Locatie: ", "").replace("; Robot-ID: 21;", "")
                        client_sock.send("currentLocation: " + currentLocation)
                    elif "direction: " in data:
                        direction = data.replace("direction: ", "")
                        confirmation = Confirm(currentLocation, direction)
                        if confirmation =="True":
                            client_sock.send("confirmation: " + confirmation)
                        else:
                            client_sock.send("currentLocation: " + currentLocation)
                    gui = Gui(root)
                    gui.parser(data)                
                print("received [%s]" % data)
            except IOError:
                pass

def guiMain():
    global root
    gui=Gui(root)
    root.protocol("WM_DELETE_WINDOW", gui.handler)
    root.mainloop()
    print("disconnected");

def exitGui():
    #global client_sock
    #client_sock.close()
    server_sock.close()
    #for thread in threads:
        #thread.join()

def connect():
    print "Waiting for connection"
    global client_sock, client_info
    client_sock, client_info = server_sock.accept()
    print("Accepted connection from ", client_info)
    listen = 1;     

def draw(self,x,y,size):
    	drawQR(self,x,y,size)

def drawQR(self,x,y,size):
		dsize = size/4
		nextx = x
		nexty = y
		for i in range(4):
			nextx = nextx+dsize
			for j in range(4):
				if(random.randint(0,1)==1):
					color ="black"
				else:
					color="white"
				nexty = nexty+dsize
				figure=self.canvas.create_rectangle(nextx,nexty,nextx+dsize,nexty+dsize, fill=color)
			nexty = y

def drawRobot(self,x,y,size,direction,xco,yco):
	x *= xco
	y *= yco
	x += size/8
	y+= size/8
	figure=self.canvas.create_rectangle(x,y,x+size/4,y+size/4, fill="yellow")

#def parser(self, data):
    #for s in data:
        #if s
     
    #self.canvas.update_idletasks()
    #drawRobot(self,20,20,300,1,1,1)

class Gui():
    def __init__(self, root):
        self.root=root
        self.entry = tk.Entry(root)
        stvar=tk.StringVar()
        stvar.set("one")

        self.canvas=tk.Canvas(root, width=canvas_width, height=canvas_height, background='grey')
        self.canvas.grid(row=0,column=1,padx=10)

        #frame = Frame(self.root)
        #frame.grid(row=0,column=0, sticky="n")
        label1=Label(self.root, text="Grid size")
        label1.grid(row=0,column=0, sticky="nw")
        #label2=Label(self.root, text="X").grid(row=1,column=0, sticky="w")
        # self.option.grid(row=0,column=1,sticky="nwe")
        e = Entry(self.root)
        e.grid(row = 1,column = 1,sticky = E+ W)
        text = e.get()
        print text
        Button1=Button(self.root,text="Connect",command=connect).grid(row = 3,column = 1, sticky = "we")

        for i in range(x):
        	for j in range(y):
        		dx = canvas_width/x
        		dy = canvas_height/y
        		draw(self,dx*i,dy*j,dx/2)
        # Grid.columnconfigure(self.root,1,weight=1, size=200)
        drawRobot(self,dx,dy,300,1,1,2)
        #drawRobot(self,dx,dy,300,1,0,0)

    def handler(self):
        if tkMessageBox.askokcancel("Quit?", "Are you sure you want to quit?"):
            #client_sock.close()
            #server_sock.close()
            exitGui()
            print "1"
            quit = True
            print "2"
            print("vamos a la playa")
            self.root.quit()
            print "3"
            
    def parser(self, data):
        drawRobot(self,20,20,300,1,1,1)
        
if __name__== '__main__':
    global quit
    global client_sock, client_info
    quit = False
    thread = Thread(target = listenBluetooth)
    thread.start()
    threads.append(thread)
    
    thread1 = Thread(target = guiMain)
    thread1.start()
    threads.append(thread1)
    

    
    for thread in threads:
        thread.join()

    #client_sock.close()

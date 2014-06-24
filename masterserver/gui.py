from threading import Thread
import Tkinter as tk
#import mtTkinter as tk
from Tkinter import *
#from mtTkinter import *
from bluetooth import *
import tkMessageBox
import random
from time import *
import re
import select

canvas_width = 600
canvas_height = 600

x = 4
y = 4

global robotX
robotX = 0
global robotY
robotY = 0
global ID
ID = 0


global OccupiedLocations
OccupiedLocations = [[0 for x in xrange(5)] for x in xrange(5)] 
for i in range(5):
        OccupiedLocations[4][i] = 1
        OccupiedLocations[0][i] = 1
        OccupiedLocations[i][4] = 1
        OccupiedLocations[i][0] = 1
        
global robots
robots = {}

global direction
direction = "None"
global clients
clients = []

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

read = [server_sock]

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
    currentLocation = "[1, 1]"
    global direction
    global OccupiedLocations
    confirmation = "False"

    ReservedLocations = ["[0, 1]", "[0, 2]", "[0, 3]", "[4, 1]", "[4, 2]", "[4, 3]", "[1, 0]", "[2, 0]", "[3, 0]", "[1, 4]", "[2, 4]", "[3, 4]"]
    
    
    
    def Confirm(currentLocation, direction):
        # Also needs a global array with all the reserved locations.
        # Currently named: 'ReservedLocations' */
        # Initialize 'newPosition' based on parameters.
        newPostion = ""
        newX = 0
        newY = 0
        test = currentLocation.replace("Locatie: ", "")
        currentX = int(test[1])
        currentY = int(test[4])
        if direction == "North":
            #newPosition = "[" + str(currentX) + ", " + str(currentY + 1) + "]"
            newX = currentX
            newY = currentY + 1
        elif direction == "East":
            #newPosition = "[" + str(currentX + 1) + ", " + str(currentY) + "]"
            newX = currentX + 1
            newY = currentY
        elif direction == "South":
            #newPosition = "[" + str(currentX) + ", " + str(currentY - 1) + "]"
            newX = currentX
            newY = currentY - 1
        elif direction == "West":
            #newPosition = "[" + str(currentX - 1) + ", " + str(currentY) + "]"
            newX = currentX - 1
            newY = currentY
          
        if OccupiedLocations[newX][newY] == 1:
            print "locations array"
            return "False"
              
        newPosition = "[" + str(newX) + ", " + str(newY) + "]"
        if newPosition in ReservedLocations:
            print "locations string"
            return "False"
        
        
        OccupiedLocations[newX][newY] = 1
        OccupiedLocations[currentX][currentY] = 0
        
        for i in range(5):
            print OccupiedLocations[i]
            
        return "True"
    
    global root
    print("Waiting for connection on RFCOMM channel %d" %port)
    global quit
    global client_sock, client_info
    #global clients
    #clients.append(server_sock.accept())
    #thread1 = Thread(target = guiMain, args=client_sock)
    #thread1.start()
    #threads.append(thread1)    
    #print("Accepted connection from ", clients[-1][1])
    #clients[-1][0].setblocking(0)
    while True:
        readable, writable, exceptional = select.select(read,read,[])

        for s in readable:
            if s == server_sock:
                conn, addr = server_sock.accept()
                s.setblocking(0)
                print("Accepted connection from ", addr)
                robots[addr] = [ID,robotX,robotY,direction,"red"]
                read.append(conn)
            else:
                data = s.recv(1024)
                if data:
                    if "QRdata: " in data:
                        QRdata = data.replace("QRdata: ", "")
                        currentLocation = QRdata.replace("Locatie: ", "").replace("; Robot-ID: 21;", "")
                        s.send("currentLocation: " + currentLocation)
                    elif "direction: " in data:
                        direction = data.replace("direction: ", "")
                        confirmation = Confirm(currentLocation, direction)
                        if confirmation =="True":
                            s.send("confirmation: " + confirmation)
                        else:
                            s.send("currentLocation: " + currentLocation)
                    print data
                    parser(data)
                    gui()
                    print("received [%s]" % data)
                else:
                    print "connection", addr, "closed"
                    s.close()
                    read.remove(s)
        
        #print quit
        """
        if (quit):
            for client in clients:
                client[0].close()
            break
        elif (listen):
            try:
                for client in clients:
                    data = client[0].recv(1024)
                    if len(data) == 0:
                        pass
                    else:
                        if "QRdata: " in data:
                            QRdata = data.replace("QRdata: ", "")
                            currentLocation = QRdata.replace("Locatie: ", "").replace("; Robot-ID: 21;", "")
                            client[0].send("currentLocation: " + currentLocation)
                        elif "direction: " in data:
                            direction = data.replace("direction: ", "")
                            confirmation = Confirm(currentLocation, direction)
                            if confirmation =="True":
                                client[0].send("confirmation: " + confirmation)
                            else:
                                client[0].send("currentLocation: " + currentLocation)
                        print data
                        parser(data)
                        gui()
                        print("received [%s]" % data)
                
            except IOError:
                pass
                """

def guiMain():
    global root
    global server_sock
    gui()
    #root.protocol("WM_DELETE_WINDOW", gui.handler)
    root.mainloop()
    print("disconnected");
    server_sock.close()

def exitGui():
    #global client_sock
    #client_sock.close()
    server_sock.close()
    #for thread in threads:
        #thread.join()

def connect():
    print "Waiting for connection"
    global client_sock, client_info
    global clients
    clients.append(server_sock.accept())
    print("Accepted connection from ", clients[-1][1])
    clients[-1][0].setblocking(0)
    listen = 1;     

def drawQR(x,y,size,canvas):
    global root
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
            figure=canvas.create_rectangle(nextx,canvas_height-nexty,nextx+dsize,canvas_height-nexty+dsize, fill=color)
        nexty = y

def drawRobot(x,y,size,direction,xco,yco,canvas):
    xco -= 1
    yco -= 1
    x *= xco
    y *= yco

    xleft = x+size/2
    xright = x+size
    ydown = canvas_height-y-size/4
    yup = canvas_height-y-size+size/4

    if (direction == "North"):
        figure = canvas.create_polygon(xleft, ydown, xright, ydown, xleft+size/4, yup, fill="red")
    elif (direction == "East"):
        figure = canvas.create_polygon(xleft, ydown, xleft, yup, xright, ydown-size/4, fill="red")
    elif (direction == "South"):
        figure = canvas.create_polygon(xleft, yup, xright, yup, xleft+size/4, ydown, fill="red")
    elif (direction == "West"):
        figure = canvas.create_polygon(xleft, ydown-size/4, xright, ydown, xright, yup, fill="red")
    #figure=canvas.create_rectangle(x+size/2,canvas_height-y -size/4,x+size,canvas_height-y-size+size/4, fill="yellow")

#def parser(self, data):
    #for s in data:
        #if s
     
    #self.canvas.update_idletasks()
    #drawRobot(self,20,20,300,1,1,1)

def gui():
    global root
    global direction
    entry = tk.Entry(root)
    stvar=tk.StringVar()
    stvar.set("one")

    canvas=tk.Canvas(root, width=canvas_width, height=canvas_height, background='grey')
    canvas.grid(row=0,column=1,ipadx=10,ipady=10)

    #frame = Frame(self.root)
    #frame.grid(row=0,column=0, sticky="n")
    label1=Label(root, text="Grid size")
    label1.grid(row=0,column=0, sticky="nw")
    label2=Label(root, text="X").grid(row=1,column=0, sticky="w")
    # self.option.grid(row=0,column=1,sticky="nwe")
    e = Entry(root)
    e.grid(row = 1,column = 1,sticky = E+ W)
    text = e.get()
    print text
    Button1=Button(root,text="Connect",command=connect).grid(row = 3,column = 1, sticky = "we")

    for i in range(x):
        for j in range(y):
            dx = canvas_width/x
            dy = canvas_height/y
            drawQR(dx*i,dy*j,dx/2, canvas)
    
    
    drawRobot(dx,dy,dx/2,direction,robotX,robotY,canvas)
    #drawRobot(dx,dy,300,1,0,0,canvas)
    #root.mainloop()

def handler(self):
    global quit
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
        
def parser(data):
    global robotX, robotY, ID
    extract = re.findall(r'\d+',data)
    if extract:
        print "poep"
        print extract
        print "poep2"
        robotX = int(extract[0])
        robotY = int(extract[1])
        ID = int(extract[2])
        #Locatie: [1, 1]; Robot-ID: 21;
    else:
        m = re.search('direction: (.+?)', data)
        if m:
            found = m.group(1)
            print found
        
if __name__== '__main__':
    global quit
    global client_sock, client_info
    quit = False
    thread = Thread(target = listenBluetooth)
    thread.start()
    threads.append(thread)
    
    guiMain()

    #thread1 = Thread(target = guiMain)
    #thread1.start()
    #threads.append(thread1)
    

    
    for thread in threads:
        thread.join()

    #client_sock.close()

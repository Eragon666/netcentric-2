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
from random import randint

canvas_width = 600
canvas_height = 600

global x
x = 4
global y
y = 4


colorList = ["red", "blue", "yellow", "green", "orange", "pink", "black", "purple", "brown"]

global OccupiedLocations
OccupiedLocations = [[0 for i in xrange(x+2)] for j in xrange(y+2)] 
for i in range(y+2):
    OccupiedLocations[i][x+1] = 1
    OccupiedLocations[i][0] = 1

for i in range(x+2):
    OccupiedLocations[y+1][i] = 1
    OccupiedLocations[0][i] = 1

global robot_list
robot_list = []

for i in range(1,x*y+1):
        robot_list.append([i, '', ''])
        

global robots
robots = {}

global blocks
blocks = {}
for i in range(x+2):
    for j in range(y+2):
        blocks[(i,j)] = [False, "black"]

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
    global robots
    global x
    global y
    currentLocation = "[1, 1]"
    robot_id = ""
    global direction
    global OccupiedLocations
    confirmation = "False"    
    
    
    def Confirm(currentLocation, direction):
        # Also needs a global array with all the reserved locations.
        # Currently named: 'OccupiedLocations' */
        # Determine the new position based on parameters.
        newX = 0
        newY = 0
        test = currentLocation.replace("Locatie: ", "")
        currentX = int(test[1])
        currentY = int(test[4])
        if direction == "North":
            newX = currentX
            newY = currentY + 1
        elif direction == "East":
            newX = currentX + 1
            newY = currentY
        elif direction == "South":
            newX = currentX
            newY = currentY - 1
        elif direction == "West":
            newX = currentX - 1
            newY = currentY
          
        if OccupiedLocations[newY][newX] == 1:
            print "locations array"
            return "False"
              
        
        OccupiedLocations[newY][newX] = 1
        OccupiedLocations[currentY][currentX] = 0
        
        for i in range(y+2):
            print OccupiedLocations[y+1-i]
            
        return "True"
    
    
    global root
    global colorList
    print("Waiting for connection on RFCOMM channel %d" %port)

    while True:
        readable, writable, exceptional = select.select(read,read,[])

        for s in readable:
            if s == server_sock:
                conn, addr = server_sock.accept()
                s.setblocking(0)
                read.append(conn)
                robots[conn] = [0,0,0,0,colorList[len(read) - 2]]

                random_val = randint(0,x*y-1)
                while robot_list[random_val][1] != '':
                        random_val = randint(0,x*y-1)
                        
                robot_list[random_val][1] = addr[0]
                robot_list[random_val][2] = conn
                print("Accepted connection from ", addr, "with robotid: ", robot_list[random_val][0])
            else:
                data = s.recv(1024)
                
                if data:
                    if "QRdata: " in data:
                        QRdata = data.replace("QRdata: ", "")
                        currentLocation = QRdata[9:15]
                        robot_id = QRdata[27:29]

                        for mac in robot_list:
                                if mac[0] == int(robot_id):
                                        for write in writable:
                                                if write == mac[2]:
                                                        setBlocks(currentLocation, robots[write][4])
                                                        print "Sent message to ", write
                                                        write.send("finalDestination: " + currentLocation)
                                        
                        
                        s.send("currentLocation: " + currentLocation)
                    elif "direction: " in data:
                        direction = data.replace("direction: ", "")

                        #Close connection when robot is on final location
                        if direction == "Done":
                            print "connection", addr, "closed"
                            s.close()
                            read.remove(s)
                            break
                            
                        confirmation = Confirm(currentLocation, direction)
                        if confirmation =="True":
                            s.send("confirmation: " + confirmation)
                        else:
                            s.send("currentLocation: " + currentLocation)
                    print data
                    parser(data, s)
                    
                    gui()
                    print("received [%s]" % data)
                else:
                    for mac in robot_list:
                                if mac[2] == s:
                                        print "connection", mac[1], "closed"
                                        
                                        mac[1] = ''
                                        mac[2] = ''
                        
                                        s.close()
                                        read.remove(s)

def guiMain():
    global root
    global server_sock
    gui()
    root.mainloop()
    print("disconnected");
    server_sock.close()

def exitGui():
    server_sock.close()


def connect():
    print "Waiting for connection"
    global client_sock, client_info
    global clients
    clients.append(server_sock.accept())
    print("Accepted connection from ", clients[-1][1])
    clients[-1][0].setblocking(0)
    listen = 1;     

def drawQR(canvas):

    '''for i in range(4):
        nextx = nextx+dsize
        for j in range(4):
            if(random.randint(0,1)==1):
                color ="black"
            else:
                color="white"
            nexty = nexty+dsize
            figure=canvas.create_rectangle(nextx,canvas_height-nexty,nextx+dsize,canvas_height-nexty+dsize, fill=color)
        nexty = y
''' 
    
    dx = canvas_width/x
    dy = canvas_height/y
    size = dx/2
    
    for i in range(x):
        for j in range(y):
            if (blocks[(i,j)][0]):
                figure = canvas.create_rectangle(dx*i +size/4, canvas_height-dy*j, dx*i+size+size/4, canvas_height-dy*j-size, width=2, outline=blocks[(i,j)][1], fill="white")
          

def drawRobot(direction,xco,yco,canvas, color):
    xco -= 1
    yco -= 1
    dx = canvas_width/x 
    dy = canvas_height/y 
    size = dx/2
    xplace = dx*xco
    yplace = dy*yco

    xleft = xplace+size/2
    xright = xplace+size
    ydown = canvas_height-yplace-size/4
    yup = canvas_height-yplace-size+size/4

    if (direction == "North"):
        figure = canvas.create_polygon(xleft, ydown, xright, ydown, xleft+size/4, yup, fill=color)
    elif (direction == "East"):
        figure = canvas.create_polygon(xleft, ydown, xleft, yup, xright, ydown-size/4, fill=color)
    elif (direction == "South"):
        figure = canvas.create_polygon(xleft, yup, xright, yup, xleft+size/4, ydown, fill=color)
    elif (direction == "West"):
        figure = canvas.create_polygon(xleft, ydown-size/4, xright, ydown, xright, yup, fill=color)

def gui():
    global root

    entry = tk.Entry(root)
    stvar=tk.StringVar()
    stvar.set("one")

    canvas=tk.Canvas(root, width=canvas_width, height=canvas_height, background='grey')
    canvas.grid(row=0,column=1,ipadx=10,ipady=10)

    e = Entry(root)
    e.grid(row = 1,column = 1,sticky = E+ W)
    text = e.get()
    Button1=Button(root,text="Connect",command=connect).grid(row = 3,column = 1, sticky = "we")

    drawQR(canvas)

    for key, value in robots.items():
        drawRobot(value[3],value[1],value[2],canvas, value[4])

def handler(self):
    global quit
    if tkMessageBox.askokcancel("Quit?", "Are you sure you want to quit?"):
        #server_sock.close()
        exitGui()
        quit = True
        self.root.quit()
        
def setBlocks(location, color):
    extract = re.findall(r'\d+', location)
    if extract:
        i = int(extract[0])
        j = int(extract[1])
        
        blocks[(i-1, j-1)] = [True, color]
    
def parser(data, conn):
    extract = re.findall(r'\d+',data)
    if extract:
        print extract
        robotX = int(extract[0])
        robotY = int(extract[1])
        ID = int(extract[2])
        direction = "North"
        
        robots[conn][0] = ID
        robots[conn][1] = robotX
        robots[conn][2] = robotY
        robots[conn][3] = direction
        blocks[(robotX-1, robotY-1)][0] = True
    else:
        m = re.search('direction: (.+?)', data)
        if m:
            found = m.group(1)
            if found == "N":
                robots[conn][3] = "North"
            elif found == "W":
                robots[conn][3] = "West"
            elif found == "E":
                robots[conn][3] = "East"
            elif found == "S":
                robots[conn][3] = "South"
            else:
                robots[conn][3] = "None"
        
if __name__== '__main__':
    quit = False
    
    thread = Thread(target = listenBluetooth)
    thread.start()
    threads.append(thread)
    
    guiMain()
    
    for thread in threads:
        thread.join()

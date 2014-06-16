
import Tkinter as tk
from Tkinter import *
import tkMessageBox
import random

canvas_width = 700
canvas_height = 700

x = 20
y = 20

def handler():
	if tkMessageBox.askokcancel("Quit?", "Are you sure you want to quit?"):
		root.quit()

def connect():
	print "connecting..."

def draw(self,x,y,size):
    	drawQR(self,x,y,size)

def drawQR(self,x,y,size):
		dsize = size/5
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


class Gui():
    def __init__(self, root):
        self.root=root
        self.entry = tk.Entry(root)
        stvar=tk.StringVar()
        stvar.set("one")

        self.canvas=tk.Canvas(root, width=canvas_width, height=canvas_height, background='grey')
        self.canvas.grid(row=0,column=1,padx=10)

        frame = Frame(self.root)
        frame.grid(row=0,column=0, sticky="n")
        label1=Label(frame, text="Grid size").grid(row=0,column=0, sticky="nw")
        label2=Label(frame, text="X").grid(row=1,column=0, sticky="w")
        label3=Label(frame, text="Y").grid(row=2,column=0, sticky="w")
        # self.option.grid(row=0,column=1,sticky="nwe")
        entryX = Entry(frame).grid(row = 1,column = 1,sticky = E+ W)
        entryY = Entry(frame).grid(row = 2,column = 1, sticky = E)

        Button1=Button(frame,text="Connect",command=connect).grid(row = 3,column = 1, sticky = "we")

        for i in range(x):
        	for j in range(y):
        		dx = canvas_width/x
        		dy = canvas_height/y
        		draw(self,dx*i,dy*j,dx/2)
        # Grid.columnconfigure(self.root,1,weight=1, size=200)




if __name__== '__main__':
    root=tk.Tk()
    root.resizable(width=FALSE, height=FALSE)
    gui=Gui(root)
    root.protocol("WM_DELETE_WINDOW", handler)
    root.mainloop()






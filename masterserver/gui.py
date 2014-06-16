
import Tkinter as tk
from Tkinter import *
import tkMessageBox

canvas_width = 800
canvas_height = 600

def handler():
	if tkMessageBox.askokcancel("Quit?", "Are you sure you want to quit?"):
		root.quit()

def connect():
	print "connecting..."






class Gui():
    def __init__(self, root):
        self.root=root
        self.entry = tk.Entry(root)
        stvar=tk.StringVar()
        stvar.set("one")
        self.option=tk.OptionMenu(root, stvar, "one", "two", "three")

        self.canvas=tk.Canvas(root, width=canvas_width, height=canvas_height, background='grey')
        self.canvas.grid(row=0,column=1)

        frame = Frame(self.root)
        frame.grid(row=0,column=0, sticky="n")
        label1=Label(frame, text="Grid size").grid(row=0,column=0, sticky="nw")
        label2=Label(frame, text="X").grid(row=1,column=0, sticky="w")
        label3=Label(frame, text="Y").grid(row=2,column=0, sticky="w")
        self.option.grid(row=0,column=1,sticky="nwe")
        entryX = Entry(frame).grid(row = 1,column = 1,sticky = E+ W)
        entryY = Entry(frame).grid(row = 2,column = 1, sticky = E)

        Button1=Button(frame,text="Connect",command=connect).grid(row = 3,column = 1, sticky = "we")

        for i in range(0,4):
        	for j in range(0,4):	
				figure1=self.canvas.create_rectangle(50*i, 50*j, 20+50*i, 20+50*j, fill="blue")
        # Grid.columnconfigure(self.root,1,weight=1, size=200)

if __name__== '__main__':
    root=tk.Tk()
    root.resizable(width=FALSE, height=FALSE)
    gui=Gui(root)
    root.protocol("WM_DELETE_WINDOW", handler)
    root.mainloop()






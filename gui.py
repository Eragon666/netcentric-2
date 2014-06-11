#simple GUI
import sys

from Tkinter import *




#create the window
root = Tk()

#modify root window
root.title("Master control monitor")
root.geometry("400x400+200+200")

def okdoei():
	label2 = Label(root, text="OK DOEI", fg="#FF9231", bg="#666").pack()

bbut = Button(root, text='abi').grid(row=4)
Label(root, text="plattegrond", fg="#FF9231", bg="#666").grid(row=0, columnspan=8)
for r in range(3):
	for c in range(4):
		Label(root, text='R%s/C%s'%(r,c), fg="#FF9231", bg="#666", borderwidth=1).grid(row=r+1, column=c)

bbut = Button(root, text='abi', command = okdoei).grid(row=4)
#ONLY FOR WINDOWS USERS!! niet voor Daan haha
root.mainloop()
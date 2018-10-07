#!/usr/bin/python
from concurrent.futures.thread import ThreadPoolExecutor
from tkinter import *
import threading
import argparse
import socket

class RedShift:

    def __init__(self, ip, port):
        self.buildGUI()
        self.connection = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.host = socket.gethostbyname(ip)
        self.port = int(port)
        self.messageQueue = []
        self.connection.connect((self.host,self.port))
        with ThreadPoolExecutor(max_workers=2) as executor:
            executor.submit(self.listener)
            self.root.mainloop()
        self.connection.close()

    def listener(self):
        running = 1
        while running:
            message = self.connection.recv(1024).decode()
            self.textbox.configure(state='normal')
            self.textbox.insert(END, message)
            self.textbox.see(END)
            self.textbox.configure(state='disabled')

    def dispatchOnEvent(self, event):
        self.dispatcher()

    def dispatcher(self):
        message = self.field.get() + '\n'
        self.field.delete(0, END)
        if(message != '\n'):
            self.connection.send(message.encode())

    def on_closing(self):
        self.connection.close()
        self.root.destroy()

    def buildGUI(self):
        self.root = Tk()
        self.root.protocol("WM_DELETE_WINDOW", self.on_closing)
        self.root.title("RedShift")
        self.scrollbar = Scrollbar(self.root)
        self.textbox = Text(self.root, height=10, width=50)
        self.textbox.grid(row=0, column=0)
        self.scrollbar.grid(row=0, column=1)
        self.scrollbar.config(command=self.textbox.yview)
        self.textbox.config(yscrollcommand=self.scrollbar.set)
        self.textbox.configure(state='disabled')
        self.field = Entry(self.root)
        self.field.grid(row=1, column=0)
        self.field.bind('<Return>', self.dispatchOnEvent)
        self.field.focus()
        self.button = Button(text="Submit", command=self.dispatcher)
        self.button.grid(row=1, column=1)

def main(ip, port):
    RedShift(ip, port)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Messaging platform.')
    parser.add_argument('ip')
    parser.add_argument('port')
    args = parser.parse_args()
    main(args.ip, args.port)
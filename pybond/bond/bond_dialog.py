from Tkinter import *
from ScrolledText import ScrolledText
import functools


class OptionDialog(Frame):
    """
    A Tkinter dialog window used for presenting the user with custom options.
    """

    def __init__(self, master, prompt, options):
            Frame.__init__(self, master)
            self.pack()
            self.last_button = options[-1]
            self._create_widgets(prompt, options, master)
            self._center(master)
            master.deiconify()
            # PyCharm likes to grab focus back from the dialog if the window is only marked as
            # topmost temporarily, which is very annoying, so leave it as topmost.
            # TODO ETK When settings are better laid out, this should be configurable.
            master.attributes('-topmost', 1)

    @staticmethod
    def create_dialog_get_value(prompt, options):
        """
        The main entrypoint; creates a dialog, presents it to the user, and returns
        the value selected by the user. If the user closes the dialog box before selecting
        an option, the default option is used.
        :param prompt: The prompt string to display to the user.
        :param options: A tuple of options (strings) to display to the user, which will
                        each get their own button. The last option is the default.
        :return: The option that the user selected.
        """
        root = Tk()
        root.title('Bond Reconciliation')
        dialog = OptionDialog(root, prompt, options)
        dialog.mainloop()
        try:
            root.destroy()
        except TclError:
            pass  # Probably already destroyed, just ignore
        return dialog.last_button

    def _create_widgets(self, prompt, options, master):

        def fill_last_button(button_text):
            self.last_button = button_text
            self.quit()

        num_cols = max(50, max(map(lambda line: len(line), prompt.split('\n'))))
        num_rows = prompt.count('\n') + 2

        textbox = ScrolledText(self, relief='flat', padx='8', pady='5',
                               height=num_rows, width=num_cols)
        textbox.insert(INSERT, prompt)
        textbox['state'] = DISABLED
        textbox.pack(side='top')

        for option in reversed(options):
            opt_button = Button(self,
                                text=option,
                                command=functools.partial(fill_last_button, option),
                                default='active' if option == options[-1] else 'normal')
            opt_button.pack(side='right')

        screen_height = master.winfo_screenheight()
        master.update_idletasks()
        size_y = int(master.geometry().split('+')[0].split('x')[1])
        while size_y > screen_height*0.85:
            textbox['height'] -= 5
            master.update_idletasks()
            size_y = int(master.geometry().split('+')[0].split('x')[1])

    def _center(self, master):
        master.update_idletasks()
        screen_width = master.winfo_screenwidth()
        screen_height = master.winfo_screenheight()
        size_x, size_y = tuple(int(_) for _ in master.geometry().split('+')[0].split('x'))
        x = screen_width/2 - size_x/2
        y = screen_height/2 - size_y/2
        master.geometry("{}x{}+{}+{}".format(size_x, size_y, x, y))
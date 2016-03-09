#!/usr/bin/env python

from Tkinter import *
from ScrolledText import ScrolledText
import functools


class OptionDialog(Frame):
    """
    A Tkinter dialog window used for presenting the user with custom options.
    """

    def __init__(self, master, before_prompt, after_prompt, content, options, editable_content=False, rows=None):
            Frame.__init__(self, master)
            self.pack()
            self.last_button = options[-1]
            self.content = None
            self.textbox = None
            self._create_widgets(before_prompt, after_prompt, content, options, master, editable_content, rows)
            self._center(master)
            master.deiconify()
            # PyCharm likes to grab focus back from the dialog if the window is only marked as
            # topmost temporarily, which is very annoying, so leave it as topmost.
            # TODO ETK When settings are better laid out, this should be configurable.
            master.attributes('-topmost', 1)

    @staticmethod
    def create_dialog_get_value(before_prompt, after_prompt, content, options):
        """
        The main entrypoint; creates a dialog, presents it to the user, and returns
        the value selected by the user. If the user closes the dialog box before selecting
        an option, the default option is used.
        :param before_prompt: A prompt string to display above the content.
        :param after_prompt: A prompt string to display below the content.
        :param content: The main block of text.
        :param options: A tuple of options (strings) to display to the user, which will
                        each get their own button. The last option is the default.
        :return: The option that the user selected.
        """
        val, content = OptionDialog.create_dialog_get_value_and_content(before_prompt, after_prompt,
                                                                        content, options, False)
        return val

    @staticmethod
    def create_dialog_get_value_and_content(before_prompt, after_prompt, content, options, editable_content=True):
        """
        The main entrypoint; creates a dialog, and presents it to the user. The content is
        in an editable text box. Once a button is clicked, the value selected is returned,
        as well as the current contents of the text box. If the user closes the dialog box
        before selecting an option, the default option is used.
        :param before_prompt: A prompt string to display above the content.
        :param after_prompt: A prompt string to display below the content.
        :param content: The main block of text which can be edited.
        :param options: A tuple of options (strings) to display to the user, which will
                        each get their own button. The last option is the default.
        :return: The option that the user selected.
        """
        #root = Tk()
        rows = None
        while True:
            try:
                root = Tk()
                dialog = OptionDialog(root, before_prompt, after_prompt, content, options, editable_content, rows)
                break
            except OptionDialog.WindowResizeError as e:
                # Sometimes the window refuses to resize; in this case we have to just start
                # over from scratch with a smaller size.
                root.destroy()
                rows = e.next_row_value
        root.title('Bond Reconciliation')
        dialog.mainloop()
        try:
            root.destroy()
        except TclError:
            pass  # Probably already destroyed, just ignore
        return dialog.last_button, dialog.content

    def _create_widgets(self, before_prompt, after_prompt, content, options, master, editable_content, rows=None):

        def fill_last_button(button_text):
            self.content = self.textbox.get('1.0', END)
            self.last_button = button_text
            self.quit()

        num_cols = max(50,
                       max(map(lambda line: len(line), content.split('\n'))),
                       max(map(lambda line: len(line), before_prompt.split('\n'))),
                       max(map(lambda line: len(line), after_prompt.split('\n'))))
        num_rows = rows if rows else content.count('\n') + 2

        font = 'serif 10 normal'

        if before_prompt != '':
            before_label = Label(self, text=before_prompt, justify='left', anchor='nw', width=num_cols, font=font)
            before_label.pack(side='top')

        if content != '':
            self.content = content
            self.textbox = ScrolledText(self, relief='flat', padx='8', pady='5',
                                        height=num_rows, width=num_cols, font=font)
            self.textbox.insert(INSERT, content)
            if not editable_content:
                self.textbox['state'] = DISABLED
            self.textbox.pack(side='top')

        if after_prompt != '':
            after_label = Label(self, text=after_prompt, justify='left', anchor='nw', width=num_cols, font=font)
            after_label.pack(side='top')

        for option in reversed(options):
            opt_button = Button(self,
                                text=option,
                                command=functools.partial(fill_last_button, option),
                                default='active' if option == options[-1] else 'normal')
            opt_button.pack(side='right')

        if content != '':
            screen_height = master.winfo_screenheight()
            master.update_idletasks()
            size_y = int(master.geometry().split('+')[0].split('x')[1])
            last_y = -1
            while size_y > screen_height*0.85:
                self.textbox['height'] -= 5
                master.update_idletasks()
                size_y = int(master.geometry().split('+')[0].split('x')[1])
                if last_y == size_y:
                    # Reduce more aggressively when using this error since it causes a lot
                    # more screen flashing
                    raise self.WindowResizeError(int(self.textbox['height']*0.75))
                last_y = size_y

    def _center(self, master):
        master.update_idletasks()
        screen_width = master.winfo_screenwidth()
        screen_height = master.winfo_screenheight()
        size_x, size_y = tuple(int(_) for _ in master.geometry().split('+')[0].split('x'))
        x = screen_width/2 - size_x/2
        y = screen_height/2 - size_y/2
        master.geometry("{}x{}+{}+{}".format(size_x, size_y, x, y))

    class WindowResizeError(Exception):
        def __init__(self, next_row_value):
            self.next_row_value = next_row_value


if __name__ == '__main__':
    import optparse

    usage = """%prog --before-prompt "prompt string" --after-prompt "prompt string" \
            --content "content string" [ --editable-content ] "dialog option one" "dialog option two" ..."""
    optParser = optparse.OptionParser(usage=usage,
                                      description='Display a dialog to the user and retrieve input; '
                                                  'prints user input to standard out')

    optParser.add_option('--before-prompt', dest='before_prompt', type='string', default='',
                         help='The prompt to display to the user above the content.')
    optParser.add_option('--after-prompt', dest='after_prompt', type='string', default='',
                         help='The prompt to display to the user below the content.')
    optParser.add_option('--content', dest='content', type='string', default='',
                         help='The content to display to the user.')
    optParser.add_option('--editable-content', dest='editable_content', action="store_true", default=False,
                         help='If this flag is present, the content will be editable.')
    (opts, args) = optParser.parse_args()
    if len(args) < 1:
        optParser.error("Must supply at least one dialog option")

    if opts.editable_content:
        (opt, content) = \
            OptionDialog.create_dialog_get_value_and_content(opts.before_prompt, opts.after_prompt, opts.content, args)
        print(opt)
        print(content)
    else:
        print(OptionDialog.create_dialog_get_value(opts.before_prompt, opts.after_prompt, opts.content, args))



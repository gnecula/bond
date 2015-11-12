package bond.reconcile;

import com.google.common.base.Splitter;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ReconcileDialog extends JDialog implements ActionListener {

  private String value;

  public ReconcileDialog(String prompt, List<String> options) {
    super((JFrame) null, "Bond Reconciliation", true);

    if (options.size() == 0) {
      throw new IllegalArgumentException("Must pass in at least one option!");
    }

    int numCols = 50;
    int numRows = 0;
    for (String line : Splitter.on("\n").split(prompt)) {
      numCols = Math.max(numCols, line.length() + 5);
      numRows++;
    }

    JTextArea textArea = new JTextArea(numRows, numCols);
    textArea.setEditable(false);
    textArea.setText(prompt);
    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    Dimension prefSize = textArea.getPreferredSize();
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(((int) prefSize.getWidth()) + 15, ((int) prefSize.getHeight()) + 15));

    JButton defaultButton = null;
    JPanel buttonPane = new JPanel();
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    buttonPane.add(Box.createHorizontalGlue());
    for (int i = 0; i < options.size(); i++) {
      JButton button = new JButton(options.get(i));
      buttonPane.add(button);
      button.addActionListener(this);
      if (i == options.size() - 1) {
        defaultButton = button;
      } else {
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
      }
    }

    value = defaultButton.getText();

    getRootPane().setDefaultButton(defaultButton);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    getContentPane().add(buttonPane, BorderLayout.PAGE_END);
    pack();

    // Center on the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation((screenSize.width / 2) - (getWidth() / 2), (screenSize.height / 2) - (getHeight() / 2));
  }

  public void actionPerformed(ActionEvent event) {
    value = event.getActionCommand();
    this.dispose();
  }

  public static String showDialogGetValue(String prompt, List<String> options) {
    ReconcileDialog dialog = new ReconcileDialog(prompt, options);
    dialog.setVisible(true);
    return dialog.value;
  }
}

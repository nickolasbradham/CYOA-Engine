package nbradham.cyoa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Handles all program execution.
 *
 * @author Nickolas S. Bradham
 *
 */
final class CYOAEngine {

    private void start() {

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Choose Your Own Adventure Engine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
            JMenu fileMen = new JMenu("File");
            JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
            JLabel head = new JLabel("Open an adventure to begin.");
            JTextArea area = new JTextArea(30, 50);
            JPanel optPane = new JPanel();
            fileMen.add(createMenuItem("Open", new ActionListener() {
                private final StringBuilder sb = new StringBuilder();
                private BufferedReader reader;
                private char c;
                private short line;
                private boolean run;

                @Override
                public void actionPerformed(ActionEvent e) {
                    jfc.setFileFilter(new FileNameExtensionFilter("Choose your Own Adventure File", "coa"));
                    jfc.setDialogTitle("Open Adventure File");
                    if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                            reader = new BufferedReader(new FileReader(jfc.getSelectedFile()));
                            reader.mark(0);
                            line = 0;
                            parseStory();
                        } catch (IOException ex) {
                            Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                private String nextTok() throws IOException {
                    sb.setLength(0);
                    while (!Character.isWhitespace(c = (char) reader.read())) {
                        sb.append(c);
                    }
                    return sb.toString();
                }

                private void parseStory() throws IOException {
                    run = true;
                    while (run) {
                        switch (nextTok()) {
                            case ">c" ->
                                area.setText("");
                            case ">h" ->
                                head.setText(reader.readLine());
                            case ">l" ->
                                reader.readLine();
                            case ">o" -> {
                                String lab = nextTok();
                                JButton jb = new JButton(reader.readLine());
                                jb.addActionListener(ev -> {
                                    try {
                                        if (!lab.equals("next")) {
                                            while (!nextTok().equals(">l") && !nextTok().equals(lab));
                                        }
                                        parseStory();
                                    } catch (IOException ex) {
                                        Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                });
                                optPane.add(jb);
                            }
                            case ">w" ->
                                run = false;
                            default -> {
                                area.append(sb.toString() + ' ' + reader.readLine());
                            }
                        }
                    }
                }
            }, 'O'));
            fileMen.add(createMenuItem("Load Spot", e -> {
                // TODO: Load Spot.
            }, 'L'));
            JMenuItem save = createMenuItem("Save Spot", e -> {
                // TODO: Save Spot.
            }, 'S');
            save.setEnabled(false);
            fileMen.add(save);
            JMenuBar bar = new JMenuBar();
            bar.add(fileMen);
            frame.setJMenuBar(bar);
            head.setAlignmentX(.5f);
            frame.add(head);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            frame.add(new JScrollPane(area));
            frame.add(optPane);
            frame.pack();
            frame.setVisible(true);
        });
    }

    private static JMenuItem createMenuItem(String txt, ActionListener act, char accel) {
        JMenuItem item = new JMenuItem(txt);
        item.addActionListener(act);
        item.setAccelerator(KeyStroke.getKeyStroke(accel, KeyEvent.CTRL_DOWN_MASK));
        return item;
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("user.dir"));
        new CYOAEngine().start();
    }
}

package nbradham.cyoa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
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
import javax.swing.JOptionPane;
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
                private final StringBuilder sb0 = new StringBuilder(), sb1 = new StringBuilder();
                private final HashMap<String, Object> vars = new HashMap<>();
                private RandomAccessFile raf;
                private String str0, str1;
                private char c;
                private float f;
                private byte n;
                private boolean run;

                @Override
                public void actionPerformed(ActionEvent e) {
                    jfc.setFileFilter(new FileNameExtensionFilter("Choose your Own Adventure File", "coa"));
                    jfc.setDialogTitle("Open Adventure File");
                    if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        try {
                            if (raf != null) {
                                raf.close();
                            }
                            raf = new RandomAccessFile(jfc.getSelectedFile(), "r");
                            parseStory();
                        } catch (IOException ex) {
                            Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                private String nextTok() throws IOException {
                    sb0.setLength(0);
                    while ((c = (char) raf.read()) != 0xFFFF && !Character.isWhitespace(c)) {
                        sb0.append(c);
                    }
                    return c == 0xFFFF ? null : sb0.toString();
                }

                private void parseStory() throws IOException {
                    run = true;
                    while (run && (str0 = nextTok()) != null) {
                        switch (str0) {
                            case ">c" ->
                                area.setText("");
                            case ">cv" ->
                                vars.clear();
                            case ">f" -> {
                                str0 = readLine();
                                n = -1;
                                while (++n < str0.length() && Character.isLetterOrDigit(c = str0.charAt(n)));
                                if (n == str0.length()) {
                                    vars.put(str0, null);
                                } else {
                                    str1 = str0.substring(n + 1);
                                    str0 = str0.substring(0, n);
                                    switch (c) {
                                        case ' ' -> {
                                            try {
                                                vars.put(str0, Float.valueOf(str1));
                                            } catch (NumberFormatException e) {
                                                vars.put(str0, str1);
                                            }
                                        }
                                        case '+' ->
                                            op((a, b) -> a + b);
                                        case '-' ->
                                            op((a, b) -> a - b);
                                        case '*' ->
                                            op((a, b) -> a * b);
                                        case '/' ->
                                            op((a, b) -> a / b);
                                        case '%' ->
                                            op((a, b) -> a % b);
                                    }
                                }
                            }
                            case ">h" ->
                                head.setText(readLine());
                            case ">i" ->
                                vars.put(nextTok(), JOptionPane.showInputDialog(raf.readLine()));
                            case ">j" ->
                                jump(nextTok());
                            case ">jf" ->
                                testExp(() -> {
                                    try {
                                        jump(nextTok());
                                    } catch (IOException ex) {
                                        Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                });
                            case ">l" ->
                                raf.readLine();
                            case ">o" -> {
                                createButton();
                            }
                            case ">of" -> {
                                testExp(() -> {
                                    try {
                                        createButton();
                                    } catch (IOException ex) {
                                        Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                });
                            }
                            case ">t" -> {
                                testExp(() -> {
                                    try {
                                        printRest();
                                    } catch (IOException ex) {
                                        Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                });
                            }
                            case ">w" ->
                                run = false;
                            case "" -> {
                            }
                            default -> {
                                area.append((str0 = sb0.toString()).charAt(0) == '\\' ? str0.substring(1) : str0);
                                area.append(" ");
                                printRest();
                            }
                        }
                    }
                }

                private void op(VarOp o) {
                    try {
                        f = Float.parseFloat(str1);
                    } catch (NumberFormatException e) {
                        f = (float) vars.get(str1);
                    }
                    vars.put(str0, o.doOp((float) vars.getOrDefault(str0, 0f), f));
                }

                private void printRest() throws IOException {
                    area.append(readLine());
                    area.append("\n");
                }

                private void testExp(Runnable r) throws IOException {
                    //TODO Finish
                    n = 0;
                    if ((str0 = nextTok()).charAt(0) == '!' && !vars.containsKey(str0.substring(1))) {
                        r.run();
                    } else {
                        raf.readLine();
                    }
                }

                private void createButton() throws IOException {
                    String lab = nextTok();
                    JButton jb = new JButton(raf.readLine());
                    jb.addActionListener(ev -> {
                        optPane.removeAll();
                        try {
                            if (!lab.equals("next")) {
                                jump(lab);
                            }
                            parseStory();
                        } catch (IOException ex) {
                            Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                    optPane.add(jb);
                    optPane.revalidate();
                    optPane.repaint();
                }

                private void jump(String lab) throws IOException {
                    if (forward(lab)) {
                        raf.seek(0);
                        forward(lab);
                    }
                }

                private boolean forward(String lab) throws IOException {
                    while ((str0 = nextTok()) != null && !(">l".equals(str0) && lab.equals(nextTok())));
                    return str0 == null;
                }

                private String readLine() throws IOException {
                    sb0.setLength(0);
                    while ((c = (char) raf.read()) != '\n') {
                        switch (c) {
                            case '<' -> {
                                while ((c = (char) raf.read()) != '>') {
                                    sb1.append(c);
                                }
                                sb0.append(vars.get(sb1.toString()));
                                sb1.setLength(0);
                                break;
                            }
                            case '\\' ->
                                sb0.append((char) raf.read());
                            default ->
                                sb0.append(c);
                        }
                    }
                    return sb0.toString();
                }

                @FunctionalInterface
                private static interface VarOp {

                    float doOp(float a, float b);
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

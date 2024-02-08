package nbradham.cyoa;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Random;
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

    private static final String EXT_SPT = "spt", SUF_SPT = '.' + EXT_SPT, STR_NULL = "\0";
    private static final FileNameExtensionFilter FIL_COA = new FileNameExtensionFilter("Choose your Own Adventure File", "coa"), FIL_SPOT = new FileNameExtensionFilter("Spot File", EXT_SPT);
    private static final File F_EMPTY = new File("");

    private final JFrame frame = new JFrame("Choose Your Own Adventure Engine");
    private final JFileChooser jfc = new JFileChooser(System.getProperty("user.dir"));
    private final JLabel head = new JLabel("Open an adventure to begin.");
    private final JTextArea area = new JTextArea(40, 70);
    private final HashMap<String, Object> vars = new HashMap<>();
    private final JPanel optPane = new JPanel();
    private final StringBuilder sb0 = new StringBuilder(), sb1 = new StringBuilder();
    private final Random r = new Random();

    private File file;
    private RandomAccessFile raf;

    private final JMenuItem save = createMenuItem("Save Spot", e -> {
        prepJFC(FIL_SPOT, "Save Spot File");
        if (jfc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File fl = jfc.getSelectedFile();
            if (!fl.getName().endsWith(SUF_SPT)) {
                fl = new File(fl + SUF_SPT);
            }
            try {
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(fl));
                dos.writeUTF(file.getAbsolutePath());
                dos.writeUTF(head.getText());
                dos.writeUTF(area.getText());
                dos.writeByte(optPane.getComponentCount());
                for (Component nb : optPane.getComponents()) {
                    dos.writeUTF(((NButton) nb).j);
                    dos.writeUTF(((NButton) nb).getText());
                }
                dos.writeLong(raf.getFilePointer());
                dos.writeByte(vars.size());
                vars.forEach((k, v) -> {
                    try {
                        dos.writeUTF(k);
                        dos.writeUTF(v == null ? STR_NULL : v.toString());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                dos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }, 'S');

    private String str0, str1;
    private float f;
    private char c;
    private byte n;
    private boolean run, bool0;

    private void start() {
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
            JMenu fileMen = new JMenu("File");
            save.setEnabled(false);
            fileMen.add(createMenuItem("Open", e -> {
                openJFC(FIL_COA, "Open Adventure File", fl -> {
                    save.setEnabled(true);
                    try {
                        file = fl;
                        openFile();
                        parseStory();
                    } catch (IOException ex) {
                        Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }, 'O'));
            fileMen.add(save);
            fileMen.add(createMenuItem("Load Spot", e -> {
                openJFC(FIL_SPOT, "Open Spot File", fl -> {
                    try {
                        DataInputStream dis = new DataInputStream(new FileInputStream(fl));
                        if (!(file = new File(dis.readUTF())).exists() && openJFC(FIL_COA, "COA File missing. Select COA.", sel -> file = sel)) {
                            return;
                        }
                        openFile();
                        head.setText(dis.readUTF());
                        area.setText(dis.readUTF());
                        n = dis.readByte();
                        for (byte i = 0; i < n; ++i) {
                            createButton(dis.readUTF(), dis.readUTF());
                        }
                        raf.seek(dis.readLong());
                        n = dis.readByte();
                        for (byte i = 0; i < n; ++i) {
                            str0 = dis.readUTF();
                            str1 = dis.readUTF();
                            try {
                                vars.put(str0, Float.valueOf(str1));
                            } catch (NumberFormatException ex) {
                                vars.put(str0, str1.equals(STR_NULL) ? null : str1);
                            }
                        }
                        dis.close();
                    } catch (IOException ex) {
                        Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }, 'L'));
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

    private void openFile() throws IOException {
        if (raf != null) {
            raf.close();
        }
        raf = new RandomAccessFile(file, "r");
        optPane.removeAll();
        vars.clear();
    }

    private boolean openJFC(FileNameExtensionFilter filter, String title, FileHandler fh) {
        jfc.setFileFilter(filter);
        jfc.setDialogTitle(title);
        jfc.setSelectedFile(F_EMPTY);
        if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            fh.handle(jfc.getSelectedFile());
            return false;
        }
        return true;
    }

    private void prepJFC(FileNameExtensionFilter filter, String title) {
        jfc.setFileFilter(filter);
        jfc.setDialogTitle(title);
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
                    toSym();
                    if (n == str0.length()) {
                        vars.put(str0, null);
                    } else {
                        splitStr();
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
                case ">i" -> {
                    vars.put(nextTok(), (str0 = JOptionPane.showInputDialog(raf.readLine())) == null ? "" : str0);
                }
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
                case ">r" -> {
                    vars.put(nextTok(), r.nextInt(Integer.valueOf(nextTok()), Integer.valueOf(nextTok())));
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

    private void printRest() throws IOException {
        area.append(readLine());
        area.append("\n");
    }

    private void createButton(String jump, String label) {
        optPane.add(new NButton(jump, label));
        optPane.revalidate();
        optPane.repaint();
    }

    private void createButton() throws IOException {
        createButton(nextTok(), raf.readLine());
    }

    private void testExp(Runnable r) throws IOException {
        str0 = nextTok();
        n = 0;
        toSym();
        if (n < str0.length()) {
            splitStr();
            switch (c) {
                case '=' -> {
                    doComp((a, b) -> a instanceof Number && b instanceof Number && a == b || a.equals(b), r);
                }
                case '!' -> {
                    doComp((a, b) -> a instanceof Number && b instanceof Number && a != b || !a.equals(b), r);
                }
                case '<' -> {
                    doComp((a, b) -> (float) a < (float) b, r);
                }
                case '>' -> {
                    doComp((a, b) -> (float) a > (float) b, r);
                }
            }
        } else if (((bool0 = str0.charAt(0) == '!') && !vars.containsKey(str0.substring(1))) || (!bool0 && vars.containsKey(str0))) {
            r.run();
        } else {
            raf.readLine();
        }
    }

    private void doComp(VarComp c, Runnable r) throws IOException {
        if (c.doComp(vars.getOrDefault(str0, 0f), str1.equals("''") ? "" : getStr1Val())) {
            r.run();
        } else {
            raf.readLine();
        }
    }

    private Object getStr1Val() {
        try {
            return Float.parseFloat(str1);
        } catch (NumberFormatException e) {
            return vars.get(str1);
        }
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

    private void op(VarOp o) {
        f = (float) getStr1Val();
        vars.put(str0, o.doOp((float) vars.getOrDefault(str0, 0f), f));
    }

    private void splitStr() {
        str1 = str0.substring(n + 1);
        str0 = str0.substring(0, n);
    }

    private void toSym() {
        while (++n < str0.length() && Character.isLetterOrDigit(c = str0.charAt(n)));
    }

    private String readLine() throws IOException {
        sb0.setLength(0);
        while ((c = (char) raf.read()) != '\n') {
            switch (c) {
                case '<' -> {
                    while ((c = (char) raf.read()) != '>') {
                        sb1.append(c);
                    }
                    try {
                        sb0.append(String.format((f = (float) vars.get(sb1.toString())) == (byte) f ? "%.0f" : "%f", f));
                    } catch (ClassCastException e) {
                        sb0.append(vars.get(sb1.toString()));
                    }
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

    private String nextTok() throws IOException {
        sb0.setLength(0);
        while ((c = (char) raf.read()) != 0xFFFF && !Character.isWhitespace(c)) {
            sb0.append(c);
        }
        return c == 0xFFFF && sb0.length() == 0 ? null : sb0.toString();
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

    private final class NButton extends JButton {

        private final String j;

        private NButton(String jump, String label) {
            super(label);
            j = jump;

            addActionListener(e -> {
                optPane.removeAll();
                try {
                    if (!j.equals("next")) {
                        jump(j);
                    }
                    parseStory();
                } catch (IOException ex) {
                    Logger.getLogger(CYOAEngine.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }

    @FunctionalInterface
    private static interface FileHandler {

        void handle(File fl);
    }

    @FunctionalInterface
    private static interface VarOp {

        float doOp(float a, float b);
    }

    @FunctionalInterface
    private static interface VarComp {

        boolean doComp(Object a, Object b);
    }
}

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

    /**
     * Handles GUI creation.
     */
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

    /**
     * Closes the current file and opens the new one.
     *
     * @throws IOException
     */
    private void openFile() throws IOException {
        if (raf != null) {
            raf.close();
        }
        raf = new RandomAccessFile(file, "r");
        optPane.removeAll();
        vars.clear();
    }

    /**
     * Configures and opens the JFileChooser. If the user selects a file, Calls
     * {@code fh} on the selected file.
     *
     * @param filter The filter to apply to the chooser.
     * @param title The title to set to the dialogue window.
     * @param fh The {@link FileHandler} to use on selection.
     * @return Returns true if the handler was NOT ran.
     */
    private boolean openJFC(FileNameExtensionFilter filter, String title, FileHandler fh) {
        prepJFC(filter, title);
        jfc.setSelectedFile(F_EMPTY);
        if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            fh.handle(jfc.getSelectedFile());
            return false;
        }
        return true;
    }

    /**
     * Sets the filter and dialogue title of the {@link JFileChooser}
     *
     * @param filter The filter to use.
     * @param title The title to set.
     */
    private void prepJFC(FileNameExtensionFilter filter, String title) {
        jfc.setFileFilter(filter);
        jfc.setDialogTitle(title);
    }

    /**
     * Parses the story until the end of the file is hit or a wait command is
     * triggered.
     *
     * @throws IOException
     */
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
                case ">fd" ->
                    vars.remove(nextTok());
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

    /**
     * Outputs the rest of the current story line to the {@link TextArea}.
     *
     * @throws IOException
     */
    private void printRest() throws IOException {
        area.append(readLine());
        area.append("\n");
    }

    /**
     * Creates a story option {@link NButton} and adds it to the option panel.
     *
     * @param jump The label to jump to when this button is pressed.
     * @param label The button text.
     */
    private void createButton(String jump, String label) {
        optPane.add(new NButton(jump, label));
        optPane.revalidate();
        optPane.repaint();
    }

    /**
     * Parses the current story line into a {@link button}.
     *
     * @throws IOException
     * @see #createButton(java.lang.String, java.lang.String)
     */
    private void createButton() throws IOException {
        createButton(nextTok(), raf.readLine());
    }

    /**
     * Tests the next story token as an expression.
     *
     * @param r The {@link Runnable} to execute if the expression is true.
     * @throws IOException
     */
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

    /**
     * Performs comparison {@code c} and executes {@link Runnable} {@code r} if
     * it is true.
     *
     * @param c The comparison to test.
     * @param r The Runnable to execute on true.
     * @throws IOException
     */
    private void doComp(VarComp c, Runnable r) throws IOException {
        if (c.doComp(vars.getOrDefault(str0, 0f), str1.equals("''") ? "" : getStr1Val())) {
            r.run();
        } else {
            raf.readLine();
        }
    }

    /**
     * Tries to convert {@code str1} to a float or tries to retrieve it from
     * {@code vars}.
     *
     * @return The float retrieved.
     */
    private Object getStr1Val() {
        try {
            return Float.parseFloat(str1);
        } catch (NumberFormatException e) {
            return vars.get(str1);
        }
    }

    /**
     * Moves the story forward to {@code lab}. Will loop through story again if
     * it doesn't find {@code lab} between current location and end.
     *
     * @param lab The label to jump to.
     * @throws IOException
     */
    private void jump(String lab) throws IOException {
        if (forward(lab)) {
            raf.seek(0);
            forward(lab);
        }
    }

    /**
     * Scans forward in the story to label {@code lab} or the end of the file.
     *
     * @param lab The label to move to.
     * @return Returns true if {@code lab} was NOT found by the end of the file.
     * @throws IOException
     */
    private boolean forward(String lab) throws IOException {
        while ((str0 = nextTok()) != null && !(">l".equals(str0) && lab.equals(nextTok())));
        return str0 == null;
    }

    /**
     * Performs {@code o} on {@code str0} in {@code vars} and {@code str1}.
     * Stores the result back in {@code str0} in {@code vars}
     *
     * @param o The {@link  VarOp} to perform.
     */
    private void op(VarOp o) {
        f = (float) getStr1Val();
        vars.put(str0, o.doOp((float) vars.getOrDefault(str0, 0f), f));
    }

    /**
     * Splits {@code str0} at {@code n} and puts the latter half in
     * {@code str1}.
     */
    private void splitStr() {
        str1 = str0.substring(n + 1);
        str0 = str0.substring(0, n);
    }

    /**
     * Finds the first non-letter, non-digit, character in {@code str0} and sets
     * {@code n} to the index.
     */
    private void toSym() {
        while (++n < str0.length() && Character.isLetterOrDigit(c = str0.charAt(n)));
    }

    /**
     * Parses the rest of the story line. Replaces {@code <VAR NAME>} with the
     * variable value. Also respects the escape ({@code \}) character.
     *
     * @return Returns the complete String.
     * @throws IOException
     */
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

    /**
     * Retrieves the next, white space terminated, token from the story.
     *
     * @return Returns the completed token.
     * @throws IOException
     */
    private String nextTok() throws IOException {
        sb0.setLength(0);
        while ((c = (char) raf.read()) != 0xFFFF && !Character.isWhitespace(c)) {
            sb0.append(c);
        }
        return c == 0xFFFF && sb0.length() == 0 ? null : sb0.toString();
    }

    /**
     * Creates a new JMenuItem and sets up the menu.
     *
     * @param txt The label text.
     * @param act The action listener on click.
     * @param accel The accelerator key.
     * @return Returns the new JMenuItem instance.
     */
    private static JMenuItem createMenuItem(String txt, ActionListener act, char accel) {
        JMenuItem item = new JMenuItem(txt);
        item.addActionListener(act);
        item.setAccelerator(KeyStroke.getKeyStroke(accel, KeyEvent.CTRL_DOWN_MASK));
        return item;
    }

    /**
     * Constructs and starts a new CYOAEngine instance.
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        System.out.println(System.getProperty("user.dir"));
        new CYOAEngine().start();
    }

    /**
     * A JButton that holds the jump label and {@link ActionListener}.
     */
    private final class NButton extends JButton {

        private final String j;

        /**
         * Constructs a new NButton.
         *
         * @param jump The label to jump to.
         * @param label The button text.
         */
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

    /**
     * Used to handle {@link File}s.
     */
    @FunctionalInterface
    private static interface FileHandler {

        /**
         * Called when a File needs handled.
         *
         * @param fl The File to handle.
         */
        void handle(File fl);
    }

    /**
     * Handles story variable operations.
     */
    @FunctionalInterface
    private static interface VarOp {

        /**
         * Performs the operation.
         *
         * @param a The first variable.
         * @param b The second variable.
         * @return Returns the output of the operation.
         */
        float doOp(float a, float b);
    }

    /**
     * Handles story variable comparisons.
     */
    @FunctionalInterface
    private static interface VarComp {

        /**
         * Performs the comparison.
         *
         * @param a The first variable.
         * @param b The second variable.
         * @return Returns the comparison result.
         */
        boolean doComp(Object a, Object b);
    }
}

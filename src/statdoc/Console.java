package statdoc;

import java.io.*;
import java.net.URISyntaxException;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class Console extends WindowAdapter implements WindowListener,
        ActionListener {

    private static class Interceptor extends PrintStream {
        public Interceptor(OutputStream out) {
            super(out, true);
        }

        @Override
        public void print(String s) {
            textArea.append(s);
            textArea.append("\n");
            super.print(s);
        }
    }

    private static class ErrorInterceptor extends PrintStream {
        public ErrorInterceptor(OutputStream out) {
            super(out, true);
        }

        @Override
        public void print(String s) {
            textArea.append(s);
            textArea.append("\n");
            super.print(s);
        }
    }

    private static JFrame frame;
    private static JTextArea textArea;

    public Console() {
        // create all components and add them
        frame = new JFrame("Statdoc Console");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = new Dimension((int) (screenSize.width / 2),
                (int) (screenSize.height / 2));
        int x = (int) (frameSize.width / 2);
        int y = (int) (frameSize.height / 2);
        frame.setBounds(x, y, frameSize.width, frameSize.height);

        textArea = new JTextArea();
        textArea.setEditable(false);

        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JButton button = new JButton("stop/close");

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new JScrollPane(textArea),
                BorderLayout.CENTER);
        frame.getContentPane().add(button, BorderLayout.SOUTH);
        frame.setVisible(true);

        frame.addWindowListener(this);
        button.addActionListener(this);

    }

    public synchronized void windowClosed(WindowEvent evt) {
        this.notifyAll(); // stop all threads
        System.exit(0);
    }

    public synchronized void windowClosing(WindowEvent evt) {
        frame.setVisible(false); // default behaviour of JFrame	
        frame.dispose();
    }

    public synchronized void actionPerformed(ActionEvent evt) {
        this.notifyAll(); // stop all threads
        System.exit(0);
    }

    public static void main(String[] args) throws IOException,
            URISyntaxException {
        new Console();

        PrintStream origOut = System.out;
        PrintStream interceptor = new Interceptor(origOut);
        System.setOut(interceptor);

        PrintStream origOutE = System.err;
        PrintStream interceptorE = new ErrorInterceptor(origOutE);
        System.setErr(interceptorE);

        Statdoc.main(args);
    }
}
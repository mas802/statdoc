package statdoc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

/**
 * Main entry point for jar file execution, opens a JFrame to display the out
 * and error streams.
 * 
 * @author Markus Schaffner
 * 
 */
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
        Dimension frameSize = new Dimension(screenSize.width / 2,
                screenSize.height / 2);
        int x = frameSize.width / 2;
        int y = frameSize.height / 2;
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

    @Override
    public synchronized void windowClosed(WindowEvent evt) {
        this.notifyAll();
        System.exit(0);
    }

    @Override
    public synchronized void windowClosing(WindowEvent evt) {
        frame.setVisible(false);
        frame.dispose();
    }

    @Override
    public synchronized void actionPerformed(ActionEvent evt) {
        this.notifyAll();
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
package raft_demo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class GUI extends JFrame {

    private Process[] nodeProcesses = new Process[3];
    private JButton startClusterBtn, stopClusterBtn, connectClientBtn, sendBtn, resetBtn;
    private JTextField commandField;
    private JLabel leaderLabel;
    private JTextArea outputArea;
    private Client client;
    private String currentLogFile = null;
    private javax.swing.Timer logRefreshTimer;

    //--------------------------------------------------------------------------------
    public GUI() {
        super("Raft Demo GUI");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); //WE NEED TO OVERRIDE THIS
        setSize(620, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        //OVERRIDE THIS WILL KILL OUR WINDOW INSTEAD!
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                killAllNodes();
                dispose();
                System.exit(0);
            }
        });

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout(5, 5));
        JPanel buttons_jframe = new JPanel(new GridLayout(3, 1));

        // START AND STOP BUTTONS
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(startClusterBtn = new JButton("Start Cluster"));
        row1.add(stopClusterBtn = new JButton("Stop Cluster"));
        row1.add(resetBtn = new JButton("Hard Reset"));
        JComboBox<String> logDropdown = new JComboBox<>(new String[]{
            "View Logs", "Node 1", "Node 2", "Node 3", "Client"
        });
        String[] logFiles = {null, "node_1.log", "node_2.log", "node_3.log", "client.log"};
        logDropdown.addActionListener(e -> {
            int selected = logDropdown.getSelectedIndex();
            if (selected > 0) {
                currentLogFile = logFiles[selected];
                readLog(currentLogFile);
                logRefreshTimer.start();
            } else {
                currentLogFile = null;
                logRefreshTimer.stop();
            }
        });
        row1.add(logDropdown);

        // REFRESH THE LOGS CHANGE DELAY IF NEEDED
        logRefreshTimer = new javax.swing.Timer(2000, e -> {
            if (currentLogFile != null) readLog(currentLogFile);
        });

        row1.add(new JButton("Clear Output") {{
            addActionListener(e -> {
                outputArea.setText("");
                currentLogFile = null;
                logRefreshTimer.stop();
                logDropdown.setSelectedIndex(0);
            });
        }});
        stopClusterBtn.setEnabled(false);
        startClusterBtn.addActionListener(e -> onStartCluster());
        stopClusterBtn.addActionListener(e -> onStopCluster());
        resetBtn.addActionListener(e -> onReset());
        buttons_jframe.add(row1);

        // CONNECT AND CURRENT LEADER BUTTONS
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(connectClientBtn = new JButton("Connect to Cluster"));
        row2.add(leaderLabel = new JLabel("Leader: unknown"));
        connectClientBtn.addActionListener(e -> onConnectClient());
        buttons_jframe.add(row2);

        // INPUT
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(commandField = new JTextField(30));
        row3.add(sendBtn = new JButton("Send"));
        commandField.setEnabled(false);
        sendBtn.setEnabled(false);
        commandField.addActionListener(e -> onSendCommand());
        sendBtn.addActionListener(e -> onSendCommand());
        buttons_jframe.add(row3);

        add(buttons_jframe, BorderLayout.NORTH);

        // OUTPUT
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
    }

    //STOPING AND STARTING BUTTON FUNCTIONS
    //--------------------------------------------------------------------------------
    private void startNode(int id) {
        try {
            Process proc = new ProcessBuilder("java", "-cp", "bin", "raft_demo.RaftServer", String.valueOf(id))
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            nodeProcesses[id - 1] = proc;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void onStartCluster() {
        startClusterBtn.setEnabled(false);
        new Thread(() -> {
            compileProject();
            startNode(1); startNode(2); startNode(3);
            SwingUtilities.invokeLater(() -> { stopClusterBtn.setEnabled(true); appendOutput("Cluster started.\n"); });
        }).start();
    }

    //--------------------------------------------------------------------------------
    private void onStopCluster() {
        killAllNodes();
        stopClusterBtn.setEnabled(false);
        startClusterBtn.setEnabled(true);
        leaderLabel.setText("Leader: unknown");
        appendOutput("All nodes stopped.\n");
    }

    private void onReset() {
        killAllNodes();
        client = null;
        leaderLabel.setText("Leader: unknown");
        startClusterBtn.setEnabled(true);
        stopClusterBtn.setEnabled(false);
        connectClientBtn.setEnabled(true);
        commandField.setEnabled(false);
        sendBtn.setEnabled(false);
        outputArea.setText("");
        appendOutput("Reset complete.\n");
    }

    // THIS IS JANK, JANK, JANK, im going crazy trying to get this to work dynamically tho 
    private void killAllNodes() {
        if (nodeProcesses[0] != null) { nodeProcesses[0].destroyForcibly(); nodeProcesses[0] = null; }
        if (nodeProcesses[1] != null) { nodeProcesses[1].destroyForcibly(); nodeProcesses[1] = null; }
        if (nodeProcesses[2] != null) { nodeProcesses[2].destroyForcibly(); nodeProcesses[2] = null; }
        appendOutput("All nodes destroyed.\n");
    }
    //--------------------------------------------------------------------------------


    private boolean compileProject() {
        new File("bin").mkdirs();

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            appendOutput("Error Java not found. Make sure Java is installed.\n");
            return false;
        }

        int commands_to_run = compiler.run(null, null, null,
            "-d", "bin",
            "src/raft_demo/RaftServer.java",
            "src/raft_demo/RaftNode.java",
            "src/raft_demo/RaftRPC.java",
            "src/raft_demo/Client.java",
            "src/raft_demo/Logger.java",
            "src/raft_demo/GUI.java"
        );
        return commands_to_run == 0; //note this means if the commands ran successfully, we return true(zero) not that it failed(one)
    }
     //--------------------------------------------------------------------------------
    //CLIENT
    private void onConnectClient() {
        //PORTS (change later if figured out how to do dynamic ports)
        Map<Integer, Integer> members = new HashMap<>();
        members.put(1, 8102);
        members.put(2, 8103);
        members.put(3, 8104);
        client = new Client(members);

        // WE NEED TO USE A THREAD OR IT GOES UNRESPONSIVE
        new Thread(() -> {
            boolean found = client.discoverLeader();
            // UPDATE THE GUI
            SwingUtilities.invokeLater(() -> {
                if (found) {
                    leaderLabel.setText("Leader: Node " + getLeaderId(client));
                    commandField.setEnabled(true);
                    sendBtn.setEnabled(true);
                    appendOutput("Connected!\n");
                } else {
                    leaderLabel.setText("No leader found");
                    appendOutput("No leader found.\n");
                }
            });
        }).start();
    }

    //USES GETTER TO GET THE LEADER ID 
    private String getLeaderId(Client c) {
        Integer id = c.getCurrentLeaderId();
        if (id == null) {
            return "unknown";
        }else{
            return id.toString();
        }
    }

    private void onSendCommand() {
        String cmd = commandField.getText().trim();
        if (cmd.isEmpty() || client == null) {
            return;
        }

        commandField.setText("");
        appendOutput("> " + cmd + "\n");

        // WE NEED TO USE A THREAD OR IT GOES UNRESPONSIVE
        new Thread(() -> {
            String response = client.sendRequest(cmd);
            // UPDATE THE GUI ON THE MAIN THREAD
            SwingUtilities.invokeLater(() -> {
                appendOutput("Response: " + response + "\n");
            });
        }).start();
    }

    // READ A LOG FILE AND PRINT ITS CONTENTS TO THE OUTPUT AREA
    private void readLog(String filename) {
        try {
            outputArea.setText( filename );
            outputArea.append(new String(java.nio.file.Files.readAllBytes(new File("logs/" + filename).toPath())));
        } catch (IOException ex) {
            outputArea.setText(filename + ": " + ex.getMessage() + "\n");
        }
    }

    // OUTPUT
    private void appendOutput(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
     //--------------------------------------------------------------------------------
    // MAIN (ENTRY POINT)

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new File("logs").mkdirs(); //MAKE SURE LOGS HAVE SOMEWHERE TO GO
            GUI gui = new GUI();
            gui.setVisible(true);
        });
    }
}

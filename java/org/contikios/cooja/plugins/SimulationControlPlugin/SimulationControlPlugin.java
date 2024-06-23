package org.contikios.cooja.plugins;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import javax.swing.JInternalFrame;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.contikios.cooja.Cooja;
import org.contikios.cooja.Plugin;
import org.contikios.cooja.plugins.PowerTracker;
import org.contikios.cooja.plugins.PowerTracker.MoteTracker;
import org.contikios.cooja.ClassDescription;
import org.contikios.cooja.dialogs.CreateSimDialog;
import org.contikios.cooja.PluginType;
import org.contikios.cooja.interfaces.Position;
import org.contikios.cooja.Simulation;
import org.contikios.cooja.MoteType;
import org.contikios.cooja.Mote;

@ClassDescription("ControlPlugin")
@PluginType(PluginType.PType.COOJA_STANDARD_PLUGIN)
public class SimulationControlPlugin implements Plugin {

    private final Cooja cooja;
    private static final Logger logger = LoggerFactory.getLogger(SimulationControlPlugin.class.getName());

    private PowerTracker powerTracker;
    private Simulation simulation;
    private ServerSocket serverSocket;

    public SimulationControlPlugin(final Cooja cooja) {
        this.cooja = cooja;
    }

    private final ArrayList<Mote> motes = new ArrayList<>();

    @Override
    public void startPlugin() {
        logger.error("started SimulationControlPlugin");
        loadSimulationFromCSC("/home/suhaib/uni/fyp/contiki-ng/examples/rpl-udp/test.csc");
        // handleCommand("ADD_MOTE%%Z1 Mote Type #2,6,9.419548524027482,5.851214847556069,0.0,5.355039605898617,6.783914814391493,0.0,1.7319899452007514,2.278350119579542,0.0,1.733560739260399,7.9357566779621935,0.0,6.30043722525555,5.697510464034817,0.0,0.27935522662136836,9.340603617820936,0.0", new PrintWriter(System.out));
        // int result = addMoteCommand("Sky Mote Type #sky1,5,2.172833652941598,4.253676307162884,0.0,3.918750397080144,7.84758374242387,0.0,2.2956290626347937,2.7102490468873572,0.0,2.7849813478687477,7.795882943088228,0.0,4.627343928932028,5.726254650764148,0.0");
        // logger.error(Integer.toString(result));
        startServerSocket();
    }

    private int addMoteCommand(String args)
    {
        String[] moteArgs = args.split(",");
        String moteType = moteArgs[0];
        int amountToAdd = Integer.parseInt(moteArgs[1]);
        double[][] positions = new double[amountToAdd][3];
        for (int i = 0; i < amountToAdd; i++) {
            positions[i][0] = Double.parseDouble(moteArgs[2 + i * 3]);
            positions[i][1] = Double.parseDouble(moteArgs[3 + i * 3]);
            positions[i][2] = Double.parseDouble(moteArgs[4 + i * 3]);
        }
        int result = addMote(moteType, amountToAdd, positions); 
        // writer.println("Motes added: " + Integer.toString(result));
        return result;
    }

    private void startServerSocket() {
        try {
            serverSocket = new ServerSocket(8888);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClientConnection(clientSocket);
            }
        } catch (IOException e) {
            logger.error("Error starting server socket: " + e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String command;
                while ((command = reader.readLine()) != null) {
                    logger.info("Received command: " + command);
                    handleCommand(command, writer);
                }
            } catch (IOException e) {
                logger.error("Error handling client connection: " + e.getMessage());
            }
    }

    private void handleCommand(String command, PrintWriter writer) {
        String[] parts = command.split("%%", 2);
        String mainCommand = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (mainCommand) {
            case "LOAD_CSC":
                boolean success = loadSimulationFromCSC(args);
                writer.println(success ? "CSC loaded successfully" : "Failed to load CSC");
                break;
            case "ADD_MOTE":
                String[] moteArgs = args.split(",");
                String moteType = moteArgs[0];
                int amountToAdd = Integer.parseInt(moteArgs[1]);
                double[][] positions = new double[amountToAdd][3];
                for (int i = 0; i < amountToAdd; i++) {
                    positions[i][0] = Double.parseDouble(moteArgs[2 + i * 3]);
                    positions[i][1] = Double.parseDouble(moteArgs[3 + i * 3]);
                    positions[i][2] = Double.parseDouble(moteArgs[4 + i * 3]);
                }
                int result = addMote(moteType, amountToAdd, positions); 
                writer.println("Motes added: " + Integer.toString(result));
                break;
            case "REMOVE_MOTE":
                removeMote(Integer.parseInt(args));
                writer.println("Mote removed");
                break;
            case "STEP_SIMULATION":
                for (int i = 0; i < 120000; i ++)
                { 
                    stepSimulation();
                }
                writer.println("Simulation stepped");
                break;
            case "START_SIMULATION":
                runSimulation();
                writer.println("Simulation started");
                break;
            case "STOP_SIMULATION":
                stopSimulation();
                writer.println("Simulation stopped");
                break;
            case "GET_POWER":
                writer.println(getPowerStatisticsString());
                break;
            default:
                writer.println("Unknown command: " + mainCommand);
        }
    }

    private boolean loadSimulationFromCSC(String cscFilePath) {
        Element root = parseCSCFile(cscFilePath);
        if (root == null) {
            logger.error("Failed to parse CSC file: " + cscFilePath);
            return false;
        }
        var cfg = new CreateSimDialog.SimConfig("My simulation", "org.contikios.cooja.radiomediums.UDGM", false, 123456, (1000 * Simulation.MILLISECOND));
        var config = new Simulation.SimConfig(null, cfg.randomSeed(), false, false,
                Cooja.configuration.logDir(), new HashMap<>());
        try {
            this.simulation = new Simulation(config, cooja, cfg.title(), cfg.generatedSeed(),
                    cfg.randomSeed(), cfg.radioMedium(), cfg.moteStartDelay(), true, root);
            cooja.setSimulation(simulation);
            cooja.tryStartPlugin(PowerTracker.class, simulation, null);
            powerTracker = (PowerTracker) cooja.getPlugin(PowerTracker.class);
            logger.error(powerTracker.radioStatistics(true, true, true));
            logger.error("Simulation loaded successfully from CSC file");
            return true;
        } catch (Exception ex) {
            logger.error("Error loading simulation from CSC file: " + ex.toString());
            return false;
        }
    }

    private double[][] getPowerStatistics() {
        double[][] powerStats = new double[motes.size()][1];
        for (int i = 0; i < motes.size(); i ++) {
            Mote mote = motes.get(i);
            MoteTracker tracker = powerTracker.getMoteTrackerOf(mote);
            powerStats[i][0] = tracker.getRadioTxRatio() + tracker.getRadioRxRatio();
            // powerStats[i][1] = tracker.getRadioTxRatio();
            // powerStats[i][2] = tracker.getRadioRxRatio();
        }
        return powerStats;
    }

    public String getPowerStatisticsString() {
        StringBuilder sb = new StringBuilder();
        double[][] powerStats = getPowerStatistics();
        for (int i = 0; i < powerStats.length; i++) {
            // sb.append(powerStats[i][0]).append(",").append(powerStats[i][1]).append(",").append(powerStats[i][2]);
            sb.append(powerStats[i][0]);
            sb.append(";");
        }
        // Remove the last semicolon
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private int addMote(String moteType, int amountToAdd, double[][] positions) {
        MoteType moteToAdd = null;
        MoteType[] types = simulation.getMoteTypes();
        for (MoteType type : types) {
            if (moteType.equals(type.getDescription())) moteToAdd = type;
        }
        if (moteToAdd == null) {
            logger.error("mote type not found: " + moteType);
            return 0;
        }
        
        ArrayList<Mote> newMotes = new ArrayList<>();
        while (newMotes.size() < amountToAdd) {
            try {
                newMotes.add(moteToAdd.generateMote(cooja.getSimulation()));
            } catch (MoteType.MoteTypeCreationException e) {
                logger.error(e.getMessage());
                return 0;
            }
        }        
        for (int i = 0; i < amountToAdd; i++) {
            Position newPosition = newMotes.get(i).getInterfaces().getPosition();
            if (newPosition != null) {
                newPosition.setCoordinates(positions[i][0], positions[i][1], positions[i][2]);
            }
        }
        int nextMoteID = 1;
        for (Mote m : cooja.getSimulation().getMotes()) {
            int existing = m.getID();
            if (existing >= nextMoteID) {
                nextMoteID = existing + 1;
            }
        }
        for (Mote m : newMotes) {
            var moteID = m.getInterfaces().getMoteID();
            if (moteID != null) {
                moteID.setMoteID(nextMoteID++);
            } else {
                logger.warn("Can't set mote ID (no mote ID interface): " + m);
            }
        }
        int result = 0;
        for (var mote : newMotes) {
            motes.add(mote);
            cooja.getSimulation().addMote(mote);
            result++;
        }
        return result;
    }

    public void removeMote(int index) {
        simulation.removeMote(motes.get(index));
        motes.remove(index);
    }

    public void stepSimulation() {
        simulation.stepMillisecondSimulation();
    }

    public void runSimulation() {
        simulation.startSimulation();
    }

    public void stopSimulation() {
        simulation.stopSimulation();
    }

    @Override
    public JInternalFrame getCooja() {
        return null;
    }

    @Override
    public void closePlugin() {}

    @Override
    public Collection<Element> getConfigXML() {
        return new ArrayList<>();
    }

    @Override
    public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable) {
        return true;
    }

    public static Element parseCSCFile(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(filename);

            // Convert W3C Document to JDOM2 Document
            org.jdom2.Document jdomDoc = new DOMBuilder().build(doc);

            // Get the root Element from the JDOM2 Document
            return jdomDoc.getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

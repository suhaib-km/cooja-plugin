package org.contikios.cooja.plugins;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.contikios.cooja.Cooja;
import org.contikios.cooja.Plugin;
import org.contikios.cooja.ClassDescription;
import org.contikios.cooja.dialogs.CreateSimDialog;
import org.contikios.cooja.PluginType;
import org.contikios.cooja.interfaces.Position;
import org.contikios.cooja.Simulation;
import org.contikios.cooja.MoteType;
import org.contikios.cooja.Mote;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.JInternalFrame;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;

@ClassDescription("ControlPlugin")
@PluginType(PluginType.PType.COOJA_STANDARD_PLUGIN)

public class SimulationControlPlugin implements Plugin {

    private final Cooja cooja;
    private static final Logger logger = LoggerFactory.getLogger(SimulationControlPlugin.class.getName());

    private Simulation simulation;

    public SimulationControlPlugin(final Cooja cooja) {
        this.cooja = cooja;
    }

    
    @Override
    public void startPlugin() 
    {

        logger.error("started");
        Element root = parseCSCFile("/home/suhaib/uni/fyp/contiki-ng/examples/multicast/multicast.csc");
        var cfg = new CreateSimDialog.SimConfig("My simulation", "org.contikios.cooja.radiomediums.UDGM", false, 123456, (1000 * Simulation.MILLISECOND));
        var config = new Simulation.SimConfig(null, cfg.randomSeed(), false, false,
                Cooja.configuration.logDir(), new HashMap<>());
        Simulation sim;
        try {
          sim = new Simulation(config, cooja, cfg.title(), cfg.generatedSeed(),
                  cfg.randomSeed(), cfg.radioMedium(), cfg.moteStartDelay(), true, root);
        } catch (MoteType.MoteTypeCreationException | Cooja.SimulationCreationException ex) {
            logger.error(ex.toString());
            return;
        }

        cooja.setSimulation(sim);
        simulation = sim;
        logger.error("hit sim creation");
    }

    private void addMote(String moteType, int amountToAdd, double[][] positions)
    {

        MoteType moteToAdd = null;
        MoteType[] types = simulation.getMoteTypes();
        for (MoteType type : types) 
        {
            if (moteType.equals(type.getDescription())) moteToAdd = type;
        }
        if (moteToAdd == null) 
        {
            logger.error("mote type not found: " + moteType);
            return;
        }
       
        ArrayList<Mote> newMotes = new ArrayList<>();
        while (newMotes.size() < amountToAdd) {
          try {
            newMotes.add(moteToAdd.generateMote(cooja.getSimulation()));
          } catch (MoteType.MoteTypeCreationException e) {
            logger.error(e.getMessage());
            return;
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
          for (var mote : newMotes) {
            cooja.getSimulation().addMote(mote);
          }
    }


    public void runSimulation()
    {
        simulation.startSimulation(false);
    }

    @Override
    public JInternalFrame getCooja()
    {
        return null;
    }

    @Override
    public void closePlugin()
    {}

    @Override
    public Collection<Element> getConfigXML()
    {
        return new ArrayList<>();
    }

    @Override
    public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable)
    {
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
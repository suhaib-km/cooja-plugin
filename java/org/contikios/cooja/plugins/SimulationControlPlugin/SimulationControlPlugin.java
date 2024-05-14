package org.contikios.cooja.plugins;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.contikios.cooja.Cooja;
import org.contikios.cooja.Plugin;
import org.contikios.cooja.ClassDescription;
import org.contikios.cooja.dialogs.CreateSimDialog;
import org.contikios.cooja.PluginType;
import org.contikios.cooja.Simulation;
import org.contikios.cooja.MoteType;
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

    private void addMote(string name)
    {

        MoteType moteToAdd;
        MoteType[] types = simulation.getMoteTypes();
        for (MoteType type : types) 
        {
            logger.error("TYPES: " + type);
            logger.error("DESCRIPTION: " + type.getDescription());   
            logger.error("=============="); 

        }
        /* 
                returnValue = new MoteAdditions(((Number) numberOfMotesField.getValue()).intValue(),
            Objects.requireNonNull(positionDistributionBox.getSelectedItem()).toString(),
            ((Number) startX.getValue()).doubleValue(), ((Number) endX.getValue()).doubleValue(),
            ((Number) startY.getValue()).doubleValue(), ((Number) endY.getValue()).doubleValue(),
            ((Number) startZ.getValue()).doubleValue(), ((Number) endZ.getValue()).doubleValue());

            var newMoteInfo = AddMoteDialog.showDialog(newMoteType, posDescriptions.toArray(new String[0]));
        if (newMoteInfo == null) return;
        Class<? extends Positioner> positionerClass = null;
        for (var positioner : cooja.getRegisteredPositioners()) {
          if (Cooja.getDescriptionOf(positioner).equals(newMoteInfo.positioner())) {
            positionerClass = positioner;
            break;
          }
        }
        if (positionerClass == null) {
          return;
        }
        Positioner positioner;
        try {
          var constr = positionerClass.getConstructor(int.class, double.class, double.class,
                  double.class, double.class, double.class, double.class);
          positioner = constr.newInstance(newMoteInfo.numMotes(), newMoteInfo.startX(), newMoteInfo.endX(),
                   newMoteInfo.startY(), newMoteInfo.endY(), newMoteInfo.startZ(), newMoteInfo.endZ());
        } catch (Exception e1) {
          logger.error("Exception when creating " + positionerClass + ": ", e1);
          return;
        }

        ArrayList<Mote> newMotes = new ArrayList<>();
        while (newMotes.size() < newMoteInfo.numMotes()) {
          try {
            newMotes.add(newMoteType.generateMote(cooja.getSimulation()));
          } catch (MoteType.MoteTypeCreationException e2) {
            JOptionPane.showMessageDialog(frame,
                    "Could not create mote.\nException message: \"" + e2.getMessage() + "\"\n\n",
                    "Mote creation failed", JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
        // Position new motes.
        for (var newMote : newMotes) {
          Position newPosition = newMote.getInterfaces().getPosition();
          if (newPosition != null) {
            double[] next = positioner.getNextPosition();
            newPosition.setCoordinates(next.length > 0 ? next[0] : 0, next.length > 1 ? next[1] : 0, next.length > 2 ? next[2] : 0);
          }
        }

        // Set unique mote id's for all new motes.
        // TODO: ID should be provided differently; not rely on the unsafe MoteID interface.
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
         */
    }

    private void removeMote(Cooja cooja)
    {

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
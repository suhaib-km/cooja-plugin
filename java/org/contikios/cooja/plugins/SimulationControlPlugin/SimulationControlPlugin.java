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
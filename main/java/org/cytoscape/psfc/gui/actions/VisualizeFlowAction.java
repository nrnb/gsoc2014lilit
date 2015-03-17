package org.cytoscape.psfc.gui.actions;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.*;
import org.cytoscape.psfc.PSFCActivator;
import org.cytoscape.psfc.gui.PSFCPanel;
import org.cytoscape.psfc.net.NetworkCyManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * PUBLIC CLASS VisualizeFlowAction
 * Initializes an iterator of ColorNodesTask,
 * which performs node color visual mapping based on node signals.
 *
 */
public class VisualizeFlowAction extends AbstractCyAction {
    private final double minSignal;
    private final double maxSignal;
    private final PSFCPanel psfcPanel;
    private CyNetwork network;
    private ArrayList<Integer> levels;
    private Color minColor = Color.decode("#40E0D0");
    private Color maxColor = Color.decode("#000000");

    public VisualizeFlowAction(CyNetwork network, double minSignal,
                               double maxSignal, ArrayList<Integer> levels,
                               PSFCPanel psfcPanel) {
        super("VisualizeFlowAction");
        this.network = network;
        this.minSignal = minSignal;
        this.maxSignal = maxSignal;
        this.levels = levels;
        this.psfcPanel = psfcPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TaskIterator taskIterator = new TaskIterator();
        for (int level : levels) {
            ColorNodesTask colorNodesTask = new ColorNodesTask(level);
            taskIterator.append(colorNodesTask);
        }
        PSFCActivator.taskManager.execute(taskIterator);

    }

    private class ColorNodesTask extends AbstractTask {
        private final CyColumn nodeSignalColumn;
        private int level;

        private ColorNodesTask(int level) {
           this.level = level;
            String columnName = CalculateScoreFlowAction.PSFC_SIGNAL + level;
            nodeSignalColumn = network.getDefaultNodeTable().getColumn(columnName);
            if (nodeSignalColumn == null) {
                JOptionPane.showMessageDialog(PSFCActivator.cytoscapeDesktopService.getJFrame(),
                        "CyColumn " + columnName + " was not found for visualization.",
                        "PSFC user message", JOptionPane.OK_OPTION);
            }
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws Exception {
            taskMonitor.setTitle("PSFC:ColorNodesTask");
            psfcPanel.getJsl_levels().setValue(level);
            psfcPanel.updateUI();
            if (nodeSignalColumn == null)
                System.out.println(("CyColumn " + nodeSignalColumn.getName() + " not found."));
            else {
                ContinuousMapping<Double, Paint> nodeColorMapping = null;
                try {
                    nodeColorMapping = (ContinuousMapping<Double, Paint>) PSFCActivator.vmfFactoryC.createVisualMappingFunction
                            (nodeSignalColumn.getName(), Double.class, BasicVisualLexicon.NODE_FILL_COLOR);
                } catch (Exception e) {
                    throw new Exception(nodeSignalColumn.getName() + " should be of type " + Double.class.getName());
                }


                BoundaryRangeValues<Paint> brvMin = new BoundaryRangeValues<Paint>(Color.WHITE, minColor, minColor);

                nodeColorMapping.addPoint(minSignal, brvMin);

                BoundaryRangeValues<Paint> brvMax = new BoundaryRangeValues<Paint>(maxColor, maxColor, Color.BLACK);
                nodeColorMapping.addPoint(maxSignal, brvMax);
                VisualStyle visualStyle = PSFCActivator.visualMappingManager.getVisualStyle(NetworkCyManager.getNetworkView(network));

                CyNetworkView networkView = NetworkCyManager.getNetworkView(network);
                for (CyNode cyNode : network.getNodeList()){
                    View<CyNode> nodeView = networkView.getNodeView(cyNode);
                    nodeView.clearValueLock(BasicVisualLexicon.NODE_FILL_COLOR);
                }

                visualStyle.addVisualMappingFunction(nodeColorMapping);
                visualStyle.apply(networkView);
                networkView.updateView();
                Thread.sleep(500);
            }
        }

    }


}


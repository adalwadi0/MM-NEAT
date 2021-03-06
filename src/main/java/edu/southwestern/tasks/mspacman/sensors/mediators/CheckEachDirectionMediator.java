/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.southwestern.tasks.mspacman.sensors.mediators;

import edu.southwestern.tasks.mspacman.sensors.VariableDirectionBlockLoadedInputOutputMediator;
import edu.southwestern.tasks.mspacman.sensors.blocks.booleansensors.BiasBlock;
import edu.southwestern.tasks.mspacman.sensors.directional.blocking.VariableDirectionPowerPillBlocksThreatGhostBlock;
import edu.southwestern.tasks.mspacman.sensors.directional.blocking.VariableDirectionThreatGhostBlocksJunctionBlock;
import edu.southwestern.tasks.mspacman.sensors.directional.counts.*;
import edu.southwestern.tasks.mspacman.sensors.directional.distance.*;
import edu.southwestern.tasks.mspacman.sensors.directional.distance.ghosts.VariableDirectionEdibleGhostDistanceBlock;
import edu.southwestern.tasks.mspacman.sensors.directional.distance.ghosts.VariableDirectionThreatGhostDistanceBlock;

/**
 * Based on Brandstetter's CIG 2012 paper
 *
 * @author Jacob Schrum
 */
public class CheckEachDirectionMediator extends VariableDirectionBlockLoadedInputOutputMediator {

	public CheckEachDirectionMediator() {
		int irrelevantStartingDirection = -1;
		blocks.add(new BiasBlock());
		// Distances
		blocks.add(new VariableDirectionPillDistanceBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionPowerPillDistanceBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionEdibleGhostDistanceBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionThreatGhostDistanceBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionJunctionDistanceBlock(irrelevantStartingDirection));
		// Obstructions
		blocks.add(new VariableDirectionThreatGhostBlocksJunctionBlock());
		blocks.add(new VariableDirectionPowerPillBlocksThreatGhostBlock());
		// Look ahead
		blocks.add(new VariableDirectionKStepPillCountBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionKStepPowerPillCountBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionKStepEdibleGhostCountBlock(irrelevantStartingDirection, true));
		blocks.add(new VariableDirectionKStepThreatGhostCountBlock(irrelevantStartingDirection));
		blocks.add(new VariableDirectionKStepJunctionCountBlock(irrelevantStartingDirection));
	}
}

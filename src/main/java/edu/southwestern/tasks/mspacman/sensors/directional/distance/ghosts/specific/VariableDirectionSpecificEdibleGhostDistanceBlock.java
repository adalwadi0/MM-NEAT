/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.southwestern.tasks.mspacman.sensors.directional.distance.ghosts.specific;

import edu.southwestern.tasks.mspacman.facades.GameFacade;
import edu.southwestern.tasks.mspacman.sensors.directional.distance.VariableDirectionDistanceBlock;

/**
 *
 * @author Jacob Schrum
 */
public class VariableDirectionSpecificEdibleGhostDistanceBlock extends VariableDirectionDistanceBlock {
	private final int ghostIndex;

	public VariableDirectionSpecificEdibleGhostDistanceBlock(int dir, int ghostIndex) {
		super(dir);
		this.ghostIndex = ghostIndex;
	}

	@Override
	public String getType() {
		return "Edible Ghost " + ghostIndex;
	}

	@Override
	public int[] getTargets(GameFacade gf) {
		if (gf.isGhostEdible(ghostIndex)) {
			return new int[] { gf.getGhostCurrentNodeIndex(ghostIndex) };
		} else {
			return new int[0];
		}
	}
}

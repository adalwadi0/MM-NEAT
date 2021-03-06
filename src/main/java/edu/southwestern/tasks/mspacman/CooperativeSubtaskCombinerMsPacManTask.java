package edu.southwestern.tasks.mspacman;

import edu.southwestern.evolution.genotypes.Genotype;
import edu.southwestern.MMNEAT.MMNEAT;
import edu.southwestern.networks.Network;
import edu.southwestern.scores.Score;
import edu.southwestern.tasks.mspacman.init.MsPacManInitialization;
import edu.southwestern.tasks.mspacman.objectives.fitnessassignment.FitnessToModeMap;
import edu.southwestern.util.ClassCreation;
import java.util.ArrayList;

/**
 * Hierarchical approach in which one combiner network population evolves on top
 * of several subnetwork populations. The combiner takes the outputs of the
 * subnets as inputs and then uses its own outputs to determine pacman's
 * actions.
 *
 * @author Jacob Schrum
 * @param <T> phenotype of component agents
 */
public class CooperativeSubtaskCombinerMsPacManTask<T extends Network> extends CooperativeMsPacManTask<T> {

	private final int[] fitnessPreferences;

	public CooperativeSubtaskCombinerMsPacManTask() {
		super();
		FitnessToModeMap fitnessMap = null;
		try {
			fitnessMap = (FitnessToModeMap) ClassCreation.createObject("pacmanFitnessModeMap");
		} catch (NoSuchMethodException ex) {
			System.out.println("FitnessToModeMap failed loading");
			System.exit(1);
		}
		fitnessPreferences = fitnessMap.associatedFitnessScores();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public ArrayList<Score> evaluate(Genotype[] team) {
		Genotype<T> topLevelNetwork = team[0];
		// Put subnets into input-output mediator
		ArrayList<Genotype<T>> subnets = new ArrayList<Genotype<T>>(team.length);
		for (int i = 1; i < team.length; i++) { // skips first net, the combiner
			subnets.add(team[i]);
		}
		MsPacManInitialization.replaceSubnets(subnets);
		// Evaluate
		Score<T> taskScores = task.evaluate(topLevelNetwork);
		// Redistribute scores to each genotype
		ArrayList<Score> genotypeScores = new ArrayList<Score>(team.length);
		// Combiner network gets all the regular fitness scores
		genotypeScores.add(taskScores);
		// Give scores to each sub-genotype
		for (int i = 1; i < team.length; i++) {
			Score<T> s = new Score<T>(subnets.get(i - 1), task.fitnessArray(fitnessPreferences[i - 1], taskScores),
					null, new double[] {});
			genotypeScores.add(s);
		}

		return genotypeScores;
	}

	/**
	 * Combiner net, each subtask net
	 *
	 * @return
	 */
        @Override
	public int numberOfPopulations() {
		return MMNEAT.modesToTrack + 1;
	}

	/**
	 * WARNING! This under estimates the number of fitness values for each
	 * evolving component, but this function is really only used by blueprints,
	 * so maybe this is not a problem? Be careful if using for anything new.
	 *
	 * @return
	 */
        @Override
	public int[] objectivesPerPopulation() {
		int[] result = new int[numberOfPopulations()];
		result[0] = task.objectives.size();
		for (int i = 1; i < result.length; i++) {
			result[i] = 1;
		}
		return result;
	}

        @Override
	public int[] otherStatsPerPopulation() {
		int[] result = new int[numberOfPopulations()];
		result[0] = task.otherScores.size();
		for (int i = 1; i < result.length; i++) {
			result[i] = 0;
		}
		return result;
	}
}

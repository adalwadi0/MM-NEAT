package edu.utexas.cs.nn.evolution.genotypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import edu.utexas.cs.nn.MMNEAT.MMNEAT;
import edu.utexas.cs.nn.networks.NetworkUtil;
import edu.utexas.cs.nn.networks.TWEANN;
import edu.utexas.cs.nn.networks.hyperneat.HyperNEATTask;
import edu.utexas.cs.nn.networks.hyperneat.HyperNEATUtil;
import edu.utexas.cs.nn.networks.hyperneat.Substrate;
import edu.utexas.cs.nn.parameters.CommonConstants;
import edu.utexas.cs.nn.parameters.Parameters;
import edu.utexas.cs.nn.util.datastructures.Pair;
import edu.utexas.cs.nn.util.datastructures.Triple;
import edu.utexas.cs.nn.util.util2D.ILocated2D;
import edu.utexas.cs.nn.util.util2D.Tuple2D;

/**
 * Genotype for a hyperNEAT CPPN network
 *
 * @author Lauren Gillespie
 *
 */
public class HyperNEATCPPNGenotype extends TWEANNGenotype {

	// For each substrate layer pairing, there can be multiple output neurons in the CPPN
	public static int numCPPNOutputsPerLayerPair = 1;
	// Within each group, the first (index 0) will always specify the link value
	public static final int LINK_INDEX = 0;
	// If a Link Expression Output is used, it will be second (index 1)
	public static final int LEO_INDEX = 1;
	public static boolean constructingNetwork = false;
	public static final double BIAS = 1.0;// Necessary for most CPPN networks
	public int innovationID = 0;// provides unique innovation numbers for links and genes

	/**
	 * Default constructor
	 */
	public HyperNEATCPPNGenotype() {
		super();
	}

	/**
	 * Used by TWEANNCrossover
	 * 
	 * @param nodes new node genes
	 * @param links new link genes
	 * @param neuronsPerModule effectively the number of output neurons
	 * @param archetypeIndex archetype to use
	 */
	public HyperNEATCPPNGenotype(ArrayList<NodeGene> nodes, ArrayList<LinkGene> links, int neuronsPerModule, int archetypeIndex) {
		super(nodes, links, neuronsPerModule, false, false, archetypeIndex);
	}	

	/**
	 * Constructor for hyperNEATCPPNGenotype. Uses super constructor from
	 * TWEANNGenotype
	 * 
	 * @param links
	 *            list of links between genes
	 * @param genes
	 *            list of nodes in genotype
	 * @param outputNeurons
	 *            number of output neurons
	 */
	public HyperNEATCPPNGenotype(ArrayList<LinkGene> links, ArrayList<NodeGene> genes, int outputNeurons) {
		super(genes, links, outputNeurons, false, false, 0);
	}

	/**
	 * Constructor for random hyperNEATCPPNGenotype.
	 * 
	 * @param networkInputs
	 *            number of network inputs
	 * @param networkOutputs
	 *            number of network outputs
	 * @param archetypeIndex
	 *            index of genotype in archetype
	 */
	public HyperNEATCPPNGenotype(int networkInputs, int networkOutputs, int archetypeIndex) {
		// Construct new CPPN with random weights
		super(networkInputs, networkOutputs, archetypeIndex); 
	}

	/**
	 * Returns TWEANN representation of the CPPN encoded by this genotype,
	 * NOT a TWEANN that is indirectly-encoded
	 * @return TWEANN representation of the CPPN
	 */
	public TWEANN getCPPN() {
		return super.getPhenotype();
	}

	/**
	 * Uses CPPN to create a TWEANN controller for the domain. This
	 * created TWEANN is unique only to the instance in which it is used. In a
	 * sense, it's a one-and-done network, which explains the lax use of
	 * innovation numbers. The TWEANN that is returned is indirectly-encoded
	 * by the CPPN.
	 *
	 * @return TWEANN generated by CPPN
	 */
	@Override
	public TWEANN getPhenotype() {
		TWEANNGenotype tg = getSubstrateGenotype((HyperNEATTask) MMNEAT.task) ;
		return tg.getPhenotype();//return call to substrate genotype
	}

	/**
	 * Use the CPPN to construct a genotype that encodes the substrate
	 * network, and return that genotype. This genotype can be used to
	 * get the substrate network phenotype, which is actually evaluated
	 * in any given domain.
	 * 
	 * Having the genotype of the substrate genotype allows access to 
	 * network components using methods of the genotype, which are sometimes
	 * more flexible than the methods of the network itself.
	 * 
	 * @param hnt HyperNEAT task that defines a substrate description used here
	 * @return genotype that encodes a substrate network generated by a CPPN
	 */
	public TWEANNGenotype getSubstrateGenotype(HyperNEATTask hnt) {
		constructingNetwork = true; // prevent displaying of substrates
		//long time = System.currentTimeMillis(); // for timing
		TWEANN cppn = getCPPN();// CPPN used to create TWEANN network
		List<Substrate> subs = hnt.getSubstrateInformation();// extract substrate information from domain
		List<Triple<String, String, Boolean>> connections = hnt.getSubstrateConnectivity();// extract substrate connectivity from domain
		ArrayList<NodeGene> newNodes = null;
		ArrayList<LinkGene> newLinks = null;
		innovationID = 0;// reset each time a phenotype is generated
		int phenotypeOutputs = 0;

		newNodes = createSubstrateNodes(hnt, cppn, subs);
		// Will map substrate names to index in subs List
		HashMap<String, Integer> substrateIndexMapping = new HashMap<String, Integer>();
		for (int i = 0; i < subs.size(); i++) {
			substrateIndexMapping.put(subs.get(i).getName(), i);
		}
		
		try {
			// loop through connections and add links, based on contents of subs
			newLinks = createNodeLinks(hnt, cppn, connections, subs, substrateIndexMapping);
		}catch(NullPointerException npe) {
			System.out.println("Error in substrate configutation!");
			System.out.println(subs);
			System.out.println(connections);
			System.exit(1);
		}
		
		// Figure out number of output neurons
		for (Substrate s : subs) {
			if (s.getStype() == Substrate.OUTPUT_SUBSTRATE) {
				phenotypeOutputs += s.getSize().t1 * s.getSize().t2;
			}
		}		
		constructingNetwork = false;

		// the instantiation of the TWEANNgenotype in question

		// Hard coded to have a single neural output module.
		// May need to fix this down the line.
		// An archetype index of -1 is used. Hopefully this won't cause
		// problems, since the archetype is only needed for mutations and crossover.
		TWEANNGenotype tg = new TWEANNGenotype(newNodes,newLinks, phenotypeOutputs, false, false, -1);
		return tg;
	}

	/**
	 * Used by HyperNEAT-seeded tasks. The HyperNEAT genotype is used to create a (generally large)
	 * substrate network, and a genotype that directly encodes this resulting substrate network
	 * is returned, so that it can then be used as a seed/template for evolving a population of
	 * such large networks.
	 * 
	 * @param hnt HyperNEAT task which contains substrate description information
	 * @return Genotype that directly encodes a substrate network
	 */
	public TWEANNGenotype getSubstrateGenotypeForEvolution(HyperNEATTask hnt) {
		TWEANNGenotype tg = getSubstrateGenotype(hnt);
		tg.archetypeIndex = 0;
		return tg;
	}

	/**
	 * Copies given genotype
	 * 
	 * @return Copy of the CPPN genotype
	 */
	@Override
	public Genotype<TWEANN> copy() {
		int[] temp = moduleUsage; // Schrum: Not sure if keeping moduleUsage is appropriate
		ArrayList<LinkGene> linksCopy = new ArrayList<LinkGene>(this.links.size());
		for (LinkGene lg : this.links) {// needed for a deep copy
			linksCopy.add(newLinkGene(lg.sourceInnovation, lg.targetInnovation, lg.weight, lg.innovation, false));
		}

		ArrayList<NodeGene> genes = new ArrayList<NodeGene>(this.nodes.size());
		for (NodeGene ng : this.nodes) {// needed for a deep copy
			genes.add(newNodeGene(ng.ftype, ng.ntype, ng.innovation, false, ng.getBias()));
		}
		HyperNEATCPPNGenotype result = new HyperNEATCPPNGenotype(linksCopy, genes, MMNEAT.networkOutputs);

		// Schrum: Not sure if keeping moduleUsage is appropriate
		moduleUsage = temp;
		result.moduleUsage = new int[temp.length];
		System.arraycopy(this.moduleUsage, 0, result.moduleUsage, 0, moduleUsage.length);
		return result;
	}

	/**
	 * creates an array list containing all the nodes from all the substrates
	 *
	 * @param cppn
	 *             CPPN that produces phenotype network
	 * @param subs
	 *            list of substrates extracted from domain
	 * @return array list of NodeGenes from substrates
	 */
	public ArrayList<NodeGene> createSubstrateNodes(HyperNEATTask hnt, TWEANN cppn, List<Substrate> subs) {
		int biasIndex = HyperNEATUtil.indexFirstBiasOutput(hnt); // first bias index
		ArrayList<NodeGene> newNodes = new ArrayList<NodeGene>();
		// loops through substrate list
		for (Substrate sub: subs) { // for each substrate
			// This loop gets every (x,y) coordinate pair from the substrate.
			for (Pair<Integer,Integer> coord : sub.coordinateList()) {
				int x = coord.t1;
				int y = coord.t2;

				// Substrate types and Neuron types match and use same values
				double bias = 0.0; // default
				// Non-input substrates can have a bias if desired
				if(CommonConstants.evolveHyperNEATBias && sub.getStype() != Substrate.INPUT_SUBSTRATE) {
					// Ask CPPN to generate a bias for each neuron
					bias = cppn.process(hnt.filterCPPNInputs(new double[]{0, 0, x, y, BIAS}))[biasIndex];
				}
				newNodes.add(newNodeGene(CommonConstants.ftype, sub.getStype(), innovationID++, false, bias));
			}
			
			if(CommonConstants.evolveHyperNEATBias && sub.getStype() != Substrate.INPUT_SUBSTRATE) {
				// Each non-input substrate has its own bias output for generating bias values.
				// Move to the next.
				biasIndex++;
			}
		}
		return newNodes;
	}

	/**
	 * creates an array list of links between substrates as dictated by
	 * connections parameter
	 *
	 * @param cppn
	 *            used to evolve link weight
	 * @param connections
	 *            list of different connections between substrates
	 * @param subs
	 *            list of substrates in question
	 * @param sIMap
	 *            hashmap that maps the substrate in question to its index in
	 *            the substrate list
	 *
	 * @return array list containing all the links between substrates
	 */
	private ArrayList<LinkGene> createNodeLinks(HyperNEATTask hnt, TWEANN cppn, List<Triple<String, String, Boolean>> connections, List<Substrate> subs, HashMap<String, Integer> sIMap) {
		ArrayList<LinkGene> result = new ArrayList<LinkGene>();
		for (int i = 0; i < connections.size(); i++) { // For each pair of substrates that are connected
			int sourceSubstrateIndex = sIMap.get(connections.get(i).t1);
			int targetSubstrateIndex = sIMap.get(connections.get(i).t2);
			Substrate sourceSubstrate = subs.get(sourceSubstrateIndex);
			Substrate targetSubstrate = subs.get(targetSubstrateIndex);
			
			// Whether to connect these layers used convolutional structure instead of standard fully connected structure
			boolean convolution = connections.get(i).t3 && CommonConstants.convolution;
			// both options add links from between two substrates to whole list of links
			if(convolution) {
				convolutionalLoopThroughLinks(hnt, result, cppn, i, sourceSubstrate, targetSubstrate, sourceSubstrateIndex, targetSubstrateIndex, subs);
			} else {
				loopThroughLinks(hnt, result, cppn, i, sourceSubstrate, targetSubstrate, sourceSubstrateIndex, targetSubstrateIndex, subs);
			}
		}
		return result;
	}

	/**
	 * Connect two substrate layers using convolutional link structures
	 * @param hnt HyperNEATTask instance with
	 * @param linksSoFar List of link genes to add to
	 * @param cppn Network generating link weights
	 * @param outputIndex index from cppn outputs to be used as weight in creating link
	 * @param s1 Where links come from
	 * @param s2 Where links go to
	 * @param s1Index Index in substrate list of source substrate
	 * @param s2Index Index in substrate list of target substrate
	 * @param subs List of substrates
	 */
	void convolutionalLoopThroughLinks(HyperNEATTask hnt, ArrayList<LinkGene> linksSoFar, TWEANN cppn, int outputIndex,
			Substrate s1, Substrate s2, int s1Index, int s2Index,
			List<Substrate> subs) {
		
		int receptiveFieldSize = Parameters.parameters.integerParameter("receptiveFieldSize");
		assert receptiveFieldSize % 2 == 1 : "Receptive field size needs to be odd to be centered: " + receptiveFieldSize;
		int edgeOffset = receptiveFieldSize / 2; // might allow zero-padding around edge later
		
		int stride = Parameters.parameters.integerParameter("stride");
		
		// Traverse center points of receptive fields
		for(int x = edgeOffset; x < s1.getSize().t1 - edgeOffset; x += stride) {
			for(int y = edgeOffset; y < s1.getSize().t2 - edgeOffset; y += stride) {
				// There is a direct correspondence between each receptive field and
				// its target neuron in the next layer
				int targetXindex = (x - edgeOffset) / stride; 
				int targetYIndex = (y - edgeOffset) / stride;
				// If target neuron is dead, do not continue
				if(!s2.isNeuronDead(targetXindex, targetYIndex)) {
					// Loop through all neurons in the receptive field
					for(int fX = -edgeOffset; fX <= edgeOffset; fX++) {
						for(int fY = -edgeOffset; fY <= edgeOffset; fY++) {
							// Source neuron is offset from receptive field center
							int fromXIndex = x + fX;
							int fromYIndex = y + fY;
							// Do not continue if source neuron is dead
							if(!s1.isNeuronDead(fromXIndex, fromYIndex)) {
								// CPPN inputs need to be centered and scaled
								ILocated2D scaledFieldCoordinates = MMNEAT.substrateMapping.transformCoordinates(new Tuple2D(fX+edgeOffset, fY+edgeOffset), receptiveFieldSize, receptiveFieldSize);
								ILocated2D scaledTargetCoordinates = MMNEAT.substrateMapping.transformCoordinates(new Tuple2D(targetXindex, targetYIndex), s2.getSize().t1, s2.getSize().t2);
								// inputs to CPPN 
								// NOTE: filterCPPNInputs call was removed because it doesn't seem to make sense with convolutional inputs
								double[] inputs = new double[]{scaledFieldCoordinates.getX(), scaledFieldCoordinates.getY(), scaledTargetCoordinates.getX(), scaledTargetCoordinates.getY(), BIAS};
								conditionalLinkAdd(linksSoFar, cppn, inputs, outputIndex, fromXIndex, fromYIndex, s1Index, targetXindex, targetYIndex, s2Index, subs);
							}							
						}						
					}
				}
			}		
		}
	}

	/**
	 * a method for looping through all nodes of two substrates to be linked
	 * Link is only created if CPPN output reaches a certain threshold that is
	 * dictated via command line parameter.
	 *
	 * @param linksSoFar
	 * 			  All added links are accumulated in this list
	 * @param cppn
	 *            used to evolve link weight
	 * @param outputIndex
	 *            index from cppn outputs to be used as weight in creating link
	 * @param s1
	 *            first substrate to be linked
	 * @param s2
	 *            second substrate to be linked
	 * @param s1Index
	 *            index of first substrate in substrate list
	 * @param s2Index
	 *            index of second substrate in substrate list
	 * @param subs
	 *            list of substrates
	 *
	 */
	void loopThroughLinks(HyperNEATTask hnt, ArrayList<LinkGene> linksSoFar, TWEANN cppn, int outputIndex, Substrate s1, Substrate s2, int s1Index, int s2Index, List<Substrate> subs) {

		// This loop goes through every (x,y) coordinate in Substrate s1: source substrate
		for(Pair<Integer,Integer> src : s1.coordinateList()) {
			int fromXIndex = src.t1;
			int fromYIndex = src.t2;
			// If the neuron in the source substrate is dead, it will not have outputs
			if(!s1.isNeuronDead(fromXIndex, fromYIndex)) {
				// This loop searches through every (x,y) coordinate in Substrate s2: target substrate
				for(Pair<Integer,Integer> target: s2.coordinateList()) {
					int targetXindex = target.t1;
					int targetYIndex = target.t2;
					// If the target neuron is dead, then don't bother with incoming links
					if(!s2.isNeuronDead(targetXindex, targetYIndex)) {
						// CPPN inputs need to be centered and scaled
						ILocated2D scaledSourceCoordinates = MMNEAT.substrateMapping.transformCoordinates(new Tuple2D(fromXIndex, fromYIndex), s1.getSize().t1, s1.getSize().t2);
						ILocated2D scaledTargetCoordinates = MMNEAT.substrateMapping.transformCoordinates(new Tuple2D(targetXindex, targetYIndex), s2.getSize().t1, s2.getSize().t2);
						// inputs to CPPN 
						// These next two lines need to be generalized for different numbers of CPPN inputs
						double[] inputs = hnt.filterCPPNInputs(new double[]{scaledSourceCoordinates.getX(), scaledSourceCoordinates.getY(), scaledTargetCoordinates.getX(), scaledTargetCoordinates.getY(), BIAS});
						conditionalLinkAdd(linksSoFar, cppn, inputs, outputIndex, fromXIndex, fromYIndex, s1Index, targetXindex, targetYIndex, s2Index, subs);
					}
				}
			}
		}
	}
	
	/**
	 * If the given inputs to the CPPN indicate that a link should be added, then it is added to the provided list of links with the
	 * appropriate weight.
	 * 
	 * @param linksSoFar List of links to add to
	 * @param cppn Network generating link weights
	 * @param inputs inputs to the CPPN
	 * @param outputIndex index within CPPN outputs to look for weight information
	 * @param fromXIndex x-coordinate of neuron in source substrate
	 * @param fromYIndex y-coordinate of neuron in source substrate
	 * @param s1Index source substrate index in substrate list
	 * @param targetXindex x-coordinate of neuron in target substrate
	 * @param targetYIndex y-coordinate of neuron in target substrate
	 * @param s2Index target substrate index in substrate list
	 * @param subs list of substrates
	 */
	void conditionalLinkAdd(ArrayList<LinkGene> linksSoFar, TWEANN cppn, double[] inputs, int outputIndex, int fromXIndex, int fromYIndex, int s1Index, int targetXindex, int targetYIndex, int s2Index, List<Substrate> subs) {
		double[] outputs = cppn.process(inputs);
		boolean expressLink = CommonConstants.leo
				// Specific network output determines link expression
				? outputs[(numCPPNOutputsPerLayerPair * outputIndex) + LEO_INDEX] > CommonConstants.linkExpressionThreshold
						// Output magnitude determines link expression
						: Math.abs(outputs[(numCPPNOutputsPerLayerPair * outputIndex) + LINK_INDEX]) > CommonConstants.linkExpressionThreshold;
						if (expressLink) {
							long sourceID = getInnovationID(fromXIndex, fromYIndex, s1Index, subs);
							long targetID = getInnovationID(targetXindex, targetYIndex, s2Index, subs);
							double weight = CommonConstants.leo
									// LEO takes its weight directly from the designated network output
									? outputs[(numCPPNOutputsPerLayerPair * outputIndex) + LINK_INDEX]
											// Standard HyperNEAT must scale the weight
											: NetworkUtil.calculateWeight(outputs[(numCPPNOutputsPerLayerPair * outputIndex) + LINK_INDEX]);
									linksSoFar.add(newLinkGene(sourceID, targetID, weight, innovationID++, false));
						}
	}

	/**
	 * returns the innovation id of the node in question
	 *
	 * @param x
	 *            x-coordinate of node
	 * @param y
	 *            y-coordinate of node
	 * @param sIndex
	 *            index of substrate in question
	 * @param subs
	 *            list of substrates available
	 *
	 * @return innovationID of link in question
	 */
	public long getInnovationID(int x, int y, int sIndex, List<Substrate> subs) {
		long innovationIDAccumulator = 0;
		for (int i = 0; i < sIndex; i++) {
			Substrate s = subs.get(i);
			innovationIDAccumulator += s.getSize().t1 * s.getSize().t2;
		}
		innovationIDAccumulator += (subs.get(sIndex).getSize().t1 * y) + x;
		return innovationIDAccumulator;
	}

	/**
	 * Creates a new random instance of the hyperNEATCPPNGenotype
	 * @return New genotype for CPPN
	 */
	@Override
	public Genotype<TWEANN> newInstance() {
		HyperNEATCPPNGenotype result = new HyperNEATCPPNGenotype(MMNEAT.networkInputs, MMNEAT.networkOutputs, this.archetypeIndex);
		result.moduleUsage = new int[result.numModules];
		return result;
	}

}

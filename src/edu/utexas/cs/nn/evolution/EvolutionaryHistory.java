package edu.utexas.cs.nn.evolution;

import edu.utexas.cs.nn.evolution.crossover.network.CombiningTWEANNCrossover;
import edu.utexas.cs.nn.evolution.genotypes.Genotype;
import edu.utexas.cs.nn.evolution.genotypes.TWEANNGenotype;
import edu.utexas.cs.nn.evolution.genotypes.TWEANNGenotype.NodeGene;
import edu.utexas.cs.nn.log.MONELog;
import edu.utexas.cs.nn.log.TWEANNLog;
import edu.utexas.cs.nn.MMNEAT.MMNEAT;
import edu.utexas.cs.nn.networks.Network;
import edu.utexas.cs.nn.networks.TWEANN;
import edu.utexas.cs.nn.parameters.CommonConstants;
import edu.utexas.cs.nn.parameters.Parameters;
import edu.utexas.cs.nn.util.file.FileUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import wox.serial.Easy;

/**
 *Stores and tracks information about a population of networks. Uses an archetype that stores
 *all genotypes used in population. Also logs history of TWEANN, mutations and lineages of each network
 *
 * @author Jacob Schrum
 */
public class EvolutionaryHistory {

    public static int maxModes;
    public static int minModes;

    //keeps track of which id to use next
    public static long largestUnusedInnovationNumber = 0;
    public static long largestUnusedGenotypeId = 0;
    //keeps track of archetype of every genotype from each generation of every member
    //of population in an array list
    public static ArrayList<NodeGene>[] archetypes = null;
    public static int[] archetypeOut = null;
    //logs that keep track of history of genotype
    public static TWEANNLog tweannLog = null;
    public static MONELog mutationLog = null;
    public static MONELog lineageLog = null;

    /**
     * Commonly used/shared networks (hierarchical architectures). Raw types are
     * allowed for greater flexibility (different types of phenotypes may be
     * generated)
     */
    @SuppressWarnings("rawtypes")
    public static HashMap<String, Genotype> loadedNetworks = new HashMap<String, Genotype>();

    /**
     * Assure that each repeatedly used subnetwork is only loaded once
     *
     * @param <T> Phenotype that the genotype encodes
     * @param xml File path of xml genotype file
     * @return the decoded genotype instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends Network> Genotype<T> getSubnetwork(String xml) {
        if (xml.isEmpty()) {
            // Return a dummy genotype to be ignored later
            return null;
        }
        //makes sure it's not a commonly shared architecture already found in loadedNetworks hashmap
        if (!loadedNetworks.containsKey(xml)) {
            System.out.println("Added to subnetworks: " + xml);
            loadedNetworks.put(xml, (Genotype<T>) Easy.load(xml));
        }
        return loadedNetworks.get(xml).copy();
    }

    /**
     * Sets up tracker for previously used innovation numbers.
     */
    public static void initInnovationHistory() {
        setInnovation(Parameters.parameters.longParameter("lastInnovation"));
    }

    /**
     * Sets up tracker for previously used genotype IDs.
     */
    public static void initGenotypeIds() {
        setHighestGenotypeId(Parameters.parameters.longParameter("lastGenotypeId") - 1);
    }

    /**
     * Assign new innovation number ID
     *
     * @param innovation Should be the larger than all previously used
     * innovation numbers
     */
    public static void setInnovation(long innovation) {
        largestUnusedInnovationNumber = innovation;
    }

    /**
     * Assign new genotype ID tracker
     *
     * @param id Should be the larger than all previously used genotype IDs
     */
    public static void setHighestGenotypeId(long id) {
        largestUnusedGenotypeId = id;
    }

    /**
     * Returns the next innovation number and increases the counter
     *
     * @return next innovation number
     */
    public static long nextInnovation() {
        long result = largestUnusedInnovationNumber;
        largestUnusedInnovationNumber++;
        Parameters.parameters.setLong("lastInnovation", largestUnusedInnovationNumber);
        return result;
    }

    /**
     * Returns the next GenotypeID and increases the counter
     * 
     * @return next Genotype ID
     */
    public static long nextGenotypeId() {
        long result = largestUnusedGenotypeId;
        largestUnusedGenotypeId++;
        Parameters.parameters.setLong("lastGenotypeId", largestUnusedGenotypeId);
        return result;
    }

    /**
     * Checks for a pre-existing file that is a genotype archetype for all
     * genotypes in the population. This file assures that crossover aligns
     * genotypes correctly when the genotypes being crossed have nodes that are
     * not present in the other parent.
     *
     * @param populationIndex Unused: Supposed to allow for multiple coevolved populations.
     * Corresponds to index of population in question. Since coevolution has not been 
     * implemented yet, unused.
     * 
     * @return true if file exists, false if doesn't
     */
    public static boolean archetypeFileExists(int populationIndex) {
        String file = FileUtilities.getSaveDirectory() + "/" + "archetype";
        return (new File(file)).exists();
    }

    /**
     * Public default method that initializes an archetype file that stores the 
     * archetype of the network in question. Includes some checks for whether or
     * not network exists.
     * 
     * @param populationIndex  Supposed to allow for multiple coevolved populations.
     * Corresponds to index of population in question. Since coevolution has not been 
     * implemented yet, unused.
     */
    public static void initArchetype(int populationIndex) {
        String base = Parameters.parameters.stringParameter("base");
        String xml = Parameters.parameters.stringParameter("archetype");
        String file = xml + populationIndex + ".xml";
        if (base.equals("")
                || !(new File(file).exists())) {
            file = null;
        }
        initArchetype(populationIndex, file);
    }

    /**
     * A method that allows the current archetype to be saved if a resume is not occuring
     * 
     * @param populationIndex Supposed to allow for multiple coevolved populations.
     * Corresponds to index of population in question. Since coevolution has not been 
     * implemented yet, unused.
     * 
     * @param loadedArchetype information needed to create the archetype file stored in 
     * a string 
     */
    @SuppressWarnings("unchecked")
    public static void initArchetype(int populationIndex, String loadedArchetype) {
        int size = MMNEAT.genotypeExamples == null ? 1 : MMNEAT.genotypeExamples.size();
        if (archetypes == null) {//checks to see if an archetype has been created yet for this genotype
            archetypes = new ArrayList[size];
        }//this if statement happens if the current experiment hasn't yet been run or is a resume
        if (loadedArchetype == null || loadedArchetype.equals("") || !(new File(loadedArchetype).exists())) {
            TWEANNGenotype tg = (TWEANNGenotype) (MMNEAT.genotypeExamples == null ? MMNEAT.genotype.copy()
            : MMNEAT.genotypeExamples.get(populationIndex).copy());//ternary operator allows for coevolution to be implemented
            archetypes[populationIndex] = tg.nodes;//saves the genotype of the current generation
            saveArchetype(populationIndex);
        } else {
        	//thise else statement runs if a next run with a new seed
            // The loaded archetype might not simply be from a resume, the seed could be from elsewhere
            System.out.println("Loading archetype: " + loadedArchetype);
            archetypes[populationIndex] = (ArrayList<NodeGene>) Easy.load(loadedArchetype);
            String combiningCrossoverFile = Parameters.parameters.stringParameter("combiningCrossoverMapping");
            if (!combiningCrossoverFile.isEmpty()) {//implement for multimodal behavior. Allows for combining of two 
            	//separate subpopulations to create a multimodal network
                combiningCrossoverFile += ".txt";
                System.out.println("Loading combining crossover file: " + combiningCrossoverFile);
                CombiningTWEANNCrossover.loadOldToNew(combiningCrossoverFile);
            }
            long highestInnovation = -1;//finds largest innovation number in network to check largestUnusedInnovationNumber is set right
            for (NodeGene ng : archetypes[populationIndex]) {
                highestInnovation = Math.max(highestInnovation, ng.innovation);
            }
            if (highestInnovation > largestUnusedInnovationNumber) {//checks to make sure largestUnusedInnovationNumber is set 
            	//to the highest innovation number
                setInnovation(highestInnovation + 1);
            }
            String xml = Parameters.parameters.stringParameter("archetype");
            String file = xml + populationIndex + ".xml";
            // Compare the loaded name with the name to save at. If different, 
            // then the load was not a resume, and the archetype needs to be saved
            if (!loadedArchetype.equals(file)) {
                saveArchetype(populationIndex);
            }
        }
        if (archetypeOut == null) {
            archetypeOut = new int[size];
        }
        //saves the archetype for the output neurons 
        archetypeOut[populationIndex] = 0;
        for (NodeGene ng : archetypes[populationIndex]) {
            if (ng.ntype == TWEANN.Node.NTYPE_OUTPUT) {
                archetypeOut[populationIndex]++;
            }
        }
    }

    /**
     * A method for saving the archetype of a network to its respective files
     * 
     * @param populationIndex Supposed to allow for multiple coevolved populations.
     * Corresponds to index of population in question. Since coevolution has not been 
     * implemented yet, unused.
     */
    public static void saveArchetype(int populationIndex) {
        if (archetypes != null && archetypes[populationIndex] != null && CommonConstants.netio) {
            System.out.println("Saving archetype");
            String file = FileUtilities.getSaveDirectory() + "/" + "archetype";
            Parameters.parameters.setString("archetype", file);
            file += populationIndex + ".xml";
            Easy.save(archetypes[populationIndex], file);
            System.out.println("Done saving " + file);
            if (!CombiningTWEANNCrossover.oldToNew.isEmpty()) {//means crossover has occured before
                System.out.println("Saving Combining Crossover Mapping");
                file = FileUtilities.getSaveDirectory() + "/" + "combiningCrossoverMapping";//adds new crossover to crossover file
                Parameters.parameters.setString("combiningCrossoverMapping", file);
                file += ".txt";
                CombiningTWEANNCrossover.saveOldToNew(file);//saves new crossover
            }
        }
    }

    /**
     * Initializes the mutation and lineage logs of an archetype
     */
    public static void initLineageAndMutationLogs() {
        mutationLog = new MONELog("Mutations", true);
        lineageLog = new MONELog("Lineage", true);
    }

    /**
     * Initializes the innovation number history and initializes the TWEANN log
     */
    public static void initTWEANNLog() {
        initInnovationHistory();
        if (tweannLog == null) {
            tweannLog = new TWEANNLog("TWEANNData");
        }
    }

    /**
     * logs data about the tweann population and generation to tweann log
     * 
     * @param population array list that contains all the genotypes of the given population
     * 
     * @param generation the number of generations performed so far
     */
    public static void logTWEANNData(ArrayList<TWEANNGenotype> population, int generation) {
        if (tweannLog != null) {
            tweannLog.log(population, generation);
        }
    }

    /**
     * logs mutation data to mutationLog
     * 
     * @param data to be added to mutationLog
     */
    public static void logMutationData(String data) {
        if (mutationLog != null) {
            mutationLog.log(data);
        }
    }

    /**
     * logs lineage data about a network to lineageLog
     * 
     * @param data to be added to lineage log
     */
    public static void logLineageData(String data) {
        if (lineageLog != null) {
            lineageLog.log(data);
        }
    }

    /**
     * 
     * @param populationIndex index of population in question
     * @param sourceInnovation innovation number in question
     * 
     * @return the index of the innovation number from the archetypes array
     */
    public static int indexOfArchetypeInnovation(int populationIndex, long sourceInnovation) {
        if (archetypes[populationIndex] != null) {
            for (int i = 0; i < archetypes[populationIndex].size(); i++) {
                if (archetypes[populationIndex].get(i).innovation == sourceInnovation) {
                    return i;
                }
            }
        }
        return -1;//returns if innovation number not found
    }

    /**
     * Removes from the archetype all nodes that are not part of the given
     * network in the population
     *
     * @param populationIndex Archetype index of network to compare for cleaning
     * @param population population of TWEANNGenotypes
     * @param generation number of generations passed, used to tell if an archetype cleaning is
     * needed
     */
    public static void cleanArchetype(int populationIndex, ArrayList<TWEANNGenotype> population, int generation) {
        int freq = Parameters.parameters.integerParameter("cleanFrequency");//command line parameter that tells how often archetype needs to be cleaned
        if (archetypes[populationIndex] != null && generation % freq == 0) {
            System.out.println("Cleaning archetype");
            HashSet<Long> activeNodeInnovations = new HashSet<Long>();
            // Get all node innovation numbers still in use by population
            for (TWEANNGenotype tg : population) {
                for (NodeGene ng : tg.nodes) {
                    activeNodeInnovations.add(ng.innovation);
                }
            }
            // Remove from archetype each innovation number no longer active in population
            Iterator<NodeGene> itr = archetypes[populationIndex].iterator();
            archetypeOut[populationIndex] = 0;
            while (itr.hasNext()) {
                NodeGene currentGene = itr.next();
                if (!activeNodeInnovations.contains(currentGene.innovation)) {
                    // If combining crossover maps to this innovation, it can only be removed
                    // if the node that maps to it is also absent.
                    if (CombiningTWEANNCrossover.oldToNew.containsValue(currentGene.innovation)) {
                        Iterator<Long> onItr = CombiningTWEANNCrossover.oldToNew.keySet().iterator();
                        long source = -1;
                        while (onItr.hasNext()) {
                            long next = onItr.next();
                            if (CombiningTWEANNCrossover.oldToNew.get(next) == currentGene.innovation) {
                                source = next;
                                break; // should only be one such entry
                            }
                        }
                        assert (source != -1) :
                                "How can source == -1 if the mapping contains the value?\n"
                                + currentGene.innovation + "\n"
                                + CombiningTWEANNCrossover.oldToNew;
                        // source is the old innovation that maps to the combined innovation.
                        // if source is still present, then what it maps to cannot be removed.
                        int indexOfSource = indexOfArchetypeInnovation(populationIndex, source);
                        // But if indexOfSource is -1, means source is no longer present and
                        //then currentGene should be removed
                        if (indexOfSource == -1) {
                            itr.remove();
                            // Remove the mapping to source too
                            CombiningTWEANNCrossover.oldToNew.remove(source);
                        }
                        //removes key if value (innovation #) no longer present
                    } else if (CombiningTWEANNCrossover.oldToNew.containsKey(currentGene.innovation)) {
                        CombiningTWEANNCrossover.oldToNew.remove(currentGene.innovation);
                        itr.remove();
                    } else {
                        // In the simple case, just remove the inactive node
                        itr.remove();
                    }
                    //if reaches this else if statement, current gene is active
                } else if (currentGene.ntype == TWEANN.Node.NTYPE_OUTPUT) {
                    archetypeOut[populationIndex]++;
                }
            }
        }
    }

    /**
     * Adds a new innovation number to archetype
     * 
     * @param populationIndex Supposed to allow for multiple coevolved populations.
     * Corresponds to index of population in question. Since coevolution has not been 
     * implemented yet, unused.
     * @param node new genotype to be added to archetype
     * 
     * @param origin indicates from where in the code the node came from, for debugging purposes only
     */
    public static void archetypeAdd(int populationIndex, NodeGene node, String origin) {
        // Make sure that the archetype exists, and does not already contain the innovation number
        if (archetypes != null && archetypes[populationIndex] != null && indexOfArchetypeInnovation(populationIndex, node.innovation) == -1) {
        	//adds the new innovation number
            archetypes[populationIndex].add(node);
            if (node.ntype == TWEANN.Node.NTYPE_OUTPUT) {
                archetypeOut[populationIndex]++;
            }
        }
    }

    /**
     * Adds a new node to archetype. Also checks if the node is a crossover of other nodes and handles
     * each crossover case specifically
     *  
     * @param populationIndex index of population
     * @param pos index of where node is to be added in archetype, corresponds to innovation number
     * @param node genotype to be added to archetype
     * @param combineCopy boolean value expressing whether or not this node is a combination of other nodes
     * @param origin indicates from where in the code the node came from, for debugging purposes only
     */
    public static void archetypeAdd(int populationIndex, int pos, NodeGene node, boolean combineCopy, String origin) {
        if (archetypes != null && archetypes[populationIndex] != null) {
            //node.origin = origin + " (" + (order++) + ")";
            //System.out.println("Archetype " + populationIndex + " Add "+pos+": " + node.innovation + ":" + node);
            archetypes[populationIndex].add(pos, node);
            //this if statement only runs if the node to be added is combined from other nodes
            if (combineCopy) {
                NodeGene previous = archetypes[populationIndex].get(pos - 1);
                switch (previous.ntype) {
                    case TWEANN.Node.NTYPE_INPUT://checks if input node
                        int firstCombined = indexOfFirstArchetypeNodeFromCombiningCrossover(populationIndex, TWEANN.Node.NTYPE_HIDDEN);
                        if (firstCombined == -1) { // no combining crossover yet
                            archetypeAddFromCombiningCrossover(populationIndex, node, archetypes[populationIndex].size() - archetypeOut[populationIndex], "combine splice (FIRST)");
                        } else {
                            archetypeAddFromCombiningCrossover(populationIndex, node, firstCombined, "combine splice (before others)");
                        }   break;
                    case TWEANN.Node.NTYPE_HIDDEN://checks if hidden node
                        long previousInnovation = previous.innovation;
                        if (CombiningTWEANNCrossover.oldToNew.containsKey(previousInnovation)) {
                            long newPreviousInnovation = CombiningTWEANNCrossover.oldToNew.get(previousInnovation);
                            int indexNewPrevious = indexOfArchetypeInnovation(populationIndex, newPreviousInnovation);
                            // Add new node directly after, in anticipation of future combining crossover
                            archetypeAddFromCombiningCrossover(populationIndex, node, indexNewPrevious + 1, "combine splice (hidden)");
                        }
                        // otherwise do nothing, since node is only being spliced in multitask networks
                        break;
                    default:
                        System.out.println("Error! " + previous + "," + pos + "," + archetypes[populationIndex]);
                        System.exit(1);
                }
            }
        }
        assert orderedArchetype(populationIndex) : "Archetype " + populationIndex + " did not exhibit proper node order after node addition: " + archetypes[populationIndex];
    }

    /**
     * A helper method for archetypeAdd method. Addresses cases where a node that is a combination of other nodes is trying to be added
     * 
     * @param populationIndex index of population in question
     * @param node node to be added
     * @param pos index of where node is to be added in archetype, corresponds to innovation number 
     * @param origin indicates from where in the code the node came from, for debugging purposes only
     */
    public static void archetypeAddFromCombiningCrossover(int populationIndex, NodeGene node, int pos, String origin) {
        NodeGene newNodeGene = node.clone();//adds a clone of node to add so original node is not affected if add is unsuccessful
        long oldInnovation = newNodeGene.innovation;
        newNodeGene.innovation = CombiningTWEANNCrossover.getAdjustedInnovationNumber(oldInnovation); // Change innovation to prevent weird overlaps
        //newNodeGene.origin = origin + " copied "+oldInnovation+" (" + (order++) + ")";
        newNodeGene.fromCombiningCrossover = true;//indicates addition was successful
        archetypes[populationIndex].add(pos, newNodeGene);
    }

    /**
     * Helper method for archetypeAdd method.
     * Gets the index of first node from archetype used in combined node
     * 
     * @param populationIndex index of population in question
     * @param ntype the type of node of the first node from crossover
     * 
     * @return index of first node in archetype from combined node to add
     */
    public static int indexOfFirstArchetypeNodeFromCombiningCrossover(int populationIndex, int ntype) {
        for (int i = 0; i < archetypes[populationIndex].size(); i++) {
            if (archetypes[populationIndex].get(i).fromCombiningCrossover && archetypes[populationIndex].get(i).ntype == ntype) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return index of first output node in archetype
     *
     * @param archetypeIndex
     * 
     * @return index of first output node in archetype
     */
    public static int firstArchetypeOutputIndex(int archetypeIndex) {
        int result = archetypeSize(archetypeIndex) - archetypeOut[archetypeIndex];
        assert archetypes[archetypeIndex].get(result).ntype == TWEANN.Node.NTYPE_OUTPUT : "First output is not an output! pos " + result + " in " + archetypes[archetypeIndex];
        return result;
        /**
         * code for testing purposes
         */
        // Assert below indicated that the above expression is indeed equivalent to searching for the node in
        // question. This would only fail if something else were also causing a problem.
//        for (int i = 0; i < archetypes[archetypeIndex].size(); i++) {
//            if (archetypes[archetypeIndex].get(i).ntype == TWEANN.Node.NTYPE_OUTPUT) {
//                //System.out.println("archetype output at " + i + " out of " + archetypes[archetypeIndex].size());
//                assert i == archetypeSize(archetypeIndex) - archetypeOut[archetypeIndex]: "First output found at " + i + " not consistent with archetype size " + archetypeSize(archetypeIndex) + " and num outputs " + archetypeOut[archetypeIndex] + ":" + archetypeOut[archetypeIndex];
//                return i;
//            }
//        }
//        return -1;
    }

    /**
     * Getter method for size of the archetype
     * 
     * @param populationIndex index of population in question
     * 
     * @return size of archetype
     */
    public static int archetypeSize(int populationIndex) {
        return archetypes[populationIndex] == null ? 0 : archetypes[populationIndex].size();
    }

    /**
     * This complicated method isn't really used. The point was to allow
     * evolving network genotypes with preference neurons to alternate between
     * stages where the policy was evolving and stages where the preference
     * neuron behavior was evolving.
     *
     * The code only runs with TWEANNGenotypes (instanceof), but gets called
     * with Genotypes of all sorts, which is why the type is raw.
     *
     * @param population A population of TWEANNGenotypes
     * @param generation index of generation neurons may or may not have been frozen at
     */
    @SuppressWarnings("rawtypes")
    public static void frozenPreferenceVsPolicyStatusUpdate(ArrayList<? extends Genotype> population, int generation) {
        if ((population.get(0) instanceof TWEANNGenotype)
                && Parameters.parameters.booleanParameter("alternatePreferenceAndPolicy")
                && (generation % Parameters.parameters.integerParameter("freezeMeltAlternateFrequency")) == 0) {
            boolean result = false;
            for (Genotype tg : population) {
                result = ((TWEANNGenotype) tg).alternateFrozenPreferencePolicy();
            }
            System.out.println((result ? "Policy" : "Preference") + " neurons were frozen at gen " + generation);
        }
    }

    /**
     * Diagnostic method used in assertion: Makes sure nodes are properly
     * ordered in archetype, i.e. inputs, then hidden, then output
     *
     * @param populationIndex
     * 
     * @return Whether archetype is ordered.
     */
    private static boolean orderedArchetype(int populationIndex) {
        int sectionType = TWEANN.Node.NTYPE_INPUT;
        for (int i = 0; i < archetypes[populationIndex].size(); i++) {
            NodeGene node = archetypes[populationIndex].get(i);
            if (node.ntype != sectionType) {
                switch (sectionType) {
                    case TWEANN.Node.NTYPE_INPUT:
                        if (node.ntype == TWEANN.Node.NTYPE_HIDDEN || node.ntype == TWEANN.Node.NTYPE_OUTPUT) {
                            // Expected progress
                            sectionType = node.ntype;
                        } else {
                            System.out.println("How does " + node.ntype + " follow " + sectionType + "?");
                            return false;
                        }
                        break;
                    case TWEANN.Node.NTYPE_HIDDEN:
                        if (node.ntype == TWEANN.Node.NTYPE_OUTPUT) {
                            // Expected progress
                            sectionType = node.ntype;
                        } else {
                            System.out.println("How does " + node.ntype + " follow " + sectionType + "?");
                            return false;
                        }
                        break;
                    case TWEANN.Node.NTYPE_OUTPUT:
                        System.out.println("How does " + node.ntype + " follow " + sectionType + "?");
                        return false;
                }
            }
        }
        return true;
    }
}

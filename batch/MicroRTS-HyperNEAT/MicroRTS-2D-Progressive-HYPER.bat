cd ..
cd ..
java -jar dist/MM-NEATv2.jar runNumber:%1 randomSeed:%1 base:microRTS trials:3 maxGens:500 mu:10 io:true netio:true mating:true task:edu.utexas.cs.nn.tasks.microrts.MicroRTSTask cleanOldNetworks:true fs:false log:MicroRTS-2DProgressiveHYPER saveTo:2DProgressiveHYPER watch:false microRTSEvaluationFunction:edu.utexas.cs.nn.tasks.microrts.evaluation.NN2DEvaluationFunction microRTSFitnessFunction:edu.utexas.cs.nn.tasks.microrts.fitness.ProgressiveFitnessFunction hyperNEAT:true genotype:edu.utexas.cs.nn.evolution.genotypes.HyperNEATCPPNGenotype allowMultipleFunctions:true ftype:1 netChangeActivationRate:0.3
cd ..
cd ..
java -jar dist/MM-NEATv2.jar runNumber:%1 randomSeed:%1 base:othello trials:5 maxGens:200 mu:50 io:true netio:true mating:true task:edu.utexas.cs.nn.tasks.boardGame.StaticOpponentBoardGameTask cleanOldNetworks:true fs:false log:Othello-HNStaticWPCDeterministicD2Random2Moves saveTo:HNStaticWPCDeterministicD2Random2Moves boardGame:boardGame.othello.Othello boardGameOpponent:boardGame.agents.treesearch.BoardGamePlayerMinimaxAlphaBetaPruning boardGameOpponentHeuristic:boardGame.heuristics.StaticOthelloWPCHeuristic boardGamePlayer:boardGame.agents.treesearch.BoardGamePlayerMinimaxAlphaBetaPruning minimaxSearchDepth:2 genotype:edu.utexas.cs.nn.evolution.genotypes.HyperNEATCPPNGenotype hyperNEAT:true minimaxRandomRate:0.0 boardGameOthelloFitness:true boardGameSimpleFitness:false randomArgMaxTieBreak:false boardGameOpeningRandomMoves:2
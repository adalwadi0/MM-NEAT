java -jar dist/MONE.jar runNumber:0 randomSeed:0 base:isolatedpm maxGens:200 mu:50 io:true netio:true mating:true task:edu.utexas.cs.nn.tasks.mspacman.MsPacManGhostsVsPillsMultitask highLevel:true infiniteEdibleTime:false imprisonedWhileEdible:false pacManLevelTimeLimit:8000 pacmanInputOutputMediator:edu.utexas.cs.nn.tasks.mspacman.sensors.mediators.IICheckEachDirectionMediator trials:10 log:IsolatedPM-Multitask saveTo:Multitask fs:false edibleTime:200 trapped:true multitaskModes:2 pacmanMultitaskScheme:edu.utexas.cs.nn.tasks.mspacman.multitask.GhostsThenPillsModeSelector
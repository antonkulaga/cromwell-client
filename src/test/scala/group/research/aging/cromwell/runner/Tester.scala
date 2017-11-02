package group.research.aging.cromwell.runner

import java.io.{File => JFile}


object DeNovoGenomeQuality extends BasicRunner(
  "/home/antonkulaga/rna-seq/workflows",
  "de-novo/quality","quality_de_novo.wdl") {
  run("wilver.json")
  //run("mother_kidney.json")
}

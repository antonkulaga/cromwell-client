//dependencies
import $ivy.`com.lihaoyi::requests:0.1.7` //http client
import $ivy.`io.circe::circe-parser:0.11.1` //JSON library
@
//imports
import io.circe._
import io.circe.parser._
import scala.collection.immutable.SortedMap
import ammonite.ops._

//gets JSON from AJAX script
def getSampleJSON(sample: String, start: Int, len: Int): Json =
  parse(requests.get(s"https://db.cngb.org/pird/ajax/get_detail/?iDisplayStart=${start}&iDisplayLength=${len}&iSortCol_0=24&sSortDir_0=desc&iSortingCols=1&samplecode=${sample}").text()).right.get

//gets number of records per sample
def getLength(sample: String) = getSampleJSON(sample, 0, 0).hcursor.downField("iTotalRecords").as[Int].getOrElse(-1) //-1 if not exist

//gets sample as rows of Strings, ordered by the quantity of sequences
def getSample(sample: String, start: Int, len: Int): Vector[Vector[String]] = getSampleJSON(sample, start, len).hcursor.downField("aaData").as[Vector[Vector[String]]].getOrElse(Vector.empty[Vector[String]])

//gets first 10000 rows, after them Chinese AJAX crashes
def getSample10000(sample: String): Vector[Vector[String]] = {
  getSample(sample, 0, 10000)
}

//headers of the raws
val headers = List("","ID", "Locus", "Functional",
  "VGene","DGene","JGene","CGene","NTCDR3","AACDR3",
  "NTFragments","AAFragments","CDR3Pos","NTFragmentsPos","AAFragmentsPos",
  "V3Deletion","D5Deletion","D3Deletion","J5Deletion",
  "VDInsertion","DJInsertion","VJInsertion",
  "OriginalSource","Paired",
  "SeqCount","NTSequence","AASequence","MapInformation")

//gets samples with headers information
def getSampleMap(sample: String, start: Int, len: Int): Vector[SortedMap[String, String]] = getSample(sample, start, len).map(row=>SortedMap(headers.zip(row):_*))

def writeCSV(sample: String, path: Path, sep: String = ",") = {
  val data = getSample10000(sample)
  write(path, headers.mkString(sep) + "\n", createFolders = true)
  for(row <- data) write.append(path, row.mkString(sep) + "\n", createFolders = true)
}


//USAGE EXAMPLE (uncomment and put the path you want to try)
//val sampleId = "S18081001000101"
//writeCSV(sampleId, Path("/data/test/S18081001000101.csv"))

def getSamplesID(path: Path): Seq[String] = for(s <- read.lines(path).tail) yield s.substring(0, s.indexOf(","))
def writeSamples(samples: Seq[String], where: Path, sep: String = ",") = for((s, i) <- samples.zipWithIndex) {
  writeCSV(s, where / s, sep)
  println(s"[${i+1}/${samples.size}]")
}

@main
def main() = {
  //SAVING FILES
  val base = Path("/data/samples/CHINESE/")
  val samples_healthy = getSamplesID( base / "HEALTHY" / "sample_description__of_project_1551634227422.csv")
  println(s"downloading clonotypes for ${samples_healthy.length} healthy samples")
  writeSamples(samples_healthy, base / "HEALTHY" / "clonotypes")

  val samples_sle = getSamplesID(base / "SLE" / "sample_description__of_project_1551634015549.csv")
  println(s"downloading clonotypes for ${samples_sle.length} sle samples")
  writeSamples(samples_sle, base / "SLE" / "clonotypes")

  val samples_ra = getSamplesID(base / "RA" / "sample_description__of_project_1551633874828.csv")
  println(s"downloading clonotypes for ${samples_ra.length} ra samples")
  writeSamples(samples_ra, base / "RA" / "clonotypes")
}


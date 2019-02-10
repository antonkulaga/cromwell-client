package group.research.aging.cromwell.web.api

import akka.actor.ActorRef
import akka.http.scaladsl.server._
import group.research.aging.cromwell.client
import group.research.aging.cromwell.web.{Commands, Results}
import group.research.aging.cromwell.web.api.runners.MessagesAPI
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._
import akka.pattern._
import io.circe.Json
import io.circe.syntax._
import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import group.research.aging.cromwell.client
import group.research.aging.cromwell.client.{CallOutputs, CromwellClient, WorkflowStatus}
import group.research.aging.cromwell.web.{Commands, Results}
import group.research.aging.cromwell.web.api.runners.MessagesAPI
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

import scala.concurrent.Future
import scala.concurrent.duration._
@Path("/api")
class GetAllService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService {


  @GET
  @Path("/all")
  @Operation(summary = "Return all metadata", description = "Return all metadata",
    parameters = Array(
      new Parameter(name = "server", in = ParameterIn.QUERY, description = "url to the cromwell server"),
      new Parameter(name = "status", in = ParameterIn.QUERY, description = "show only workflows with some status"),
      new Parameter(name = "subworkflows", in = ParameterIn.QUERY, description = "if subworkflows should be shown")
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Metadata for all pipelines",
        content = Array(
          new Content(schema = new Schema(          example ="""
{
  "metadata": [
{
      "id": "517a3f71-b502-4ec5-b101-b73f3514c9c5",
      "submission": "2019-02-02T23:57:18.148Z",
      "status": "Succeeded",
      "start": "2019-02-02T23:57:32.016Z",
      "end": "2019-02-03T02:29:08.907Z",
      "inputs": {
        "extract_threads": 4,
        "salmon_threads": 2,
        "samples": [
          "GSM1698568",
          "GSM1698570",
          "GSM2927683",
          "GSM2927750",
          "GSM2042593",
          "GSM2042596"
        ],
        "samples_folder": "/data/samples",
        "key": "0a1d74f32382b8a154acacc3a024bdce3709",
        "bootstraps": 128,
        "salmon_indexes": {
          "Mus musculus": "/data/indexes/salmon/Mus_musculus",
          "Drosophila melanogaster": "/data/indexes/salmon/Drosophila_melanogaster",
          "Bos taurus": "/data/indexes/salmon/Bos_taurus",
          "Heterocephalus glaber": "/data/indexes/salmon/Heterocephalus_glaber",
          "Rattus norvegicus": "/data/indexes/salmon/Rattus_norvegicus",
          "Caenorhabditis elegans": "/data/indexes/salmon/Caenorhabditis_elegans",
          "Homo sapiens": "/data/indexes/salmon/Homo_sapiens"
        }
      },
      "outputs": {},
      "failures": [],
      "submittedFiles": {
        "inputs": "{\"quantification.key\":\"0a1d74f32382b8a154acacc3a024bdce3709\",\"quantification.samples_folder\":\"/data/samples\",\"quantification.salmon_indexes\":{\"Mus musculus\":\"/data/indexes/salmon/Mus_musculus\",\"Drosophila melanogaster\":\"/data/indexes/salmon/Drosophila_melanogaster\",\"Bos taurus\":\"/data/indexes/salmon/Bos_taurus\",\"Heterocephalus glaber\":\"/data/indexes/salmon/Heterocephalus_glaber\",\"Rattus norvegicus\":\"/data/indexes/salmon/Rattus_norvegicus\",\"Caenorhabditis elegans\":\"/data/indexes/salmon/Caenorhabditis_elegans\",\"Homo sapiens\":\"/data/indexes/salmon/Homo_sapiens\"},\"quantification.samples\":[\"GSM1698568\",\"GSM1698570\",\"GSM2927683\",\"GSM2927750\",\"GSM2042593\",\"GSM2042596\"]}",
        "workflow": "version development\n\nstruct QuantifiedRun {\n    String run\n    File folder\n    File quant\n    File lib\n    #Map[String, String] metadata\n    Array[Pair[String, String]] metainfo\n}\n\nstruct QuantifiedGSM {\n    Array[QuantifiedRun] runs\n    File metadata\n}\n\nworkflow quantification {\n    input {\n        Map[String, File] salmon_indexes\n        Array[String] samples\n        String key = \"0a1d74f32382b8a154acacc3a024bdce3709\"\n        Int extract_threads = 4\n        String samples_folder\n        Int salmon_threads = 2\n        Int bootstraps = 128\n    }\n\n    #Headers headers = {\"gsm\": 0, \"series\": 1, \"run\": 2, \"path\": 3, \"organism\": 4, \"model\": 5, \"layout\": 6, \"strategy\": 7, \"title\" : 8, \"name\": 9, \"characteristics\": 9}\n    scatter(gsm in samples) {\n\n        call get_gsm{\n            input: gsm = gsm, key = key\n        }\n\n        String gse_folder = samples_folder + \"/\" + get_gsm.runs[0][1]\n        String gsm_folder = gse_folder + \"/\" + get_gsm.runs[0][0]\n        Array[String] headers = get_gsm.headers\n\n\n       #Array[String] headers = get_gsm.headers\n       scatter(run in get_gsm.runs) {\n            Array[Pair[String, String]] pairs = zip(headers, run)\n            Map[String, String] info = as_map(pairs)\n            String layout = run[6] #info[\"layout\"] #run[6]\n            Boolean is_paired = (layout != \"SINGLE\")\n            String srr = run[2]#info[\"run\"]\n            String sra_folder = gsm_folder + \"/\" + srr #run[2]\n\n            call download {  input: sra = srr }\n            call extract {input: sra = download.out, is_paired = is_paired, threads = extract_threads}\n            call fastp { input: reads = extract.out, is_paired = is_paired }\n            call copy as copy_report {\n             input:\n                destination = sra_folder + \"/report\",\n                files = [fastp.report_json, fastp.report_html]\n            }\n\n            call copy as copy_cleaned_reads {\n             input:\n                destination = sra_folder + \"/reads\",\n                files = fastp.reads_cleaned\n            }\n\n            String organism = run[4]#info[\"organism\"] #run[4]\n\n            call salmon {\n                input:\n                    index = salmon_indexes[organism],\n                    reads = fastp.reads_cleaned,\n                    is_paired = is_paired,\n                    threads = salmon_threads,\n                    bootstraps = bootstraps,\n                    run = srr\n            }\n\n            call copy as copy_quant{\n            input:\n               destination = sra_folder,\n               files = [salmon.out]\n            }\n            File quant_folder = copy_quant.out[0]\n            File quant_lib = quant_folder + \"/\" + \"lib_format_counts.json\"\n            File quant = quant_folder + \"/\" + \"quant.sf\"\n\n            #QuantifiedRun quantified_run = {\"run\": srr, \"folder\": quant_folder, \"quant\": quant, \"lib\": quant_lib, \"metainfo\": pairs}\n            Map[String, File] runs = {\"run\": srr, \"folder\": quant_folder, \"quant\": quant, \"lib\": quant_lib}\n            Map[String, String ] metadata = info\n        }\n\n        #QuantifiedGSM quantified_gsm = {\"runs\": quantified_run, \"metadata\": get_gsm.gsm_json}\n\n    }\n\n    output {\n        #Array[QuantifiedGSM] quantified_gsms = quantified_gsm\n    }\n}\n\ntask get_gsm {\n\n    input {\n       String gsm\n       String key\n    }\n\n    String runs_path = gsm +\"_runs.tsv\"\n    String runs_tail_path = gsm +\"_runs_tail.tsv\"\n    String runs_head_path = gsm +\"_runs_head.tsv\"\n\n\n    command {\n        /opt/docker/bin/geo-fetch gsm --key ~{key} -e --output ~{gsm}.json --runs ~{runs_path}  ~{gsm}\n        head -n 1 ~{runs_path} > ~{runs_head_path}\n        tail -n +2 ~{runs_path} > ~{runs_tail_path}\n    }\n\n    runtime {\n        docker: \"quay.io/comp-bio-aging/geo-fetch:0.0.2\"\n    }\n\n    output {\n        File runs_tsv = runs_path\n        File gsm_json = gsm + \".json\"\n        Array[String] headers = read_tsv(runs_head_path)[0]\n        Array[Array[String]] runs = read_tsv(runs_tail_path)\n    }\n}\n\ntask download {\n    input {\n        String sra\n    }\n\n    command {\n        download_sra_aspera.sh ~{sra}\n    }\n\n    #https://github.com/antonkulaga/biocontainers/tree/master/downloaders/sra\n\n    runtime {\n        docker: \"quay.io/antonkulaga/download_sra:latest\"\n        #maxRetries: 2\n    }\n\n    output {\n        File out = \"results\" + \"/\" + sra + \".sra\"\n     }\n}\n\ntask extract {\n    input {\n        File sra\n        Boolean is_paired\n        Int threads\n    }\n\n    String name = basename(sra, \".sra\")\n    String folder = \"extracted\"\n    String prefix = folder + \"/\" + name\n    String prefix_sra = prefix + \".sra\"\n\n    #see https://github.com/ncbi/sra-tools/wiki/HowTo:-fasterq-dump for docs\n\n    command {\n        fasterq-dump --outdir ~{folder} --threads ~{threads} --progress --split-files --skip-technical ~{sra}\n        ~{if(is_paired) then \"mv\" + \" \" + prefix_sra + \"_1.fastq\" + \" \" + prefix + \"_1.fastq\"  else \"mv\" + \" \" + prefix_sra + \".fastq\" + \" \" + prefix + \".fastq\"}\n        ~{if(is_paired) then \"mv\" + \" \" + prefix_sra + \"_2.fastq\" + \" \" + prefix + \"_2.fastq\"  else \"\"}\n    }\n\n    runtime {\n        docker: \"quay.io/biocontainers/sra-tools@sha256:b03fd02fefc3e435cd36eef802cc43decba5d13612142e9bc9610f2727364f4f\" #2.9.1_1--h470a237_0\n        #maxRetries: 3\n    }\n\n    output {\n        Array[File] out = if(is_paired) then [prefix + \"_1.fastq\",  prefix + \"_2.fastq\"] else [prefix + \".fastq\"]\n     }\n}\n\n\n\ntask fastp {\n    input {\n        Array[File] reads\n        Boolean is_paired\n    }\n\n    command {\n        fastp --cut_by_quality5 --cut_by_quality3 --trim_poly_g --overrepresentation_analysis \\\n            -i ~{reads[0]} -o ~{basename(reads[0], \".fastq.gz\")}_cleaned.fastq.gz \\\n            ~{if( is_paired ) then \"--detect_adapter_for_pe \" + \"--correction -I \"+reads[1]+\" -O \" + basename(reads[1], \".fastq.gz\") +\"_cleaned.fastq.gz\" else \"\"}\n    }\n\n    runtime {\n        docker: \"quay.io/biocontainers/fastp@sha256:159da35f3a61f6b16650ceef6583c49d73396bc2310c44807a0d929c035d1011\" #0.19.5--hd28b015_0\n    }\n\n    output {\n        File report_json = \"fastp.json\"\n        File report_html = \"fastp.html\"\n        Array[File] reads_cleaned = if( is_paired )\n            then [basename(reads[0], \".fastq.gz\") + \"_cleaned.fastq.gz\", basename(reads[1], \".fastq.gz\") + \"_cleaned.fastq.gz\"]\n            else [basename(reads[0], \".fastq.gz\") + \"_cleaned.fastq.gz\"]\n    }\n}\n\ntask salmon {\n  input {\n    File index\n    Array[File] reads\n    Boolean is_paired\n    Int threads\n    Int bootstraps = 128\n    String run\n  }\n\n  command {\n    salmon --no-version-check quant -i ~{index}  --numBootstraps ~{bootstraps} --threads ~{threads} -l A --seqBias --gcBias -o quant_~{run} \\\n    ~{if(is_paired) then \"-1 \" + reads[0] + \" -2 \"+ reads[1] else \"-r \" + reads[0]}\n  }\n  # --validateMappings --rangeFactorizationBins ~{rangeFactorizationBins}\n\n  runtime {\n    docker: \"combinelab/salmon:0.12.0\"\n    maxRetries: 3\n  }\n\n  output {\n    File out = \"quant_\" + run\n    File lib = out + \"/\" + \"lib_format_counts.json\"\n    File quant = out + \"/\" + \"quant.sf\"\n  }\n}\n\n\ntask copy {\n    input {\n        Array[File] files\n        String destination\n    }\n\n    command {\n        mkdir -p ~{destination}\n        cp -L -R -u ~{sep=' ' files} ~{destination}\n    }\n\n    output {\n        Array[File] out = files\n    }\n}",
        "options": "{\n\n}"
      },
      "workflowName": "quantification",
      "workflowRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5",
      "calls": {
        "quantification.get_gsm": [
          {
            "stderr": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-0/execution/stderr",
            "stdout": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-0/execution/stdout",
            "attempt": 1,
            "shardIndex": 0,
            "callRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-0",
            "executionStatus": "Done",
            "callCaching": {
              "allowResultReuse": true,
              "effectiveCallCachingMode": "ReadAndWriteCache",
              "hit": true,
              "result": "Cache Hit: cf14203a-6554-4c12-9908-6de88a20f083:quantification.get_gsm:0"
            }
          },
          {
            "stderr": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-1/execution/stderr",
            "stdout": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-1/execution/stdout",
            "attempt": 1,
            "shardIndex": 1,
            "callRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-1",
            "executionStatus": "Done",
            "callCaching": {
              "allowResultReuse": true,
              "effectiveCallCachingMode": "ReadAndWriteCache",
              "hit": true,
              "result": "Cache Hit: cf14203a-6554-4c12-9908-6de88a20f083:quantification.get_gsm:1"
            }
          },
          {
            "stderr": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-2/execution/stderr",
            "stdout": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-2/execution/stdout",
            "attempt": 1,
            "shardIndex": 2,
            "callRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-2",
            "executionStatus": "Done",
            "callCaching": {
              "allowResultReuse": true,
              "effectiveCallCachingMode": "ReadAndWriteCache",
              "hit": true,
              "result": "Cache Hit: cf14203a-6554-4c12-9908-6de88a20f083:quantification.get_gsm:2"
            }
          },
          {
            "stderr": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-3/execution/stderr",
            "stdout": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-3/execution/stdout",
            "attempt": 1,
            "shardIndex": 3,
            "callRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-3",
            "executionStatus": "Done",
            "callCaching": {
              "allowResultReuse": true,
              "effectiveCallCachingMode": "ReadAndWriteCache",
              "hit": true,
              "result": "Cache Hit: cf14203a-6554-4c12-9908-6de88a20f083:quantification.get_gsm:3"
            }
          },
          {
            "stderr": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-4/execution/stderr",
            "stdout": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-4/execution/stdout",
            "attempt": 1,
            "shardIndex": 4,
            "callRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-4",
            "executionStatus": "Done",
            "callCaching": {
              "allowResultReuse": true,
              "effectiveCallCachingMode": "ReadAndWriteCache",
              "hit": true,
              "result": "Cache Hit: cf14203a-6554-4c12-9908-6de88a20f083:quantification.get_gsm:4"
            }
          },
          {
            "stderr": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-5/execution/stderr",
            "stdout": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-5/execution/stdout",
            "attempt": 1,
            "shardIndex": 5,
            "callRoot": "/data/cromwell-executions/quantification/517a3f71-b502-4ec5-b101-b73f3514c9c5/call-get_gsm/shard-5",
            "executionStatus": "Done",
            "callCaching": {
              "allowResultReuse": true,
              "effectiveCallCachingMode": "ReadAndWriteCache",
              "hit": true,
              "result": "Cache Hit: cf14203a-6554-4c12-9908-6de88a20f083:quantification.get_gsm:5"
            }
          }
        ],
        "ScatterAt41_16": [
          {
            "stderr": "",
            "stdout": "",
            "attempt": 1,
            "shardIndex": 0,
            "callRoot": "",
            "executionStatus": "Done",
            "callCaching": null
          },
          {
            "stderr": "",
            "stdout": "",
            "attempt": 1,
            "shardIndex": 1,
            "callRoot": "",
            "executionStatus": "Done",
            "callCaching": null
          },
          {
            "stderr": "",
            "stdout": "",
            "attempt": 1,
            "shardIndex": 2,
            "callRoot": "",
            "executionStatus": "Done",
            "callCaching": null
          },
          {
            "stderr": "",
            "stdout": "",
            "attempt": 1,
            "shardIndex": 3,
            "callRoot": "",
            "executionStatus": "Done",
            "callCaching": null
          },
          {
            "stderr": "",
            "stdout": "",
            "attempt": 1,
            "shardIndex": 4,
            "callRoot": "",
            "executionStatus": "Done",
            "callCaching": null
          },
          {
            "stderr": "",
            "stdout": "",
            "attempt": 1,
            "shardIndex": 5,
            "callRoot": "",
            "executionStatus": "Done",
            "callCaching": null
          }
        ]
      }
    }
]
}
""",
          )
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def getAllMeta: Route =
    withServerExtended("getting all metadata") { (server, status, sub) =>
      val com = Commands.GetAllMetadata(status, sub)
      import io.circe.syntax._
      val comm = MessagesAPI.ServerCommand(com, server)
      val fut = (runner ? comm).mapTo[Results.UpdatedMetadata]
      fut
    }



  def routes: Route  = pathPrefix("all"){ getAllMeta }
}

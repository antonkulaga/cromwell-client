package group.research.aging.cromwell.web.api

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
import io.swagger.v3.oas.annotations.enums.{ParameterIn, ParameterStyle}
import io.swagger.v3.oas.annotations.media._
import io.swagger.v3.oas.annotations.responses._
import javax.ws.rs._

import scala.concurrent.Future
import scala.concurrent.duration._

@Path("/api")
class WorkflowService(val runner: ActorRef)(implicit val timeout: Timeout) extends CromwellClientService {


  @GET
  @Path("/metadata/{id}")
  @Operation(summary = "Return metadata", description = "Return metadata for specific pipeline", tags = Array("workflow"),
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the metadata",  style = ParameterStyle.SIMPLE, allowReserved = true),
      new Parameter(name = "server", in = ParameterIn.QUERY, description = "url to the cromwell server",   style = ParameterStyle.SIMPLE, allowReserved = true),
      new Parameter(name = "subworkflows", in = ParameterIn.QUERY, description = "if subworkflows should be shown",   style = ParameterStyle.SIMPLE, allowReserved = true)
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Metadata for specific workflow",
        content = Array(new Content(schema = new Schema(implementation = classOf[client.Metadata])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def metaData: Route = pathPrefix("metadata" / Remaining) { id =>
    withServerExtended("gettings metadata"){
      (server, status, incl) =>
        val comm = MessagesAPI.ServerCommand(Commands.QueryWorkflows(status, incl), server)
       (runner ? comm).mapTo[Results.UpdatedMetadata]
    }
  }

  @GET
  @Path("/outputs/{id}")
  @Operation(summary = "Return outputs", description = "Return outputs of specific workflow", tags = Array("workflow"),
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the metadata",   style = ParameterStyle.SIMPLE, allowReserved = true),
      new Parameter(name = "host", in = ParameterIn.QUERY, description = "url to the cromwell server",   style = ParameterStyle.SIMPLE, allowReserved = true)
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Outputs for specific workflow",
        content = Array(new Content(schema = new Schema(example ="""
          {
            "outputs": {
              "quantification.results": [
            {
              "metadata": "/data/cromwell-executions/quantification/fa6d098a-4986-4aca-9562-86fdd4adf37c/call-quant_sample/shard-0/quant_sample/3e77f791-b979-4656-b1c2-cc297ee62425/call-get_gsm/execution/GSM1164701.json",
              "runs": [
            {
              "genes": "/data/samples/GSE47999/GSM1164701/SRR901960/GSE47999_GSM1164701_SRR901960_genes_abundance.tsv",
              "quant": "/data/samples/GSE47999/GSM1164701/SRR901960/quant_GSE47999_GSM1164701_SRR901960/quant.sf",
              "run": "SRR901960",
              "run_folder": "/data/samples/GSE47999/GSM1164701/SRR901960",
              "lib": "/data/samples/GSE47999/GSM1164701/SRR901960/quant_GSE47999_GSM1164701_SRR901960/lib_format_counts.json",
              "metadata": {
              "series": "GSE47999",
              "name": "GSM1164701",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra12/SRR/000880/SRR901960",
              "model": "Illumina HiSeq 2000",
              "run": "SRR901960",
              "gsm": "GSM1164701",
              "characteristics": "tissue -> whole body;radiation dose -> 20,000 Roentgen;time post-irradiation (days) -> 10",
              "strategy": "RNA-Seq",
              "organism": "Drosophila melanogaster",
              "layout": "SINGLE",
              "title": "20,000 R Day 10, Rep 2"
            },
              "tx2gene": "/data/indexes/FLY/96/dmelanogaster_96_tx2gene.tsv",
              "quant_folder": "/data/samples/GSE47999/GSM1164701/SRR901960/quant_GSE47999_GSM1164701_SRR901960"
            }
              ]
            },
            {
              "metadata": "/data/cromwell-executions/quantification/fa6d098a-4986-4aca-9562-86fdd4adf37c/call-quant_sample/shard-1/quant_sample/f142d95e-8c63-40b2-bdc5-45225bce8fad/call-get_gsm/execution/GSM1164702.json",
              "runs": [
            {
              "genes": "/data/samples/GSE47999/GSM1164702/SRR901961/GSE47999_GSM1164702_SRR901961_genes_abundance.tsv",
              "quant": "/data/samples/GSE47999/GSM1164702/SRR901961/quant_GSE47999_GSM1164702_SRR901961/quant.sf",
              "run": "SRR901961",
              "run_folder": "/data/samples/GSE47999/GSM1164702/SRR901961",
              "lib": "/data/samples/GSE47999/GSM1164702/SRR901961/quant_GSE47999_GSM1164702_SRR901961/lib_format_counts.json",
              "metadata": {
              "series": "GSE47999",
              "name": "GSM1164702",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra12/SRR/000880/SRR901961",
              "model": "Illumina HiSeq 2000",
              "run": "SRR901961",
              "gsm": "GSM1164702",
              "characteristics": "tissue -> whole body;radiation dose -> 20,000 Roentgen;time post-irradiation (days) -> 10",
              "strategy": "RNA-Seq",
              "organism": "Drosophila melanogaster",
              "layout": "SINGLE",
              "title": "20,000 R Day 10, Rep 3"
            },
              "tx2gene": "/data/indexes/FLY/96/dmelanogaster_96_tx2gene.tsv",
              "quant_folder": "/data/samples/GSE47999/GSM1164702/SRR901961/quant_GSE47999_GSM1164702_SRR901961"
            }
              ]
            },
            {
              "metadata": "/data/cromwell-executions/quantification/fa6d098a-4986-4aca-9562-86fdd4adf37c/call-quant_sample/shard-2/quant_sample/7b5f5ab3-7881-441a-84c9-883a57dfc996/call-get_gsm/execution/GSM1164703.json",
              "runs": [
            {
              "genes": "/data/samples/GSE47999/GSM1164703/SRR901962/GSE47999_GSM1164703_SRR901962_genes_abundance.tsv",
              "quant": "/data/samples/GSE47999/GSM1164703/SRR901962/quant_GSE47999_GSM1164703_SRR901962/quant.sf",
              "run": "SRR901962",
              "run_folder": "/data/samples/GSE47999/GSM1164703/SRR901962",
              "lib": "/data/samples/GSE47999/GSM1164703/SRR901962/quant_GSE47999_GSM1164703_SRR901962/lib_format_counts.json",
              "metadata": {
              "series": "GSE47999",
              "name": "GSM1164703",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra12/SRR/000880/SRR901962",
              "model": "Illumina HiSeq 2000",
              "run": "SRR901962",
              "gsm": "GSM1164703",
              "characteristics": "tissue -> whole body;radiation dose -> 20,000 Roentgen;time post-irradiation (days) -> 20",
              "strategy": "RNA-Seq",
              "organism": "Drosophila melanogaster",
              "layout": "SINGLE",
              "title": "20,000 R Day 20, Rep 1"
            },
              "tx2gene": "/data/indexes/FLY/96/dmelanogaster_96_tx2gene.tsv",
              "quant_folder": "/data/samples/GSE47999/GSM1164703/SRR901962/quant_GSE47999_GSM1164703_SRR901962"
            }
              ]
            },
            {
              "metadata": "/data/cromwell-executions/quantification/fa6d098a-4986-4aca-9562-86fdd4adf37c/call-quant_sample/shard-3/quant_sample/3fbfe78a-24b6-42dc-bfee-64e4a40c95e8/call-get_gsm/execution/GSM1164704.json",
              "runs": [
            {
              "genes": "/data/samples/GSE47999/GSM1164704/SRR901963/GSE47999_GSM1164704_SRR901963_genes_abundance.tsv",
              "quant": "/data/samples/GSE47999/GSM1164704/SRR901963/quant_GSE47999_GSM1164704_SRR901963/quant.sf",
              "run": "SRR901963",
              "run_folder": "/data/samples/GSE47999/GSM1164704/SRR901963",
              "lib": "/data/samples/GSE47999/GSM1164704/SRR901963/quant_GSE47999_GSM1164704_SRR901963/lib_format_counts.json",
              "metadata": {
              "series": "GSE47999",
              "name": "GSM1164704",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra12/SRR/000880/SRR901963",
              "model": "Illumina HiSeq 2000",
              "run": "SRR901963",
              "gsm": "GSM1164704",
              "characteristics": "tissue -> whole body;radiation dose -> 20,000 Roentgen;time post-irradiation (days) -> 20",
              "strategy": "RNA-Seq",
              "organism": "Drosophila melanogaster",
              "layout": "SINGLE",
              "title": "20,000 R Day 20, Rep 2"
            },
              "tx2gene": "/data/indexes/FLY/96/dmelanogaster_96_tx2gene.tsv",
              "quant_folder": "/data/samples/GSE47999/GSM1164704/SRR901963/quant_GSE47999_GSM1164704_SRR901963"
            }
              ]
            }
              ]
            },
            "id": "fa6d098a-4986-4aca-9562-86fdd4adf37c"
          }""")))
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def outputs: Route =  pathPrefix("outputs" / Remaining) { id =>
    withServer("getting outputs"){
      server =>
        val g = Commands.SingleWorkflow.GetOutput(id)
        val comm = MessagesAPI.ServerCommand(g, server)
        val fut = (runner ? comm).mapTo[CallOutputs]
        fut
    }
  }
  @GET
  @Path("/status/{id}")
  @Operation(summary = "Return status", description = "Return status of specific workflow", tags = Array("workflow"),
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, description = "ID of the workflow which returns the status",  style = ParameterStyle.SIMPLE, allowReserved = true),
      new Parameter(name = "host", in = ParameterIn.QUERY, description = "url to the cromwell server",   style = ParameterStyle.SIMPLE, allowReserved = true)
    ),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Outputs for specific workflow",
        content = Array(new Content(schema = new Schema(implementation = classOf[client.StatusInfo])))),
      new ApiResponse(responseCode = "500", description = "Internal server error"))
  )
  def status: Route =   pathPrefix("status" / Remaining) { id =>
    withServer("getting status"){
      server =>
        val g = Commands.SingleWorkflow.GetStatus(id)
        val comm = MessagesAPI.ServerCommand(g, server)
        val fut = (runner ? comm).mapTo[CallOutputs]
        fut
    }
  }

  def routes: Route = metaData ~ outputs ~ status

}

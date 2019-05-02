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
        content = Array(new Content(schema = new Schema(example =
          """
{
  "outputs": {
    "quantification.results": [
      {
        "metadata": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-0/quant_sample/9e867a91-8060-4eaf-a072-1087643193a0/call-get_gsm/execution/GSM1698568.json",
        "runs": [
          {
            "run": "SRR2014238",
            "run_folder": "/data/samples/GSE69360/GSM1698568/SRR2014238",
            "lib": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-0/quant_sample/9e867a91-8060-4eaf-a072-1087643193a0/call-quant_run/shard-0/quant_run/08b1f22b-4464-4a33-b13a-d81d7f74ed5a/call-salmon/execution/quant_SRR2014238/lib_format_counts.json",
            "metadata": {
              "series": "GSE69360",
              "name": "Biochain_Adult_Liver",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra29/SRR/001967/SRR2014238",
              "model": "Illumina HiSeq 2000",
              "run": "SRR2014238",
              "gsm": "GSM1698568",
              "characteristics": "number of donors -> 1;age -> 64 years old;tissue -> Liver;vendor -> Biochain;isolate -> Lot no.: B510092;gender -> Male",
              "strategy": "RNA-Seq",
              "organism": "Homo sapiens",
              "layout": "PAIRED",
              "title": "Biochain_Adult_Liver"
            },
            "quant_folder": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-0/quant_sample/9e867a91-8060-4eaf-a072-1087643193a0/call-quant_run/shard-0/quant_run/08b1f22b-4464-4a33-b13a-d81d7f74ed5a/call-salmon/execution/quant_SRR2014238"
          }
        ]
      },
      {
        "metadata": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-1/quant_sample/ae3e3ff9-c1be-45b5-8c62-b084b8e04ce4/call-get_gsm/execution/GSM1698570.json",
        "runs": [
          {
            "run": "SRR2014240",
            "run_folder": "/data/samples/GSE69360/GSM1698570/SRR2014240",
            "lib": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-1/quant_sample/ae3e3ff9-c1be-45b5-8c62-b084b8e04ce4/call-quant_run/shard-0/quant_run/95334282-ffea-446e-b679-ee3e2885f676/call-salmon/execution/quant_SRR2014240/lib_format_counts.json",
            "metadata": {
              "series": "GSE69360",
              "name": "Biochain_Adult_Kidney",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra29/SRR/001967/SRR2014240",
              "model": "Illumina HiSeq 2000",
              "run": "SRR2014240",
              "gsm": "GSM1698570",
              "characteristics": "number of donors -> 1;age -> 26 years old;tissue -> Kidney;vendor -> Biochain;isolate -> Lot no.: B106007;gender -> Male",
              "strategy": "RNA-Seq",
              "organism": "Homo sapiens",
              "layout": "PAIRED",
              "title": "Biochain_Adult_Kidney"
            },
            "quant_folder": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-1/quant_sample/ae3e3ff9-c1be-45b5-8c62-b084b8e04ce4/call-quant_run/shard-0/quant_run/95334282-ffea-446e-b679-ee3e2885f676/call-salmon/execution/quant_SRR2014240"
          }
        ]
      },
      {
        "metadata": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-2/quant_sample/84d9cd88-4761-4b31-92f9-778186aa8e7d/call-get_gsm/execution/GSM2927683.json",
        "runs": [
          {
            "run": "SRR6456687",
            "run_folder": "/data/samples/GSE108990/GSM2927683/SRR6456687",
            "lib": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-2/quant_sample/84d9cd88-4761-4b31-92f9-778186aa8e7d/call-quant_run/shard-0/quant_run/67a29600-3a23-442e-82cf-efe95f7b5c56/call-salmon/execution/quant_SRR6456687/lib_format_counts.json",
            "metadata": {
              "series": "GSE108990",
              "name": "GSM2927683",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra57/SRR/006305/SRR6456687",
              "model": "Illumina HiSeq 2500",
              "run": "SRR6456687",
              "gsm": "GSM2927683",
              "characteristics": "strain -> CAST/EiJ;genotype -> Wild-type;treatment -> Clean-air;tissue -> liver",
              "strategy": "RNA-Seq",
              "organism": "Mus musculus",
              "layout": "PAIRED",
              "title": "RNA_105_liver_Control"
            },
            "quant_folder": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-2/quant_sample/84d9cd88-4761-4b31-92f9-778186aa8e7d/call-quant_run/shard-0/quant_run/67a29600-3a23-442e-82cf-efe95f7b5c56/call-salmon/execution/quant_SRR6456687"
          }
        ]
      },
      {
        "metadata": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-3/quant_sample/2a2fac1a-8650-4770-91e0-3371963861f2/call-get_gsm/execution/GSM2927750.json",
        "runs": [
          {
            "run": "SRR6456754",
            "run_folder": "/data/samples/GSE108990/GSM2927750/SRR6456754",
            "lib": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-3/quant_sample/2a2fac1a-8650-4770-91e0-3371963861f2/call-quant_run/shard-0/quant_run/43eb1ca9-c2cd-4a62-b5a2-38f40adce4d1/call-salmon/execution/quant_SRR6456754/lib_format_counts.json",
            "metadata": {
              "series": "GSE108990",
              "name": "GSM2927750",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra57/SRR/006305/SRR6456754",
              "model": "Illumina HiSeq 2500",
              "run": "SRR6456754",
              "gsm": "GSM2927750",
              "characteristics": "strain -> CAST/EiJ;genotype -> Wild-type;treatment -> Clean-air;tissue -> kidney",
              "strategy": "RNA-Seq",
              "organism": "Mus musculus",
              "layout": "PAIRED",
              "title": "RNA_105_kidney_Control"
            },
            "quant_folder": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-3/quant_sample/2a2fac1a-8650-4770-91e0-3371963861f2/call-quant_run/shard-0/quant_run/43eb1ca9-c2cd-4a62-b5a2-38f40adce4d1/call-salmon/execution/quant_SRR6456754"
          }
        ]
      },
      {
        "metadata": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-4/quant_sample/6393b2b0-4ba1-476f-837f-5c34e2dd69b3/call-get_gsm/execution/GSM2042593.json",
        "runs": [
          {
            "run": "SRR3109705",
            "run_folder": "/data/samples/GSE77020/GSM2042593/SRR3109705",
            "lib": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-4/quant_sample/6393b2b0-4ba1-476f-837f-5c34e2dd69b3/call-quant_run/shard-0/quant_run/12af585a-4e1b-4a27-aafc-f9627a37e678/call-salmon/execution/quant_SRR3109705/lib_format_counts.json",
            "metadata": {
              "series": "GSE77020",
              "name": "GSM2042593",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra37/SRR/003036/SRR3109705",
              "model": "Illumina HiSeq 2500",
              "run": "SRR3109705",
              "gsm": "GSM2042593",
              "characteristics": "strain -> indigenous;location -> Chengdu, Sichuan province, China;tissue -> liver;age -> ~4 years old",
              "strategy": "RNA-Seq",
              "organism": "Bos taurus",
              "layout": "PAIRED",
              "title": "cattle_liver_1"
            },
            "quant_folder": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-4/quant_sample/6393b2b0-4ba1-476f-837f-5c34e2dd69b3/call-quant_run/shard-0/quant_run/12af585a-4e1b-4a27-aafc-f9627a37e678/call-salmon/execution/quant_SRR3109705"
          }
        ]
      },
      {
        "metadata": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-5/quant_sample/04b25fe7-77e7-4b48-bef3-1b3192470ea1/call-get_gsm/execution/GSM2042596.json",
        "runs": [
          {
            "run": "SRR3109708",
            "run_folder": "/data/samples/GSE77020/GSM2042596/SRR3109708",
            "lib": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-5/quant_sample/04b25fe7-77e7-4b48-bef3-1b3192470ea1/call-quant_run/shard-0/quant_run/5412ec9b-dfca-422b-baff-6ac4c53fa39f/call-salmon/execution/quant_SRR3109708/lib_format_counts.json",
            "metadata": {
              "series": "GSE77020",
              "name": "GSM2042596",
              "path": "https://sra-download.ncbi.nlm.nih.gov/traces/sra38/SRR/003036/SRR3109708",
              "model": "Illumina HiSeq 2500",
              "run": "SRR3109708",
              "gsm": "GSM2042596",
              "characteristics": "strain -> indigenous;location -> Chengdu, Sichuan province, China;tissue -> kidney;age -> ~4 years old",
              "strategy": "RNA-Seq",
              "organism": "Bos taurus",
              "layout": "PAIRED",
              "title": "cattle_kidney_1"
            },
            "quant_folder": "/data/cromwell-executions/quantification/39d5131a-8a35-49db-87d8-c11105a84d36/call-quant_sample/shard-5/quant_sample/04b25fe7-77e7-4b48-bef3-1b3192470ea1/call-quant_run/shard-0/quant_run/5412ec9b-dfca-422b-baff-6ac4c53fa39f/call-salmon/execution/quant_SRR3109708"
          }
        ]
      }
    ]
  },
  "id": "39d5131a-8a35-49db-87d8-c11105a84d36"
}
          """)))
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

package group.research.aging.cromwell.web.server

object Pages extends Pages
trait Pages {

  val indexHTML =
    <html>
      <head>
        <meta charset="utf-8" />
        <title>GEOmetadb UI</title>
        <script type="text/javascript" src="https://code.jquery.com/jquery-3.3.1.js"></script>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.3.1/semantic.css" />
        <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.3.1/semantic.js"></script>
        <link rel="stylesheet" href="/styles/mystyles.css" />
        <script type="text/javascript" src="/public/out.js"></script>
      </head>
      <body id ="main">
        <div class="ui blue piled segment">
          <div id="blank">
          </div>
        </div>
      </body>
    </html>

}
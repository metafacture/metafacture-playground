FLUX_DIR + "playground.data"
|open-file
|as-lines
|decode-pica
|fix("
  paste('{to:118514768}authorOf', '_id') # TODO: 118514768 from 028A.9
  retain('{to:118514768}authorOf')
")
|stream-to-triples(redirect="true")
|count-triples(countBy="subject")
|@X;

"https://raw.githubusercontent.com/hbz/metafacture-flux-examples/master/sample6/authority-persons.pica"
|open-http
|as-lines
|decode-pica
|fix("
  paste('name', '028A.d', '028A.a')
  retain('name')
")
|stream-to-triples
|@X;

@X
|wait-for-inputs("2")
|sort-triples(by="subject")
|collect-triples
|encode-formeta(style="verbose")
|print;
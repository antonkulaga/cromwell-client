val str = """19832	5585	5753	2698	1230	2825	1694
5585	7997	3942	1418	865	1681	940
5753	3942	8620	2109	1150	2310	1466
2698	1418	2109	50582	1110	5271	6319
1230	865	1150	1110	5968	729	824
2825	1681	2310	5271	729	20618	4029
1694	940	1466	6319	824	4029	38037"""

//"[" + str.replace("	", "	,") + "]"

val list = s"[" + str.split("\n").map(s =>"[" + s.replace("	", ", ") + "]").mkString("\n")
/*

val mat: Array[Array[String]] = str.split("\n").map(_.split("\t"))

val headers: Map[String, Int] = "Bowhead whale   Gray whale   Minke whale   Human   Naked mole rat   Cow   Mouse".split("   ").zipWithIndex.toMap
def get(one: String, two: String): String = {
  val r = headers(one)
  val c = headers(two)
  mat(r)(c)
}


get("Bowhead whale", "Gray whale")


def make1(one: String) = {
  s"""{sets:["${one}"], size: ${get(one, one)}}"""
}

def make2(one: String, two: String) = {
  s"""{sets:["${one}", "${two}"], size: ${get(one, two)}}"""
}

for{
  name <- headers.keys
} println(make1(name) + ",")

for{
  one <- headers.keys
  two <- headers.keys
  if one != two
} println(make2(one, two) + ",")
*/
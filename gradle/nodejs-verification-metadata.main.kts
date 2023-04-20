@file:DependsOn("com.squareup.okhttp3:okhttp:4.10.0")

// https://nodejs.org/dist/v18.12.1/SHASUMS256.txt
val version = "18.12.1"
val url = "https://nodejs.org/dist/v$version/SHASUMS256.txt"
val info = loadText(url).lines()
    .filter { it.isNotEmpty() }
    .map { it.split("\\s+".toRegex(), 2) }

println("Loaded info about ${info.size} artifacts")

val indent = " ".repeat(6)
var xml = "\n\n"
xml += "$indent<component group=\"org.nodejs\" name=\"node\" version=\"$version\">\n"
for ((sha256, name) in info) {
    // skip thisngs like 'win-x64/node.exe'
    if ('/' in name) continue
    xml = xml.addComponentForArtifact(sha256, name)
}
xml += "$indent</component>\n\n"

println(xml)

fun String.addComponentForArtifact(sha256: String, name: String): String {
    var xml = this
    xml += "$indent   <artifact name=\"$name\">\n" // TODO: escape?
    xml += "$indent      <sha256 value=\"$sha256\" origin=\"$url\" reason=\"NodeJS distribition\"/>\n"
    xml += "$indent   </artifact>\n"

    // It seems like Kotlin changes name after downloading, support both versions.
    val prefix = "node-v"
    if (name.startsWith(prefix)) {
        val name2 = "node-" + name.substring(prefix.length)
        xml = xml.addComponentForArtifact(sha256, name2)
    }

    return xml
}

fun loadText(url: String): String {
    val request = okhttp3.Request.Builder().url(url).build()
    return okhttp3.OkHttpClient().newCall(request).execute().use { it.body?.string() ?: "" }
}

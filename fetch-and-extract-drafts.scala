#!/usr/bin/env -S scala-cli shebang
//> using dep "com.lihaoyi::mainargs:0.7.6"
//> using dep "com.lihaoyi::os-lib:0.11.3"
import scala.compiletime.ops.double

import os.{*, given}
import mainargs.*

object FetchDrafts:

  val rfcs = List(
    8525, // Replaces 7895
    8072, 8639, 8641, 9196, 9640, 9641, 9642, 9643, 9645
  )

  val drafts = List(
    "draft-ietf-netconf-https-notif-15",
    "draft-ietf-netconf-udp-notif-16",
    "draft-ietf-netconf-udp-client-server-05",
    "draft-ietf-netconf-distributed-notif-10",
    "draft-ietf-netconf-http-client-server-23",
    "draft-ietf-netconf-yang-notifications-versioning-05",
    "draft-ietf-netconf-yang-library-augmentedby-01",
    "draft-ietf-netmod-yang-module-versioning-12",
    "draft-ietf-netmod-yang-semver-17",
    "draft-tgraf-netconf-yang-push-observation-time-02",
    // "draft-tgraf-netconf-notif-sequencing-06", // Use notif-envolope instead?
    "draft-netana-netconf-yp-transport-capabilities-00",
    "draft-netana-netconf-notif-envelope-01"
  )

  val ianaYang = List(
    "iana-tls-cipher-suite-algs.yang"
  )

  // YANG files that are only imported, we don't want to build schema nodes for.
  // Any file in this list are moved from the published directory to the
  // import-only sub-directory.
  val importOnly = List()
  //   "ietf-notification-sequencing@2023-05-29.yang",
  //   "ietf-https-notif-transport@2024-02-01.yang",
  //   "ietf-keystore@2024-10-10.yang",
  //   "ietf-truststore@2024-10-10.yang",
  //   "ietf-tls-common@2024-10-10.yang"
  // )

  val excludedYangModules = List(
    "ietf-https-notif-transport@2024-02-01.yang",
    "ietf-yang-library-rfc7895-augmentedby.yang"
  )

  val docsDir = os.pwd / "docs"
  val yangDir = os.pwd / "yang" / "published"

  @main(doc = """|Fetch and extra drafts""".stripMargin)
  def buildYangTree(
      @arg(doc = "Apply deviations")
      applyDeviations: Flag
  ) =
    if !os.exists(docsDir) then os.makeDir(docsDir)

    // Download RFCs
    for rfcNo <- rfcs do
      val rfcDoc = docsDir / s"rfc$rfcNo.txt"
      if !os.exists(rfcDoc) then
        val cmd =
          s"curl -s -o $rfcDoc https://www.rfc-editor.org/rfc/rfc$rfcNo.txt"
        val cmdRes = os.call(cmd = ("sh", "-c", cmd))
        if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
        else println(s"RFC $rfcNo fetched")

    // Download drafts
    for draft <- drafts do
      val draftDoc = docsDir / s"$draft.txt"
      if !os.exists(draftDoc) then
        val cmdStr =
          s"curl -s -o $draftDoc https://www.ietf.org/archive/id/$draft.txt"
        val cmdRes = os.call(cmd = ("sh", "-c", cmdStr))
        if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
        else println(s"Draft $draft fetched")

    // Extract YANG files
    if !os.exists(yangDir) then os.makeDir.all(yangDir)
    val cmd = s"bin/rfcstrip -d $yangDir ${docsDir}/*"
    val cmdRes = os.call(cmd = ("sh", "-c", cmd))
    if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
    else println(s"YANG extracted to $yangDir")

    // Download IANA YANG modules
    for yangFile <- ianaYang do
      val yangFilePath = yangDir / yangFile
      if !os.exists(yangFilePath) then
        val cmdStr =
          s"curl -s -o $yangFilePath https://www.iana.org/assignments/yang-parameters/$yangFile"
        val cmdRes = os.call(cmd = ("sh", "-c", cmdStr))
        if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
        else println(s"IANA YANG $yangFile fetched")

    // Remove unwanted/implemented YANG files.
    for yangFile <- excludedYangModules do
      os.remove(yangDir / yangFile, checkExists = false)

    // Remove example files.
    for file <- os.list(yangDir) do
      if file.baseName.startsWith("example") ||
        file.baseName.startsWith("ex-")
      then os.remove(file)

    val renames = Map(
      "ietf-udp-client@2024-10-15" -> "ietf-udp-client@2024-10-15.yang",
      "ietf-distributed-notif@2024-04-14.yang" -> "ietf-distributed-notif@2024-04-21.yang",
      "ietf-yang-push-revision@2024-05-28.yang" -> "ietf-yang-push-revision@2024-06-16.yang"
    )

    // Fixes, rename wrong filenames.
    for rename <- renames do
      val oldPath = yangDir / rename._1
      val newPath = yangDir / rename._2
      if os.exists(oldPath) then
        os.move(oldPath, newPath, replaceExisting = true)

    // Simple renames within drafts.
    case class Patch(draft: String, from: String, to: String)
    val patches = List(
      Patch(
        draft = "ietf-udp-server@2024-10-15.yang",
        from = "grouping udp-server",
        to = "grouping udp-server-grouping"
      ),
      Patch(
        draft = "ietf-udp-client@2024-10-15.yang",
        from = "grouping udp-client",
        to = "grouping udp-client-grouping"
      ),
      Patch(
        draft = "ietf-https-notif-transport@2024-02-01.yang",
        from = "uses httpc:http-client-stack-grouping",
        to = "uses httpc:http-client-grouping"
      )
    )

    for p <- patches do
      if os.exists(yangDir / p.draft) then
        val cmdStr =
          s"sed -i -c 's/${p.from}/${p.to}/' ${p.draft}"
        val cmdRes = os.call(cmd = ("sh", "-c", cmdStr), cwd = yangDir)
        if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
        else println(s"In ${p.draft}, '${p.from}' replaced with '${p.to}'")

    // Apply patch files to YANG modules to fix issues.
    case class ApplyDiff(draft: String, diffFileName: String)
    val diffs = List(
      ApplyDiff(
        draft = "ietf-yp-observation@2024-06-18.yang",
        diffFileName = "ietf-yp-observation@2024-06-18.yang.diff"
      )
    )

    for d <- diffs do
      if os.exists(yangDir / d.draft) &&
        os.exists(os.pwd / "patches" / d.diffFileName)
      then
        val cmdStr =
          s"patch --reverse ${d.draft} ../../patches/${d.diffFileName}"
        val cmdRes = os.call(cmd = ("sh", "-c", cmdStr), cwd = yangDir)
        if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
        else println(s"Patch applied to ${d.draft}")

    // Move import-only files to import-only directory.
    if !os.exists(yangDir / "import-only") then
      os.makeDir.all(yangDir / "import-only")
    for io <- importOnly do
      if os.exists(yangDir / io) then
        os.move(
          yangDir / io,
          yangDir / "import-only" / io,
          replaceExisting = true
        )

    // Generate tree output.
    {
      val deviations =
        if applyDeviations.value then
          println("Applying deviations")
          "../../cisco/*.yang"
        else
          println("Not applying deviations")
          ""

      val outputFile =
        if applyDeviations.value then "tree-output-with-deviations.txt"
        else "tree-output.txt"

      val cmd =
        s"pyang -f tree *.yang $deviations -p import-only > ../$outputFile"
      val cmdRes =
        (os.call(cmd = ("sh", "-c", cmd), cwd = yangDir, check = false))
      // if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
      // else
      // os.write.over(treeDir / "tree-output.txt", cmdRes.out.text())
      println(s"Tree output written to ${yangDir / os.up / outputFile}")
    }

  // Hack to make main-args work with scala-cli.
  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

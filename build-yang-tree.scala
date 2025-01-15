#!/usr/bin/env -S scala-cli shebang
//> using dep "com.lihaoyi::mainargs:0.7.6"
//> using dep "com.lihaoyi::os-lib:0.11.3"

import os.{*, given}
import mainargs.*

object BuildYangTree:
  val treeDir = "tree-output"

  def extractRPC(output: String, start: String, end: String, fileName: String) =
    val filePath = os.pwd / treeDir / fileName
    val selectedOutput =
      output
        .split("\n")
        .dropWhile(!_.contains(start))
        .takeWhile(!_.contains(end))
        .mkString("\n")
    os.write.over(filePath, selectedOutput)
    println(s"Tree output for $start written to $fileName")

  @main(doc = """|Build the tree output""".stripMargin)
  def buildYangTree() =
    val cmd = "pyang -f tree *.yang -p published/ --tree-line-length=69"
    val cmdRes = (os.call(cmd = ("sh", "-c", cmd), cwd = os.pwd / "yang"))
    if cmdRes.exitCode != 0 then println(s"Error: ${cmdRes.out.text()}")
    else
      val outputText = cmdRes.out.text()
      os.makeDir.all(os.pwd / treeDir)
      os.write.over(os.pwd / treeDir / "ietf-yp-lite-tree.txt", outputText)
      println("Tree output written to ietf-yp-lite-tree.txt")

      extractRPC(
        outputText,
        "establish-subscription",
        "delete-subscription",
        "establish-subscription-tree.txt"
      )

  // Hack to make main-args work with scala-cli.
  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)

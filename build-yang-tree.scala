#!/usr/bin/env -S scala-cli shebang
//> using dep "com.lihaoyi::mainargs:0.7.6"
//> using dep "com.lihaoyi::os-lib:0.11.3"

import os.{*, given}
import mainargs.*

object BuildYangTree:
  @main(doc =
    """|Build the tree output""".stripMargin)
  def buildYangTree() =
    val cmd = "pyang -f tree *.yang -p published/ --tree-line-length=69"
    val cmdRes = (os.call(cmd = ("sh", "-c", cmd), cwd = os.pwd / "yang"))
    if cmdRes.exitCode != 0 then
      println(s"Error: ${cmdRes.out.text()}")
    else
      os.write.over(os.pwd / "ietf-yp-lite-tree.txt", cmdRes.out.text())
      println("Tree output written to ietf-yp-lite-tree.txt")

  // Hack to make main-args work with scala-cli.
  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
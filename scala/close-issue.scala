import java.util._
import java.io.File
import java.io.FileWriter
import java.nio.file._

import org.jboss.set.aphrodite._
import org.jboss.set.aphrodite.config._

import scala.collection.JavaConversions._
import scala.collection.immutable.Map

import org.jboss.set.aphrodite.Aphrodite
import org.jboss.set.aphrodite.config._
import org.jboss.set.aphrodite.domain._

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

val EOL = "\n"

def saveOnFile(filename: String, content: String) = new java.io.PrintWriter(filename) { write(content);  close }

def deleteFile(file: File): Unit = if (file.exists && !file.delete) throw new Exception(s"Unable to delete ${file.getAbsolutePath}")

def fetchIssue(issueId:String) = {
  val aphrodite = Aphrodite.instance()
  val issue = aphrodite.getIssue(new java.net.URL(issueId))
  aphrodite.close()
  issue
}

def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    deleteFile(file)
}

object Args {

  @Parameter(names = Array( "-d", "--issue-workdir" ), description = "root directory where to create the issue dir", required = true)
  var rootDir = ""

  @Parameter(names = Array( "-i", "--bug-id" ), description = "bug id", required = true)
  var bugId = ""
}

new JCommander(Args, args.toArray: _*)

val issuePrefix = if ( Args.bugId.contains("bugzilla") ) "BZ" else ""
val issue = fetchIssue(Args.bugId)
println("Retrieved Issue:" + issue.getSummary.get())

val issueDir = Args.rootDir + File.separator + issuePrefix + issue.getTrackerId().get().toUpperCase()
println("Deleting workdir: " + issueDir)
deleteRecursively(new java.io.File(issueDir))

val trackingFile = "set-issues-status.csv"
println("Remove " + issue.getURL() + " from " + trackingFile)
saveOnFile(trackingFile,scala.io.Source.fromFile(trackingFile).getLines().filter(!_.startsWith(issue.getURL().toString())).mkString("\n"))

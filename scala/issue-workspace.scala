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

def formatIssue(issue: Issue) = {
  issue.getTrackerId.get + EOL +
  issue.getSummary.get + " (" + aggregateAllThreeFlags(issue.getStage()) + ")" + EOL
  issue.getRelease.toString + EOL
  issue.getDescription.get + EOL + aggregateComments(issue)
}

def aggregateComments(issue: Issue) = {
  var comments = EOL
  for ( comment <- issue.getComments() )
    comments += comment + EOL
  comments
}

def aggregateAllThreeFlags(stage: Stage):String = (for ( f <- stage.getStateMap.keySet() ) yield(f.toString + stage.getStatus(f).getSymbol + ",")).mkString.dropRight(1)

def saveOnFile(filename: String, content: String) = new java.io.PrintWriter(filename) { write(content);  close }

object Args {

  @Parameter(names = Array( "-u", "--username" ), description = "Bugzilla username", required = true)
  var username = ""

  @Parameter(names = Array( "-p", "--password" ), description = "BugZilla password", required = true)
  var password = ""

  @Parameter(names = Array( "-d", "--issue-workdir" ), description = "root directory where to create the issue dir", required = true)
  var rootDir = ""

  @Parameter(names = Array( "-i", "--bug-id" ), description = "bug id", required = true)
  var bugId = ""
}

new JCommander(Args, args.toArray: _*)

def getServerUrl(s: String) =  s.substring(0,s.indexOf('/', "https://".length) + 1)

val tracker = getServerUrl(Args.bugId)

val issuePrefix = if ( tracker.contains("bugzilla") ) "BZ" else ""

val issueTrackerConfigs: List[IssueTrackerConfig] = new ArrayList[IssueTrackerConfig];
issueTrackerConfigs.add(new IssueTrackerConfig(tracker, Args.username, Args.password, TrackerType.BUGZILLA, 1))

val aphrodite = Aphrodite.instance(AphroditeConfig.issueTrackersOnly(issueTrackerConfigs))
println("Aphrodite configured - retrieving data from server:" + tracker)

val issue = aphrodite.getIssue(new java.net.URL(Args.bugId))
println("Retrieved Issue:" + issue.getSummary.get())

val issueDir = Args.rootDir + File.separator + issuePrefix + issue.getTrackerId().get().toLowerCase()
println("Creating workdir: " + issueDir)
new java.io.File(issueDir + File.separator + ".workspace").mkdirs

val issueFile = issueDir + File.separator + issuePrefix + issue.getTrackerId.get + ".txt"
println("Printing issue " + issue.getTrackerId.get + " in file: " + issueFile)
saveOnFile(issueFile, formatIssue(issue))

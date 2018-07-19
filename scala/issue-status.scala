//$ scala -classpath ~/.m2/repository/org/jboss/jbossset/bugclerk/0.5.2.Final/bugclerk-0.5.2.Final.jar

import scala.collection.JavaConversions._
import collection.JavaConverters._
import scala.collection.immutable.Map
import java.util._
import java.io.File

import org.jboss.set.aphrodite.Aphrodite
import org.jboss.set.aphrodite.config._
import org.jboss.set.aphrodite.domain._
import org.jboss.set.aphrodite.issue.trackers.jira._

import com.beust.jcommander.JCommander
import com.beust.jcommander.IVariableArity
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

val SET_CUSTOM_STATUS_FILENAME = System.getProperty("user.dir") + File.separator + "set-issues-status.csv"

val displayPr = false

object Args extends IVariableArity {

    @Parameter(names = Array( "-i", "--bug-id" ), variableArity = true, description = "bug id", required = false)
    var bugId : List[String] = new java.util.ArrayList[String](0)

    def processVariableArity(optionName: String, options: Array[String]) = {
      bugId = options.toSeq
      bugId.length
    }
}

new JCommander(Args, args.toArray: _*)

def printIssueIfDevAckMissing(issue: Issue) = println(formatIssue(issue))
def formatIssue(issue: Issue) = issue.getTrackerId.get + " (" + aggregateAllThreeFlags(issue.getStage()) + "): " + issue.getSummary.get + "[" + issue.getType() + "]"
def aggregateAllThreeFlags(stage: Stage):String = (for ( f <- stage.getStateMap.keySet() ) yield(f.toString + stage.getStatus(f).getSymbol + ",")).mkString.dropRight(1)

def onlyReleasesFrom(issue: Issue) = {
  if ( issue.getReleases().isEmpty )
    "No Target Release"
  else
    (for (release <- issue.getReleases()) yield(release.getVersion.get())).mkString(",")
}

val aphrodite = Aphrodite.instance()

def getUrls(listAsString : java.util.List[String]) = {
  val coll : java.util.Collection[java.net.URL] = new java.util.ArrayList[java.net.URL](listAsString.length)
  import collection.JavaConverters._
  for ( item <- listAsString.asScala)
    coll.add(new java.net.URL(item))
  coll
}

def getAssigneeIfAny(issue: Issue) = { if ( issue.getAssignee() != null && issue.getAssignee.isPresent() && issue.getAssignee.get().getName().isPresent() ) issue.getAssignee.get().getName().get() else "" }

def getPrIfAny(jiraIssue: JiraIssue) = if ( jiraIssue != null ) jiraIssue.getPullRequests().toString() else "";

def getStreamFrom(issue:Issue) = if ( issue.getStreamStatus() != null && ! issue.getStreamStatus().isEmpty() ) issue.getStreamStatus().keySet().iterator().next().toString() else ""

def printLine(issue: Issue, issueStateMap: Map[String,String]) = {
  val url = issue.getURL
  val status = issueStateMap.get(url.toString).getOrElse(issue.getStatus.toString)
  val releases = onlyReleasesFrom(issue)
  val stream = getStreamFrom(issue)
  val summary = issue.getSummary.get
  val acks = formatAcks(issue.getStage.getStateMap)

  if ( displayPr ) {
    val pr = getPrIfAny(issue.asInstanceOf[JiraIssue])
    println(f"$url%-43s $status%-8s $stream $acks - $summary%-20s - PR: $pr")
  } else
    println(f"$url%-43s $status%-8s $stream $acks - $summary%-20s")
}

def printRelease(issue:Issue) = {
  if ( issue.getReleases().isEmpty )
    "No Releases"
  else
    (for ( release <- issue.getReleases()) yield(formatReleaseInfo(release))).mkString(",")
}

def formatReleaseInfo(release:Release) = {
  var res = ""
  if ( release.getVersion().isPresent() ) res = release.getVersion().get() + " " else res = "<No Version>" + " "
  if ( release.getMilestone().isPresent() ) res = res + release.getMilestone().get() else res = res + "<No Milestone>"
  res
}

def loadCustomStatusMapIfExists(filename: String) = if ( new File(filename).exists ) loadCustomStatusMap(filename).toMap else Map[String,String]()

def loadCustomStatusMap(filename: String) = {
    for {
      line <- io.Source.fromFile(filename).getLines()
      values = line.split(";").map(_.trim)
    } yield (values(0) -> values(1))
}

def loadIssues(issues:List[String]):java.util.List[Issue] = {
  try {
    aphrodite.getIssues( getUrls(issues) )
  } catch {
    case e: Throwable => throw e
  }
}

def formatAcks(map: java.util.Map[Flag, FlagStatus]) = { "[" + formatAcksMap(map).toString().dropRight(1).replace("Set","").replace("(","").replaceAll(":","") + "]" }

def formatAcksMap(map: java.util.Map[Flag, FlagStatus]) = { for ( e <- map.entrySet)  yield (e.getKey + ":" + e.getValue.getSymbol) }

val issueStateMap = loadCustomStatusMapIfExists(SET_CUSTOM_STATUS_FILENAME)
val issues = loadIssues(Args.bugId)
if ( issues.isEmpty() )
  println("No issue found for " + Args.bugId)
else
  for ( issue <- issues.asScala ) printLine(issue,issueStateMap)
aphrodite.close()

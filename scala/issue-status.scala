//$ scala -classpath ~/.m2/repository/org/jboss/jbossset/bugclerk/0.5.2.Final/bugclerk-0.5.2.Final.jar

import scala.collection.JavaConversions._
import scala.collection.immutable.Map
import java.util._

import org.jboss.set.aphrodite.Aphrodite
import org.jboss.set.aphrodite.config._
import org.jboss.set.aphrodite.domain._

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

object Args {

    @Parameter(names = Array( "-i", "--bug-id" ), variableArity = true, description = "bug id", required = true)
    var bugId : List[String] = new java.util.ArrayList[String](0)
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

def printLine(issue: Issue) = {
  val url = issue.getURL
  val status = issue.getStatus
  val releases = onlyReleasesFrom(issue)
  val stream = issue.getStreamStatus.keySet.iterator.next
  val summary = issue.getSummary.get

  println(f"$url%-43s $status%-8s $stream - $summary%-20s")
}

val issues = aphrodite.getIssues( getUrls(Args.bugId) )
import collection.JavaConverters._
for ( issue <- issues.asScala ) printLine(issue)
aphrodite.close()

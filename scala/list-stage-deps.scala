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

    @Parameter(names = Array( "-u", "--username" ), description = "Bugzilla username", required = true)
    var username = ""

    @Parameter(names = Array( "-p", "--password" ), description = "BugZilla password", required = true)
    var password = ""

    @Parameter(names = Array( "-i", "--bug-id" ), description = "bug id", required = true)
    var bugId = ""
}

new JCommander(Args, args.toArray: _*)

def getServerUrl(s: String) = s.substring(0,s.indexOf('/', "https://".length) + 1)
val tracker = getServerUrl(Args.bugId)
val trackerType = if ( tracker.contains("bugzilla") ) TrackerType.BUGZILLA else TrackerType.JIRA

def printIssueIfDevAckMissing(issue: Issue) = println(formatIssue(issue))

def formatIssue(issue: Issue) = issue.getTrackerId.get + " (" + aggregateAllThreeFlags(issue.getStage()) + "): " + issue.getSummary.get + "[" + issue.getType() + "]"

def aggregateAllThreeFlags(stage: Stage):String = (for ( f <- stage.getStateMap.keySet() ) yield(f.toString + stage.getStatus(f).getSymbol + ",")).mkString.dropRight(1)

val issueTrackerConfigs: List[IssueTrackerConfig] = new ArrayList[IssueTrackerConfig];
issueTrackerConfigs.add(new IssueTrackerConfig(tracker, Args.username, Args.password, trackerType, 1))
val aphrodite = Aphrodite.instance(AphroditeConfig.issueTrackersOnly(issueTrackerConfigs))
println("Aphrodite configured - retrieving data from server:" + tracker)

val issue = aphrodite.getIssue(new java.net.URL(Args.bugId))
println("Retrieved Issue:" + issue.getSummary.get())
aphrodite.getIssues(issue.getDependsOn.asInstanceOf[java.util.Collection[java.net.URL]]).foreach(printIssueIfDevAckMissing)
aphrodite.close()

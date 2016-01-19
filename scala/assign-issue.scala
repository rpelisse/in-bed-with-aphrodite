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

    @Parameter(names = Array( "-e", "--estimate" ), description = "estimate", required = false)
    var estimate = 24.0

    @Parameter(names = Array("-a", "--assigned-to" ), description = "", required = true)
    var assignedTo = ""

    @Parameter(names = Array("-c", "--comment" ), description = "", required = false)
    var comment = "I'll take a look at this issue asap."
}

// From here to the next comment, this is ugly piece of code - feel free to push improvement !!!
def getArg(args: Array[String], posInit: Int ) = {
  var pos = posInit
  var res = "";
  while ( pos < args.length && ! args(pos).startsWith("-")) {
    res += args(pos) + " "
    pos = pos + 1
  }
  res.trim()
}

var pos = 0;

import scala.collection.mutable.ArrayBuffer
var newArgs = ArrayBuffer[String]()
for (  arg <- args ) {
    pos = pos + 1;
    if ( arg.startsWith("-") ) {
        newArgs += arg;
        newArgs += getArg(args, pos)
    }
}
// OK, complete horror stop here...
new JCommander(Args, newArgs.toArray: _*)

def getServerUrl(s: String) = s.substring(0,s.indexOf('/', "https://".length) + 1)

val tracker = getServerUrl(Args.bugId)
val trackerType = if ( tracker.contains("bugzilla") ) TrackerType.BUGZILLA else TrackerType.JIRA

def allStageToSet() = {
  val stage = new Stage()
  stage.getStateMap().put(Flag.DEV,FlagStatus.SET)
  stage.getStateMap().put(Flag.PM,FlagStatus.SET)
  stage.getStateMap().put(Flag.QE,FlagStatus.SET)
  stage
}

val issueTrackerConfigs: List[IssueTrackerConfig] = new ArrayList[IssueTrackerConfig];
issueTrackerConfigs.add(new IssueTrackerConfig(tracker, Args.username, Args.password, trackerType, 1))
val aphrodite = Aphrodite.instance(AphroditeConfig.issueTrackersOnly(issueTrackerConfigs))
println("Aphrodite configured - retrieving data from server:" + tracker)

val issue = aphrodite.getIssue(new java.net.URL(Args.bugId))
println("Retrieved Issue:" + issue.getSummary.get())
issue.setAssignee(Args.assignedTo)
issue.setStatus(IssueStatus.ASSIGNED)
issue.setEstimation(new IssueEstimation(Args.estimate))
issue.setStage(allStageToSet)
aphrodite.updateIssue(issue)
aphrodite.addCommentToIssue(issue, new Comment(Args.comment, false))
println("Task " + issue.getTrackerId.get + " has been assigned to " + issue.getAssignee.get)

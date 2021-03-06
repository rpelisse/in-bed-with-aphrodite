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

    @Parameter(names = Array( "-i", "--bug-id" ), description = "bug id", required = true)
    var bugId = ""
}

new JCommander(Args, args.toArray: _*)

def printIssueIfDevAckMissing(issue: Issue) = println(formatIssue(issue))

def formatIssue(issue: Issue) = issue.getTrackerId.get + " (" + aggregateAllThreeFlags(issue.getStage()) + "): " + issue.getSummary.get + "[" + issue.getType() + "]"

def aggregateAllThreeFlags(stage: Stage):String = (for ( f <- stage.getStateMap.keySet() ) yield(f.toString + stage.getStatus(f).getSymbol + ",")).mkString.dropRight(1)

val aphrodite = Aphrodite.instance()
val issue = aphrodite.getIssue(new java.net.URL(Args.bugId))
println("Retrieved Issue:" + issue.getSummary.get())
println("Depends on:")
aphrodite.getIssues(issue.getDependsOn.asInstanceOf[java.util.Collection[java.net.URL]]).foreach(printIssueIfDevAckMissing)
println("Blocked by:")
aphrodite.getIssues(issue.getBlocks.asInstanceOf[java.util.Collection[java.net.URL]]).foreach(printIssueIfDevAckMissing)
aphrodite.close()

import java.util._
import java.io.File
import java.io.FileWriter
import java.nio.file._

import util.Try

import org.jboss.set.aphrodite._
import org.jboss.set.aphrodite.config._
import org.jboss.set.aphrodite.domain._

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

import scala.collection.JavaConversions._

// put your own filer here
val filters = scala.collection.immutable.Map(
  ("SET",     "https://bugzilla.redhat.com/buglist.cgi?cmdtype=dorem&remaction=run&sharer_id=374917&namedcmd=rpelisse-jboss-eap-6.4.z-superset-only-new"),
  ("BACKLOG", "https://bugzilla.redhat.com/buglist.cgi?cmdtype=dorem&remaction=run&sharer_id=213224&namedcmd=Potential%20JBoss%20SET%20EAP%206%20issues"),
  ("MINE",    "https://bugzilla.redhat.com/buglist.cgi?cmdtype=dorem&remaction=run&sharer_id=374917&namedcmd=mine")
)

// change if you will
val EXCLUDE_FILE = System.getProperty("user.home") + File.separator + ".exclude-list.csv"

object Args {

  @Parameter(names = Array("-f", "--bz-filter-name"), description = "Filter name", required = false)
  var filterName: String = ""

  @Parameter(names = Array("-u", "--bz-username"), description = "Bugzilla Username", required = true)
  var username: String = ""

  @Parameter(names = Array("-p", "--bz-password"), description = "Bugzilla Password", required = true)
  var password: String = ""

  @Parameter(names = Array("-c", "--clean-cache"), description = "Clean local cache file - forces lookup to remote server again.", required = false)
  var delete_cache_file: Boolean = false

  @Parameter(names = Array("-P", "--purge-ignore-list"), description = "Clean local ignore list file", required = false)
  var purgeIgnoreList: Boolean = false

  @Parameter(names = Array("-i", "--add-issues-to-ignore-list") , variableArity = true, required = false)
  var ignoreIds: java.util.List[String] = new java.util.ArrayList[String]()

}

def collectionToSet(ids: Collection[String]): Set[String] = {
  val set = new HashSet[String](ids.size())
  set.addAll(ids)
  set
}

def typeFilter(issue: Issue): Option[Issue] = issue.getType() match {
  case IssueType.UPGRADE => None
  case IssueType.ONE_OFF => None
  case IssueType.SUPPORT_PATCH => None
  case IssueType.PAYLOAD => None
  case _ => Some(issue)
}

def componentFilter(issueOrNot: Option[Issue]): Option[Issue] = {
  issueOrNot match {
    case Some(issue) => issue.getComponent.get match {
      case "RPMs" => None
      case "Documentation - Translation" => None
      case "Maven Repository" => None
      case "distribution" => None
      case "mod_cluster" => None
      case "Apache Server (httpd) and Connectors" => None
      case _ => Some(issue)
    }
    case None => None
  }
}

def formatEntry(bug: Issue): String= {
  bug.getURL + "\t[" + bug.getComponent.get + " - " + bug.getType.toString +  "] : \t'" + bug.getSummary().get + "'"
}

def mv(oldName: String, newName: String) =  Try(new File(oldName).renameTo(new File(newName))).getOrElse(false)

def endOfIdField(s: String): Int = {
  val end = s.indexOf('\t')
  if ( end > 0 ) return end
  return s.length
}

def loadFilterURl(filterName: String):String = {
  if ( filterName != null && ! "".equals(filterName) ) {
    filters.get(filterName).get
  } else
    throw new IllegalArgumentException("Not such filters available:" + filterName)
}

def loadBugsFromLocalCacheFile(filename: String) = {
  val source = scala.io.Source.fromFile(filename)
  val lines = try source.mkString finally source.close()
}

def appendToFile(fw: FileWriter, line: String):Unit = {
  try {
    fw.write(line + "\n")
  } catch {
     case e: Throwable => {
       Console.err.println(e.getMessage)
       System.exit(1)
     }
  }
}

def loadAndPrintCacheFileThenExit(cacheFile: File) = {
  Console.err.println("Read from " + cacheFile.getAbsolutePath + " and exit.")
  scala.io.Source.fromFile(cacheFile.getAbsolutePath()).getLines().foreach { println }
  System.exit(0)
}

def getServerUrl(s: String) = {
  s.substring(0,s.indexOf('/', "https://".length) + 1)
}

def excludeList(filename: String): List[String] = {

  if ( Files.exists(Paths.get(filename) ) )
    scala.io.Source.fromFile(filename).getLines().toList.collect { case s: String => s.substring(0, endOfIdField(s)) }
  else
    new java.util.ArrayList[String]()
}

def loadAndPrintCacheFileIfExistsAndQuitOrCreateIt(filterName: String, deleteCacheFile: Boolean) = {
  val cacheFile = new File( "/tmp/" + filterName  + ".csv")
  if ( cacheFile.exists() && ! deleteCacheFile ) {
    loadAndPrintCacheFileThenExit(cacheFile)
  }
  cacheFile.createNewFile()

  new FileWriter(cacheFile.getAbsolutePath(), true)
}

def buildAphrodite() = {

  val filterUrl = loadFilterURl(Args.filterName)
  val tracker = getServerUrl(filterUrl)
  val issueTrackerConfigs: List[IssueTrackerConfig] = new ArrayList[IssueTrackerConfig];
  issueTrackerConfigs.add(new IssueTrackerConfig(tracker, Args.username, Args.password, TrackerType.BUGZILLA, 1))
  Aphrodite.instance(AphroditeConfig.issueTrackersOnly(issueTrackerConfigs))
}

def buildURLFromFileLine(x: String) = {
  new java.net.URL(x.substring(0,endOfURL(x)))
}

def endOfURL(s: String, sep: String = "\t"): Int = { if ( s.indexOf(sep) == -1 ) s.length else s.indexOf(sep) }

def loadFile(pathToFile:String) = { scala.io.Source.fromFile(pathToFile).getLines() }

// FIXME: there is for sure a better Scala way to do this !!!
def loadExcludedIssues() = {
  var coll: java.util.Collection[java.net.URL] = new java.util.ArrayList[java.net.URL]()
  val list = for ( line <- loadFile(EXCLUDE_FILE)) yield(buildURLFromFileLine(line))
  list.foreach { coll.add }
  coll
}

def backupAndUpdateFile(it: scala.collection.GenTraversableOnce[String]) = {
  val tmpFile = EXCLUDE_FILE + ".tmp"
  new java.io.PrintWriter(tmpFile) { write(it.mkString("\n")); close }
  //FIXME add checksum before mv to avoid replace .bck if content is the same
  mv(EXCLUDE_FILE, EXCLUDE_FILE + ".bck")
  mv(tmpFile, EXCLUDE_FILE)
}

def purgeIgnoreList(excludeFilename: String): Unit = {
  Console.err.println("Purging excluded issues file from issue no longer in a NEW state.")

  var coll = loadExcludedIssues
  Console.err.println("Issues loaded:" + coll.size)
  val issuesToKeep = buildAphrodite().getIssues(coll).collect { case issue:Issue if issue.getStatus.equals(org.jboss.set.aphrodite.domain.IssueStatus.NEW) => issue }
  val issuesIndexedByURL = issuesToKeep.map(issue => issue.getURL -> issue).toMap
  Console.err.println("Issues to keep:" + issuesToKeep.size)
  backupAndUpdateFile(loadFile(EXCLUDE_FILE).collect {
    case line: java.lang.String if ( issuesIndexedByURL.containsKey(buildURLFromFileLine(line))) => line
  })
  Console.err.println("Purging done, " + EXCLUDE_FILE + " has been updated.")
}

def buildURLListFrom(issues: List[String]) = {
  var ignoreUrls: Seq[java.net.URL] = Seq[java.net.URL]()
  for ( issue <- issues) {
    try {
     ignoreUrls = ignoreUrls.:+(new java.net.URL(issue))
    } catch {
      case malformedUrl: java.net.MalformedURLException =>
        Console.err.println("Not a valid URL:" + issue + " - aborting, no change to the ignore list was made.")
      case unexceptedException: Throwable =>
        Console.err.println("Exception raised while processing issues list:" + unexceptedException)
    }
  }
  ignoreUrls.distinct // removes duplicates URL
}

// FIXME: there is for sure a better Scala way to do this !!!
// FIXME/ no reason for Seq, switch to List
def turnIntoURLsCollection(urls: Seq[java.net.URL]) = {
  var coll: java.util.Collection[java.net.URL] = new java.util.ArrayList[java.net.URL]()
  val list = for ( url <- urls ) yield(url)
  list.foreach {coll.add}
  coll
}

def ignoreIssues(issues: List[String]) = {
  val coll = turnIntoURLsCollection(buildURLListFrom(issues))
  val ignoredIssues = buildAphrodite().getIssues(coll)
  ignoredIssues.foreach { addBugToExcludeList }
}

def addBugToExcludeList(bug: Issue) =
  scala.tools.nsc.io.File(EXCLUDE_FILE).appendAll(formatEntry(bug)  + ", status " +
    bug.getStatus.toString + ", insert reason or reminder for ignore here\n")

new JCommander(Args, args.toArray: _*)

if ( "".equals(Args.filterName) ) {
  Console.err.println("Not filter name provided - exiting")
  System.exit(1)
}

if ( Args.purgeIgnoreList ) {
  purgeIgnoreList(EXCLUDE_FILE)
  System.exit(0)
}

if ( ! Args.ignoreIds.isEmpty ) {
  Console.err.println("Adding " + Args.ignoreIds.size + " issues (if no duplicate entries) to the ignore list:")
  ignoreIssues(Args.ignoreIds)
  System.exit(0)
}

Console.err.println("Loading local exclusion list " + EXCLUDE_FILE)
val excludedIds = excludeList(EXCLUDE_FILE)
Console.err.println(excludedIds.size + " issues to ignore.")

val fw = loadAndPrintCacheFileIfExistsAndQuitOrCreateIt(Args.filterName, Args.delete_cache_file)

// If we reach this, we need to load data from Tracker...
// FIXME: incrementing counter ? So un-scala-ee...
var nbIssuesIgnored = 0
var nbIssuesRetrieved = 0
buildAphrodite().searchIssuesByFilter(new java.net.URL(loadFilterURl(Args.filterName))).foreach(bug => {
    nbIssuesRetrieved = nbIssuesRetrieved + 1
    if ( ! excludedIds.contains(bug.getURL.toString())) {
      componentFilter(typeFilter(bug)) match {
        case Some(bug) => {
          val line = formatEntry(bug)
          println(line)
          appendToFile(fw, line)
        }
        case None => {}
      }
    } else
      nbIssuesIgnored = nbIssuesIgnored + 1
  }
)
fw.close()
Console.err.println("Nb issues retrieved:" + nbIssuesRetrieved)
Console.err.println("Nb issues ignored:" + nbIssuesIgnored)

import java.util._
import java.io.File
import java.io.FileWriter
import java.nio.file._

import util.Try

import org.jboss.set.aphrodite._
import org.jboss.set.aphrodite.config._
import org.jboss.set.aphrodite.domain._

import com.beust.jcommander.JCommander
import com.beust.jcommander.IVariableArity
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException

import scala.collection.JavaConversions._
import collection.JavaConverters._

val debug = false
val restURL = "https://issues.jboss.org/rest/api/latest/filter/"
val filters = scala.collection.immutable.Map(
  ("70z", restURL + "12324632"),
  ("71x", restURL + "12330472"),
  ("72x", restURL + "12332911"),
  ("ME",  restURL + "12327088"),
  ("SET", restURL + "12330394"),
  ("Backlog", restURL + "12317605")
)

val MAX_ISSUES_FETCHED = 1000

val EXCLUDE_FILE = System.getProperty("user.home") + File.separator + ".exclude-list.csv"

val EXCLUDE_COMPONENTS = scala.collection.immutable.List( "RPMs", "Documentation - Translation", "Maven Repository", "distribution", "mod_cluster", "Apache Server (httpd) and Connectors")

val SET_USERNAME_LIST =
  scala.collection.immutable.List("mstefank", "gaol", "soul2zimate","istudens","rpelisse","baranowb", "iweiss","thofman", "spyrkob", "dpospisil", "elguardian", "ron_sigal", "fedor.gavrilov", "ppalaga", "pgier" /* not SET, but PROD*/, "gbadner" /* not SET, but similar */)

object Args extends IVariableArity {

  @Parameter(names = Array("-f", "--filter-name"), description = "Filter name", required = false)
  var filterName: String = ""

  @Parameter(names = Array("-c", "--clean-cache"), description = "Clean local cache file - forces lookup to remote server again.", required = false)
  var delete_cache_file: Boolean = false

  @Parameter(names = Array("-P", "--purge-ignore-list"), description = "Clean local ignore list file", required = false)
  var purgeIgnoreList: Boolean = false

  @Parameter(names = Array("-i", "--add-issues-to-ignore-list") , variableArity = true, required = false)
  var ignoreIds: java.util.List[String] = new java.util.ArrayList[String]()

  @Parameter(names = Array("-r", "--reason-to-ignore"), description = "Use with -i and gives extra information on why the issue has been added to the ignore list", required = false)
  var reason: String = ""

  @Parameter(names = Array("-s", "--sort-by"), description = "indicate which field should be ", required = false)
  var sortedBy : String = "c"

  @Parameter(names = Array("-C", "--cursor"), description = "adds > to prefix the current 'cursor' issue", required = false)
  var cursorIssueId = ""

  @Parameter(names = Array("--jboss-set"), description = "Filters issue already assigned to SET Members" , required = false)
  var noSetFiltering = false;

  def processVariableArity(optionName: String, options: Array[String]) = {
    ignoreIds = options.toSeq
    ignoreIds.length
  }
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

def assigneeFilter(issueOrNot: Option[Issue]): Option[Issue] = {
  if ( ! Args.noSetFiltering ) return issueOrNot
  issueOrNot match {
    case Some(issue) => {
      if ( issue.getAssignee.isPresent) {
        if ( SET_USERNAME_LIST.contains( issue.getAssignee.get().getName().get() ) ) {
          None
        } else
          Some(issue)
      } else
        Some(issue)
    }
    case None => None
  }
}

def componentFilter(issueOrNot: Option[Issue]): Option[Issue] = {
  issueOrNot match {
    case Some(issue) => {
      import scala.collection.JavaConverters._
      if ( EXCLUDE_COMPONENTS.diff(issue.getComponents().asScala).size.>(0) )
         Some(issue)
      else
         None
    }
    case None => None
  }
}

def formatStage(map: Map[Flag, FlagStatus]) = { "[" + map.toString + "]" }

def formatAcks(map: Map[Flag, FlagStatus]) = { "[" + formatAcksMap(map).toString().dropRight(1).replace("Set","").replace("(","").replaceAll(":","") + "]" }

def formatAcksMap(map: Map[Flag, FlagStatus]) = { for ( e <- map.entrySet)  yield (e.getKey + ":" + e.getValue.getSymbol) }

def formatAssigne(bug: Issue) =  "@" + (if ( bug.getAssignee.isPresent ) bug.getAssignee.get().getName().get() else "no_one")

def formatEntry(bug: Issue): String= addCursorIfNeeded(bug) + " \t" + bug.getStatus + "\t" + bug.getComponents + " - " + bug.getType.toString +  "\t" + formatAssigne(bug) + "\t" + formatAcks(bug.getStage.getStateMap) + "\t'" + bug.getSummary().get + "'"

def mv(oldName: String, newName: String) =  Try(new File(oldName).renameTo(new File(newName))).getOrElse(false)

def addCursorIfNeeded(bug: Issue) = {
  if (! "".equals(Args.cursorIssueId) && bug.getURL.toString().contains(Args.cursorIssueId))
    ">>>> " + bug.getURL
  else
    bug.getURL.toString
}

def endOfIdField(s: String): Int = {
  val end = s.indexOf(' ')
  if ( end > 0 ) return end
  return s.length
}

def loadFilterURL(filterName: String):String = {
  if ( filterName != null && ! "".equals(filterName) && filters.contains(filterName) ) {
    return filters.get(filterName).get
  } else
    displayErrorMssgAndExit("No such filters available:" + filterName)
  return ""
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

def getServerUrl(s: String) = s.substring(0,s.indexOf('/', "https://".length) + 1)

def excludeList(filename: String): List[String] = {
  if ( Files.exists(Paths.get(filename) ) )
    scala.io.Source.fromFile(filename).getLines().toList.collect { case s: String => s.substring(0, endOfIdField(s)) }
  else
    new java.util.ArrayList[String]()
}

def loadAndPrintCacheFileIfExistsAndQuitOrCreateIt(filterName: String, deleteCacheFile: Boolean) = {
  val cacheFile = new File(getCacheFilePath(filterName))
  if ( cacheFile.exists() && ! deleteCacheFile ) loadAndPrintCacheFileThenExit(cacheFile)
  cacheFile.createNewFile()
  new FileWriter(cacheFile.getAbsolutePath(), true)
}

def buildAphrodite() = {
  val filterUrl = loadFilterURL(Args.filterName)
  Aphrodite.instance()
}

def buildURLFromFileLine(x: String) = new java.net.URL(x.substring(0,endOfURL(x)))

def endOfURL(s: String, sep: String = "\t"): Int = { if ( s.indexOf(sep) == -1 ) s.length else s.indexOf(sep) }

def loadFile(pathToFile:String) = { scala.io.Source.fromFile(pathToFile).getLines() }

def loadExcludedIssues(): List[java.net.URL] = (for ( line <- loadFile(EXCLUDE_FILE)) yield(buildURLFromFileLine(line))).toList

def backupAndUpdateFile(it: scala.collection.GenTraversableOnce[String]) = {
  val tmpFile = EXCLUDE_FILE + ".tmp"
  new java.io.PrintWriter(tmpFile) { write(it.mkString("\n")); close }
  if ( ! (loadFile(tmpFile).toList.diff(loadFile(EXCLUDE_FILE).toList)).isEmpty ) {
    mv(EXCLUDE_FILE, EXCLUDE_FILE + ".bck")
    mv(tmpFile, EXCLUDE_FILE)
  }
}

val statusNotTakenCareOf = scala.collection.immutable.List(IssueStatus.NEW, IssueStatus.CREATED, IssueStatus.UNDEFINED, IssueStatus.ASSIGNED)

def purgeIgnoreList(excludeFilename: String): Unit = {
  Console.err.println("Purging excluded issues file from issue no longer in a the following states:" + statusNotTakenCareOf.mkString(" "))
  val issuesToKeep = ( for ( url <- loadExcludedIssues ) yield(buildAphrodite().getIssue(url))).collect {
    case issue:Issue if statusNotTakenCareOf.contains(issue.getStatus) => issue
  }
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
        if ( debug ) Console.err.println("Not a valid URL:" + issue + " - aborting, no change to the ignore list was made.")
      case unexceptedException: Throwable =>
        if ( debug ) Console.err.println("Exception raised while processing issues list:" + unexceptedException)
    }
  }
  ignoreUrls.distinct // removes duplicates URL
}

def ignoreIssues(issues: List[String], reason: String = "No reason provided") =
  for ( issue <- buildAphrodite().getIssues( ((for ( url <- buildURLListFrom(issues) ) yield(url)).asJava) ) )
    yield(addBugToExcludeList(issue, reason))

def addBugToExcludeList(bug: Issue, reason: String) = scala.tools.nsc.io.File(EXCLUDE_FILE).appendAll(formatEntry(bug)  + ", state: " + bug.getStatus.toString + ", " + reason + "\n")

def getCacheFilePath(filterName:String) = "/tmp/" + filterName  + ".csv";

def sortByField(issue: Issue, option: String) = {
  option match {
    case "t" => issue.getType().toString()
    case "a" => if ( issue.getAssignee().isPresent() ) issue.getAssignee().get().getName().toString() else ""
    case "c" => issue.getComponents().toString
  }
}

def displayErrorMssgAndExit(mssg:String, exitCode:Int = 1) = {
  println(mssg)
  System.exit(exitCode)
}

def createUrl(url:String):java.net.URL = {
  try {
    new java.net.URL(url)
  } catch {
    case malformedUrl: java.net.MalformedURLException => displayErrorMssgAndExit("Malformed URL:" + url + "\n" + malformedUrl.getMessage,2)
    return null
  }
}

try {
  new JCommander(Args, args.toArray: _*)
} catch {
  case invalidParameter: com.beust.jcommander.ParameterException => displayErrorMssgAndExit("Invalid Parameter:" + invalidParameter.getMessage())
  case illegalArgument: java.lang.IllegalArgumentException => displayErrorMssgAndExit("Illegal Argument:" + illegalArgument)
}

if ( "".equals(Args.filterName) ) {
  Console.err.println("No filter name provided - exiting")
  System.exit(1)
}

if ( Args.delete_cache_file ) new File( getCacheFilePath(Args.filterName) ).delete()

if ( Args.purgeIgnoreList ) {
  purgeIgnoreList(EXCLUDE_FILE)
  System.exit(0)
}

if ( ! Args.ignoreIds.isEmpty ) {
  if ( ! "".equals(Args.reason) )
    ignoreIssues(Args.ignoreIds, Args.reason)
  else
    ignoreIssues(Args.ignoreIds)
  System.exit(0)
}

val fw = loadAndPrintCacheFileIfExistsAndQuitOrCreateIt(Args.filterName, Args.delete_cache_file)

Console.err.println("Loading local exclusion list " + EXCLUDE_FILE)
val excludedIds = excludeList(EXCLUDE_FILE)
Console.err.println(excludedIds.size + " issues to ignore.")

// If we reach this, we need to load data from Tracker...
// FIXME: incrementing counter ? So un-scala-ee...
var nbIssuesIgnored = 0
var nbIssuesRetrieved = 0
var nbIssueDiplayed = 0
val aphrodite = buildAphrodite()
aphrodite.searchIssuesByFilter(createUrl((loadFilterURL(Args.filterName)))).sortBy( sortByField(_, Args.sortedBy)).foreach(bug => {
    nbIssuesRetrieved = nbIssuesRetrieved + 1
    if ( ! excludedIds.contains(bug.getURL.toString())) {
      assigneeFilter(componentFilter( typeFilter(bug) ) ) match {
        case Some(bug) => {
          val line = formatEntry(bug)
          println(line)
          appendToFile(fw, line)
          nbIssueDiplayed = nbIssueDiplayed + 1
        }
        case None => {}
      }
    } else
      nbIssuesIgnored = nbIssuesIgnored + 1
  }
)
println("Nb Issue:" + nbIssueDiplayed + " (Retrieved:" + nbIssuesRetrieved + " / Ignored: " + nbIssuesIgnored + ")" )

fw.close()
aphrodite.close()

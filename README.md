In bed with Aphrodite
====

Just a bunch of scripts (scala scripts) using the [Aprhodite](https://github.com/jboss-set/aphrodite) domain API to help me in my daily work. Most likely quite specific to the JBoss SET team activites, but might be a good base for use for other Java/Scala developpers who would like to automate part of their daily interaction with JIRA, Bugzilla and Github.

Why Scala ?
----

The only reason I've develop those scripts in Scala, it's because it allowed me to use Java API within a concise, script-like structure. I'm not a Scala guru (feel free to PR some enhancement if you like), neither a Scala fan - so please go somewhere else for language troll ;)

How to use the script ?
----

~Important note: all the scripts expect the env variable TRACKER_USERNAME and TRACKER_PASSWORD to defined accordingly to the tracker (Bugzilla or JIRA) used.~

* assign-issue:
    Assign issue to somebody (change status to ASSIGNED), along with adding the required flags (dev?,pm?,qa?)
    and an estimate:

    Syntax:
    ```
    $ ./assign-issue <bug-url> [estimate] [assigned_to] [comment]
    ```

    Example:
    ```
    $ ./assign-issue.sh https://bugzilla.redhat.com/show_bug.cgi?id=1184440 '18' 'rpelisse@redhat.com' 'An nice useful, comment'
    ```

    ~Note: be carefull with the comment arg, it easily breaks !~

    ~Note: those not work with JIRA for now, due to an issue with Aphrodite
    ~
* issue-workspace:
    An script quite specific to my way of work - it set up a local folder for an issue, and download
    all the infos on it into a local file. Help work offline. Also, it creates .workspace folder to
    use with Eclipse. This is a start, it might get enhance down the road, to do more.

    Syntax:
    ```
    $ ./issue-workspace <bug-url>
    ```

    Example:
    ```
    $ ./issue-workspace https://bugzilla.redhat.com/show_bug.cgi?id=1235744
    ```

* list-stage-deps.sh:
    List of the dependencies of one issue, along with the status of each flag (dev,pm, qe). Come in
    handy, when one looks for "what is blocking this BZ or Payload". List is printed on stdout to
    allow parsing it afterward with grep, sed, awk, and so on....

    Syntax:
    ```
    $ ./list-stage-deps.sh <bug-url>
    ```

    Example:
    ```
    $ ./list-stage-deps.sh https://bugzilla.redhat.com/show_bug.cgi?id=1235744
    ```

* filter-bz:


    This script is pretty specific to the work of the JBoss Sustain Engineering Team (SET), but it
    can probably be adapted for other needs. What it does it fetch all the bugs description from a
    filter gathering all the NEW issue within the scope of the SET team. Thus, it also removes all
    entries that should be ignored by SET (see this function for more details), that the filter
    has not yet removed.

    Also the script removes entries based on a local exclusion file (~/.exclude-list.csv). This allow
    user to remove bugs he already looked at it and decided to not work on, and simply print the bugs
    that are really "new" to him. This content of the file is maintened by the script and allow to
    add some extra fields at the end of the line (to put or reminder why an issue has been added to
    the list.

    The scripts also stores the issues on a local CSV file (/tmp/<filtername>.csv) - mostly to not have to look
    up for data to the server (slow) everytime the user wants to take a look at the list.

    ~Note(1): in the context of this command, *filtered* means that issues are trim down by the script
    itself, it does NOT refer to the filtering work done on the server side by the repository side~

    ~Note(2): the filtername is pretty much mandatory for any commands. You add your own filtername
    by modifying the provided script.~

    Sample usage:

    1. Getting the (filtered) content from a the server:

    Syntax:
    ```
    $ ./filter-bz -f <filtername> -c
    ```

    Example:
    ```
    $ ./filter-bz -f SET -c
    ```

    2. Print a (filtered) content (from server, if no cache file, or from the cache file):

    Syntax
    ```
    $ ./filter-bz -f <filtername>
    ```

    Example
    ```
    $ ./filter-bz -f MINE
    ```

    3. Add an entry to the local exclude list

    Syntax:

    ```
    $ ./filter-bz -f <filtername> -i <urls...>
    ```

    Example:
    ```
    $ ./filter-bz -f SET -i https://bugzilla.redhat.com/show_bug.cgi?id=1200299 https://bugzilla.redhat.com/show_bug.cgi?id=1200399
    ```

    ~Note: The filtername is just required for consistency with the other commands and to allow to
    intiate properly Aphrodite. Issues ignored does NOT need to be retrieve by this filter.~

    TODO:
    * adding support to retrieve and filter JIRA issues
    * externalizing config (filtername + exclude list file) into ~/.filter-bz.conf


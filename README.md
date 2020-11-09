Git Lite is a Git-like command-line tool created in Java.

The main functionality that Git Lite supports is:

-> Saving the contents of entire directories of files. In Git Lite, this is called committing, and the saved contents themselves are called commits.

-> Restoring a version of one or more files or entire commits. In Git Lite, this is called checking out those files or that commit.

-> Viewing the history of your backups. In Git Lite, you view this history in something called the log.

-> Maintaining related sequences of commits, called branches.
Merging changes made in one branch into another.


Commands:


-> init: Creates new version control system in directory

-> add: stages files for commit

-> rm: removes files from staging

-> commit: saves snapshot of current setup

-> log: displays info about each commit in current branch

-> global-log: displays info about each commit ever made

-> find: finds commit based on message

-> status: displays which branches currently exist, which files have been staged, untracked files, and modifications not staged

-> checkout: retrieve either a branch, a file from current commit, or file from specified commit

-> branch: creates new branch

-> rm-branch: deletes branch with given name

-> reset: checksout all files tracked by commit

-> merge: merges files from a given branch into current


Usage : java gitlet.Main [command]

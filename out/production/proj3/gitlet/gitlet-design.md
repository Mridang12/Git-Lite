# Gitlet Design Document
**Name**: Mridang Sheth, Shivang Singh

# Classes and Data Structures
## Class: Blob implements Serializable

A class representing a Blob(file content)

**Fields:**
content(byte[])
UID (String)



## Class: Commit implements Serializable

A class representing a Commit containing Metadata, a map from file name to blob, and a pointer to parent.

**Fields:**
logMessage (String)
timestamp (String)
ID : UID (String)
parentCommit1 (Commit UID String)
parentCommit2 (Commit UID String)
BlobsMap (HashMap<String (File names), String (UID of Blobs)>)

**Methods:**
toString()


## Class: Repository 
- Represents a local repository
- Contains a collection of commits(some kind of map?)(SHA1 → commit obj)
- Contains **BRANCH** pointers and **HEAD** pointers
- FILE objs locating WORKING dir, STAGING dir

**Fields:**
Working directory (File)
Staging directory (File)
BranchesCol (MAP: name(String) → UID(String))
Head UID (String)
AddMap (HashMap: String (File name) → String (Blob UID)) (N
RemoveSet (HashSet: String(File name))

# Algorithms
    Blobs:
        - Blob(): the class constructor. Save the contents of file passed in to this blob. 
        - isModified(): 


    Commits: 
        - Commit(): the class constructor. Saves a commit as a commit tree. Information about the commit includes a log message, timestamp, unique integer id, it’s parent commit, and a list of blobs that make up the commit.
        - Stage(Blob b) : Will set up a blob passed in to be staged for a future commit. 
        - clearStaged(): Clear files to be added so there are none remaining. 
        - isEquals(Commit c): Checks if a commit is the exact same to its parent
        - toString():  Handles formating for logs 
            - ID, Date and Time, Message
    
    Repository:
        - Repository(): check if initialize → load the data to the fields
        - Init():  
            - Failure case
                - Already initialized? Gitlet version-control system already exists in the current directory”
            - Set up for persistence
            - Create “initial commit” with the specified metadata
            - Add (“master”, UID of “initial commit”) into branches
            - set HEAD to the “initial commit”
            - serialize all into correct files.
        - add():
            - Failure Cases
                - Check if the file exists? otherwise - error “File does not exist.”
            - Create Blob obj according to the file content
            - Check if the file of the same name exists in the current commit and if the content is the same
                - Yes?: remove it from being staged
            - Compute the UID, Check if that file exists in AddMap? add/overwrite UID it to the AddMap
            - Serialize the blob into correct directory
            - Serialize the AddMap into add.txt
            
        - commit():
            - Failure Cases
                - nothing to change → abort
                    - “No changes added to the commit.”
                - Blank message
                    - “ Please enter a commit message.”
            - Copy over the parent commit (Define a copy constructor in commit class)
            - change the parent field
            - Go through the AddMap
                - Check if the file is in commit?
                    - Yes, replace the UID in the same file in BlobsMap
                    - No, add it to BlobsMap
            - Go through the RemoveSet
                - remove from BlobsMap.
                
        - remove():
            - Failure Cases
                - If file isn’t staged, or being tracked by head 
                    - “No reason to remove the file”
            - If file currently staged for addition in AddMap
                - Remove it from Addmap
                - Delete Blob serialization.
            - If file being tracked in current commit (look through blobsmap)
                - Stage for removal and remove from working directory
        - log():
            - Starting at HEAD
                - iterate through the .parentCommit1 
                - sout every commit
                - until null(reach initial commit)
        - globalLog():
            - Go through the commits folder
# Persistence
- .gitlet is highest level
    - \stage
        - add.txt - HashMap<String (File Name), String (UID)>
        - remove.txt - HashSet<String (File Name)>
    - \commits
        - UID.txt - for each commit
    - \blobs
        - UID.txt - for each blob
    - branches.txt - serialized HashMap<String (branch names), String (commit UID)>


    


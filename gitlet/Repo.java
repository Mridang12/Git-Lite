package gitlet;


import com.sun.tools.corba.se.idl.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class Repo {
    static String stagingDirectory = Utils.join(Main.gitletDirectory, "staging").getPath();

    /** Maps the name of the branch (String) to the UID of the Head Commit of the branch (String) */
    private HashMap<String, String> branchMap;

    /** Name of the current Branch */
    private String head;

    /** Maps the name of the files staged for addition to the UID of its blob */
    private HashMap<String, String> addMap;

    /** Set of names of files staged for removal */
    private HashSet<String> removeSet;


    public Repo() {
         if (!Main.isInitialised()) {
             Utils.message("Not in an initialized Gitlet directory.");
             System.exit(0);
         }

         head = Utils.readObject(Utils.join(Main.gitletDirectory, "head.data"),
                 String.class);
         branchMap = Utils.readObject(Utils.join(Main.gitletDirectory, "branchMap.data"),
                 HashMap.class);
         addMap = Utils.readObject(Utils.join(stagingDirectory, "addMap.data"),
                 HashMap.class);
         removeSet = Utils.readObject(Utils.join(stagingDirectory, "removeSet.data"),
                 HashSet.class);

     }

    public void add(String fileName) {

         File file = Utils.join(Main.workingDirectory, fileName);
         if (file.exists() && !file.isDirectory()) {
             Blob blob = new Blob(file);
             Commit currentCommit = Commit.fromUID(getCommitID(head));
             if (!currentCommit.fileEqualInCommit(fileName, blob.getUID())) {
                 if (addMap.containsKey(fileName)) {
                     Blob.deleteBlob(addMap.get(fileName));
                 }
                 addMap.put(fileName, blob.serialize());
             } else {
                 addMap.remove(fileName);
                 removeSet.remove(fileName);
                 saveRemoveSet();
             }
             saveAddMap();
         } else {
             Utils.message("File does not exist.");
         }
     }

    public void commit(String logMessage) {
        if (this.addMap.isEmpty() && this.removeSet.isEmpty()) {
            Utils.message("No changes added to the commit.");
            return;
        }
        if (logMessage.isEmpty() || logMessage.isBlank()) {
            Utils.message("Please enter a commit message.");
            return;
        }
        Commit newCommit = new Commit(logMessage, getCommitID(head));;
        branchMap.put(head, newCommit.commit(addMap, removeSet));
        addMap.clear();
        removeSet.clear();
        saveAll();
     }

     private void commitMerge(String logMessage, String commitUID1, String commitUID2) {
         Commit newCommit = new Commit(logMessage, commitUID1, commitUID2);;
         branchMap.put(head, newCommit.commit(addMap, removeSet));
         addMap.clear();
         removeSet.clear();
         saveAll();
     }

    public void remove(String fileName) {
         if (addMap.containsKey(fileName)) {
             Blob.deleteBlob(addMap.remove(fileName));
             saveAddMap();
         } else {
             Commit currentCommit = Commit.fromUID(getCommitID(head));
             if (currentCommit.filePresentInCommit(fileName)) {
                 removeSet.add(fileName);
                 Utils.restrictedDelete(Utils.join(Main.workingDirectory, fileName));
                 saveRemoveSet();
             } else {
                 Utils.message("No reason to remove the file.");
             }
         }
     }

    public void log() {
        Commit currentCommit = Commit.fromUID(getCommitID(head));
        Utils.message(currentCommit.toString());
        while (currentCommit.getParentUID() != null && !currentCommit.getParentUID().isEmpty()) {
            currentCommit = Commit.fromUID(currentCommit.getParentUID());
            Utils.message("");
            Utils.message(currentCommit.toString());
        }
        Utils.message("");
    }

    public void globalLog() {
         File commitDir = new File(Commit.commitDirectory);
         if (commitDir.listFiles().length == 0) {
             return;
         }
         String message = "";
         for (File commit : commitDir.listFiles()) {
             message += Utils.readObject(commit, Commit.class).toString() + "\n\n";
         }

         Utils.message(message.strip() + "\n");
    }

    public void find(String logMessage) {
        File commitDir = new File(Commit.commitDirectory);
        ArrayList<String> list = new ArrayList<String>();
        String message = "";
        for (File commit : commitDir.listFiles()) {
            Commit c = Utils.readObject(commit, Commit.class);
            if (logMessage.equals(c.logMessage)) {
                list.add(c.UID);
            }
        }
        if (list.isEmpty()) {
            message = "Found no commit with that message.";
        } else {
            for (String UID : list) {
                message += UID + "\n";
            }
        }
        Utils.message(message.strip());
    }

    public void status() {

         String branchMessage = "=== Branches ===\n";

         for (String branchName : getSortedSet(branchMap.keySet())) {
             if (branchName.equals(head)) {
                 branchMessage += "*" + branchName + "\n";
             } else {
                 branchMessage += branchName + "\n";
             }
         }

         String stagedMessage = "=== Staged Files ===" + "\n";
         for (String fileName : getSortedSet(addMap.keySet())) {
             stagedMessage += fileName + "\n";
         }

         String removedMessage = "=== Removed Files ===\n";
         for (String fileName : getSortedSet(removeSet)) {
             removedMessage += fileName + "\n";
         }

         List<String> modificationsList = new ArrayList<String>();
         String modifications = "=== Modifications Not Staged For Commit ===\n";
         Commit currentCommit = Commit.fromUID(getCommitID(head));
         HashMap<String, String> blobMap = currentCommit.getBlobMap();
         for (String fileName : blobMap.keySet()) {
             File file = Utils.join(Main.workingDirectory, fileName);
             if (!file.exists()) {
                 if (!removeSet.contains(fileName)) {
                     modificationsList.add(fileName + " (deleted)\n");
                 }
             } else {
                 Blob fileBlob = new Blob(file);
                 if (!fileBlob.getUID().equals(blobMap.get(fileName))) {
                     if (!addMap.containsKey(fileName)) {
                         modificationsList.add(fileName + " (modified)\n");
                     }
                 }
             }
         }
         for (String fileName : addMap.keySet()) {
             File file = Utils.join(Main.workingDirectory, fileName);
             if (!file.exists()) {
                 modificationsList.add(fileName + " (deleted)\n");
             } else {
                 Blob fileBlob = new Blob(file);
                 if (!fileBlob.getUID().equals(addMap.get(fileName))) {
                     modificationsList.add(fileName + " (modified)\n");
                 }
             }
         }
         Collections.sort(modificationsList);
         for (String mod : modificationsList) {
            modifications += mod;
         }

         List<String> untrackList = new ArrayList<String>();
         String untracked = "=== Untracked Files ===\n";
         for (File file : new File(Main.workingDirectory).listFiles()) {
             if (!file.isDirectory()) {
                 if ((!blobMap.containsKey(file.getName())
                  && !addMap.containsKey(file.getName()))
                  || removeSet.contains(file.getName())) {
                     untrackList.add(file.getName() + "\n");
                 }
             }
         }
         Collections.sort(untrackList);
         for (String s : untrackList) {
             untracked += s;
         }

         Utils.message(branchMessage.strip() + "\n\n"
                            + stagedMessage.strip() + "\n\n"
                            + removedMessage.strip() + "\n\n"
                            + modifications.strip() + "\n\n"
                            + untracked.strip());

    }

    public void checkout(String commitUID, String fileName) {
         if(commitUID.length() < 40) {
            commitUID = estimateCommitUID(commitUID);
         }
         Commit commit = Commit.fromUID(commitUID);
         if (commit.filePresentInCommit(fileName)) {
             if (addMap.containsKey(fileName)) {
                 addMap.remove(fileName);
                 saveAddMap();
             }
             if (removeSet.contains(fileName)) {
                 removeSet.remove(fileName);
                 saveRemoveSet();
             }
             commit.restoreFile(fileName);
         } else {
             Utils.message("File does not exist in that commit.");
         }
    }

    public void checkout(String branchName) {
         if (branchName.equals(head)) {
             Utils.message("No need to checkout the current branch.");
             return;
         }
         String commitID = getCommitID(branchName);
         if (!commitID.isEmpty()) {
             Commit newCommit = Commit.fromUID(commitID);
             Commit currentCommit = Commit.fromUID(getCommitID(head));

             HashMap<String, String> newBlobsMap = newCommit.getBlobMap();
             HashMap<String, String> currentBlobsMap = currentCommit.getBlobMap();

             for (String fileName : newBlobsMap.keySet()) {
                 if (Utils.join(Main.workingDirectory, fileName).exists() && !currentBlobsMap.containsKey(fileName)) {
                     Utils.message("There is an untracked file in the way; delete it, or add and commit it first.");
                     return;
                 }
             }

             for (String fileName : currentBlobsMap.keySet()) {
                 Utils.restrictedDelete(Utils.join(Main.workingDirectory, fileName));
             }
             for (String fileName : newBlobsMap.keySet()) {
                 newCommit.restoreFile(fileName);
             }
             head = branchName;
             addMap.clear();
             removeSet.clear();
             saveRemoveSet();
             saveAddMap();
             saveHead();
         }
    }

    public void branch(String branchName) {
         if (branchMap.containsKey(branchName)) {
             Utils.message("A branch with that name already exists.");
             return;
         } else {
             branchMap.put(branchName, getCommitID(head));
             saveBranchMap();
         }
    }

    public void removeBranch (String branchName) {
         if (branchName.equals(head)) {
             Utils.message("Cannot remove the current branch.");
         } else if (branchMap.containsKey(branchName)) {
             branchMap.remove(branchName);
             saveBranchMap();
         } else {
             Utils.message("A branch with that name does not exist.");
         }
    }

    public void reset(String commitID) {
        if(commitID.length() < 40) {
            commitID = estimateCommitUID(commitID);
        }
        Commit newCommit = Commit.fromUID(commitID);
        Commit currentCommit = Commit.fromUID(getCommitID(head));

        HashMap<String, String> newBlobsMap = newCommit.getBlobMap();
        HashMap<String, String> currentBlobsMap = currentCommit.getBlobMap();

        for (String fileName : newBlobsMap.keySet()) {
            if (Utils.join(Main.workingDirectory, fileName).exists() && !currentBlobsMap.containsKey(fileName)) {
                Utils.message("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }

        for (String fileName : currentBlobsMap.keySet()) {
            Utils.restrictedDelete(Utils.join(Main.workingDirectory, fileName));
        }
        for (String fileName : newBlobsMap.keySet()) {
            newCommit.restoreFile(fileName);
        }
        branchMap.put(head, commitID);
        addMap.clear();
        removeSet.clear();
        saveRemoveSet();
        saveAddMap();
        saveBranchMap();
    }

    public void merge(String branchName) {

        if (branchName.equals(getHead())) {
            Utils.message("Cannot merge a branch with itself.");
            return;
        }
        if (!branchMap.containsKey(branchName)) {
            Utils.message("A branch with that name does not exist.");
            return;
        }
        String commitUID = getCommitID(branchName);
        String headCommitUID = getCommitID(getHead());

        if (commitUID == null || commitUID.isEmpty()) {
            return;
        }

        if (!addMap.isEmpty() || !removeSet.isEmpty()) {
            Utils.message("You have uncommitted changes.");
            return;
        }


        if (isBackwardMerge(commitUID)) {
            Utils.message("Given branch is an ancestor of the current branch.");
            return;
        } else if (isForwardMerge(commitUID)) {
            reset(commitUID);
            Utils.message("Current branch fast-forwarded.");
            return;
        } else {
            Commit headCommit = Commit.fromUID(headCommitUID);
            Commit givenCommit = Commit.fromUID(commitUID);
            Commit commonAncestor = Commit.fromUID(findCommonAncestor(commitUID));

            for (String fileName : givenCommit.getBlobMap().keySet()) {
                if (Utils.join(Main.workingDirectory, fileName).exists()
                        && !headCommit.getBlobMap().containsKey(fileName)) {
                    Utils.message("There is an untracked file in the way; delete it, or add and commit it first.");
                    return;
                }
            }
            HashMap<String, String> ancestorBlobMap = commonAncestor.getBlobMap();
            HashMap<String, String> currentBlobMap = headCommit.getBlobMap();
            HashMap<String, String> givenBlobMap = givenCommit.getBlobMap();

            for (String fileName : ancestorBlobMap.keySet()) {
                if (currentBlobMap.containsKey(fileName)) {
                    if (givenBlobMap.containsKey(fileName)) {
                        if (!isFileModified(currentBlobMap.get(fileName), ancestorBlobMap.get(fileName))
                        && isFileModified(currentBlobMap.get(fileName), ancestorBlobMap.get(fileName))) {
                            Blob.fromUID(currentBlobMap.get(fileName)).toFile(fileName);
                            this.add(fileName);
                        }
                        if (isFileModified(currentBlobMap.get(fileName), ancestorBlobMap.get(fileName))
                        && isFileModified(givenBlobMap.get(fileName), ancestorBlobMap.get(fileName))
                        && isFileModified(givenBlobMap.get(fileName), currentBlobMap.get(fileName))) {
                            mergeFiles(fileName, currentBlobMap.get(fileName), givenBlobMap.get(fileName));
                        }
                    } else {
                        if (!isFileModified(currentBlobMap.get(fileName), ancestorBlobMap.get(fileName))) {
                            this.remove(fileName);
                        } else {
                            mergeFiles(fileName, currentBlobMap.get(fileName), givenBlobMap.get(fileName));
                        }
                    }
                } else if (givenBlobMap.containsKey(fileName)) {
                    if (isFileModified(givenBlobMap.get(fileName), ancestorBlobMap.get(fileName))) {
                        mergeFiles(fileName, currentBlobMap.get(fileName), givenBlobMap.get(fileName));
                    }
                }
            }

            for (String fileName : givenBlobMap.keySet()) {
                if (!ancestorBlobMap.containsKey(fileName)
                        && !currentBlobMap.containsKey(fileName)) {
                    Blob.fromUID(givenBlobMap.get(fileName)).toFile(fileName);
                    this.add(fileName);
                } else if (!ancestorBlobMap.containsKey(fileName)
                        && currentBlobMap.containsKey(fileName)) {
                    mergeFiles(fileName, currentBlobMap.get(fileName), givenBlobMap.get(fileName));
                }
            }

            commitMerge("Merged " + branchName + " into " + head + ".",
                    headCommitUID, commitUID);
        }

    }


     //Start Util functions

    private List<String> getSortedSet(Set<String> s) {
        List<String> list = new ArrayList<String>(s);
        Collections.sort(list);
        return list;
    }

    public String getCommitID(String branchName) {
         if (branchMap.containsKey(branchName)) {
             return branchMap.get(branchName);
         } else {
             Utils.message("No such branch exists.");
             return "";
         }
     }

     public String getHead() {
         return head;
     }

     private void saveAddMap() {
         File addMapFile = Utils.join(stagingDirectory, "addMap.data");
         Utils.writeObject(addMapFile, this.addMap);
     }

     private void saveRemoveSet() {
         File removeSetFile = Utils.join(stagingDirectory, "removeSet.data");
         Utils.writeObject(removeSetFile, this.removeSet);
     }

     private void saveBranchMap() {
         Utils.writeObject(Utils.join(Main.gitletDirectory, "branchMap.data"),
                 branchMap);
     }

     private void saveHead() {
         Utils.writeObject(Utils.join(Main.gitletDirectory, "head.data"), head);
     }

     private void saveAll() {
         saveAddMap();
         saveRemoveSet();
         saveBranchMap();
         saveHead();
     }

    static void setUpRepo() {
        File staging = new File(stagingDirectory);
        if (!staging.exists()) {
            staging.mkdir();
        }
        File addMapFile = Utils.join(stagingDirectory, "addMap.data");
        HashMap<String, String> addMap = new HashMap<String, String>();
        if (!addMapFile.exists()) {
            Utils.writeObject(addMapFile, addMap);
        }
        File removeSetFile = Utils.join(stagingDirectory, "removeSet.data");
        HashSet<String> removeSet = new HashSet<String>();
        if (!removeSetFile.exists()) {
            Utils.writeObject(removeSetFile, removeSet);
        }

        File commits = new File(Commit.commitDirectory);
        if (!commits.exists()) {
            commits.mkdir();
        }

        File blobs = new File(Blob.blobDirectory);
        if (!blobs.exists()) {
            blobs.mkdir();
        }

        Commit c = new Commit("initial commit");
        c.isFirstCommit = true;
        HashMap<String, String> branchMap = new HashMap<String, String>();
        String head = "master";
        branchMap.put(head, c.commit(addMap, removeSet));

        Utils.writeObject(Utils.join(Main.gitletDirectory, "head.data"), head);
        Utils.writeObject(Utils.join(Main.gitletDirectory, "branchMap.data"), branchMap);
    }

    private static boolean isFileModified(String UID1, String UID2) {
        return !UID1.equals(UID2);
    }

    private void mergeFiles (String fileName, String blobUID1, String blobUID2) {
        byte[] prefix = "<<<<<<< HEAD\n".getBytes();
        byte[] middle = "=======\n".getBytes();
        byte[] suffix = ">>>>>>>".getBytes();
        Utils.writeContents(Utils.join(Main.workingDirectory, fileName),
                prefix, Blob.fromUID(blobUID1).getContents(), middle,
                Blob.fromUID(blobUID2).getContents(), suffix);
        Utils.message("Encountered a merge conflict.");
    }

    private String estimateCommitUID(String commitUID){

        File commitDir = new File(Commit.commitDirectory);
        String[] list = commitDir.list(new FilenameFilter() {

            public boolean accept(File file, String s) {
                return s.startsWith(commitUID);
            }
        });
        if (list.length != 1) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
            return null;
        } else {
            return list[0].substring(0, list[0].length() - ".data".length());
        }
    }

    private String findCommonAncestor (String commitUID) {

        HashSet<String> ancestors = new HashSet<String>();
        Commit c = Commit.fromUID(commitUID);
        findAllAncestors(c, ancestors);

        Commit headCommit = Commit.fromUID(getCommitID(head));
        Queue<Commit> ancestorQueue = new PriorityQueue<Commit>();
        if (headCommit.getParent1() != null) {
            ancestorQueue.add(headCommit.getParent1());
        }
        if (headCommit.getParent2() != null) {
            ancestorQueue.add(headCommit.getParent2());
        }

        while (!ancestorQueue.isEmpty()) {
            c = ancestorQueue.poll();
            if (c == null) {
                continue;
            }
            if (ancestors.contains(c.UID)) {
                return c.UID;
            } else {
                if (headCommit.getParent1() != null) {
                    ancestorQueue.add(headCommit.getParent1());
                }
                if (headCommit.getParent2() != null) {
                    ancestorQueue.add(headCommit.getParent2());
                }
            }
        }

        return null;
    }

    private boolean isBackwardMerge (String commitUID) {
        String headstring = getCommitID(head);
        Commit headCommit = Commit.fromUID(headstring);
        HashSet<String> ancestors = new HashSet<String>();
        findAllAncestors(headCommit, ancestors);
        return ancestors.contains(commitUID);
    }

    private boolean isForwardMerge (String commitUID) {
        HashSet<String> ancestors = new HashSet<String>();
        Commit c = Commit.fromUID(commitUID);
        findAllAncestors(c, ancestors);
        return ancestors.contains(getCommitID(head));
    }

    private void findAllAncestors (Commit c, HashSet<String> ancestors) {
        if (c == null) {
            return;
        } else {
            ancestors.add(c.UID);
            findAllAncestors(c.getParent1(), ancestors);
            findAllAncestors(c.getParent2(), ancestors);
        }
    }

}

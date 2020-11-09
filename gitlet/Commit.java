package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class Commit implements Serializable, Comparable<Commit> {

    public String logMessage;
    public String timeStamp;
    public String UID;
    public String parentCommit1;
    public String parentCommit2;
    boolean isFirstCommit;

    /** Maps the Name of the file (String) to the UID of the blob of the file (UID) */
    private HashMap<String, String> blobMap;

    static String commitDirectory = Utils.join(Main.gitletDirectory, "commits").getPath();

    public Commit(String logMessage) {
        this.logMessage = logMessage;
        blobMap = new HashMap<String, String>();
    }

    public Commit(String logMessage, String parent1UID) {
        this(logMessage);
        this.parentCommit1 = parent1UID;
    }

    public Commit(String logMessage, String parent1UID, String parent2UID) {
        this(logMessage);
        this.parentCommit1 = parent1UID;
        this.parentCommit2 = parent2UID;
    }

    public static Commit fromUID(String UID) {
        File commit = Utils.join(commitDirectory, (UID + ".data"));
        if (commit.exists()) {
            return Utils.readObject(commit, Commit.class);
        } else {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        }
        return null;
    }

    public String commit(HashMap<String, String> addMap, HashSet<String> removeSet) {
        if (parentCommit1 != null && !parentCommit1.isEmpty()) {
            blobMap.putAll(fromUID(parentCommit1).blobMap);
        }
        blobMap.putAll(addMap);
        for (String fileName : removeSet) {
            blobMap.remove(fileName);
        }
        this.timeStamp = getCurrentTimestamp(isFirstCommit);
        this.UID = Utils.sha1(Utils.serialize(this));
        Utils.writeObject(Utils.join(commitDirectory, (this.UID + ".data")), this);

        return this.UID;
    }

    public boolean fileEqualInCommit(String name, String UID) {
        return blobMap.containsKey(name) && blobMap.get(name).equals(UID);
    }

    public boolean filePresentInCommit(String name) {
        return blobMap.containsKey(name);
    }

    public String getParentUID() {
        return parentCommit1;
    }

    public HashMap<String, String> getBlobMap() {
        return (HashMap<String, String>) blobMap.clone();
    }

    public void restoreFile(String fileName) {
        Blob fileBlob = Blob.fromUID(blobMap.get(fileName));
        fileBlob.toFile(fileName);
    }

    public Commit getParent1() {
        if (parentCommit1 != null && !parentCommit1.isEmpty()) {
            return fromUID(parentCommit1);
        } else {
            return null;
        }
    }

    public Commit getParent2() {
        if (parentCommit2 != null && !parentCommit2.isEmpty()) {
            return fromUID(parentCommit2);
        } else {
            return null;
        }
     }

    @Override
    public String toString() {

        String message = "===";
        String mergeMessage = "";
        message += "\n" + "commit " + UID;
        if (parentCommit1 != null && !parentCommit1.isEmpty()
            && parentCommit2 != null && !parentCommit2.isEmpty()) {
            message += "\nMerge: " + parentCommit1.substring(0, 7) + " " + parentCommit2.substring(0, 7);
        }
        message += "\nDate: " + timeStamp;
        message += "\n" + logMessage;

        return message;
    }

    private static String getCurrentTimestamp(boolean isFirstCommit) {
        DateFormat df = new SimpleDateFormat("E MMM d HH:mm:ss yyyy Z");
        Date dateobj = new Date();
        if (isFirstCommit) {
            dateobj.setTime(0);
        }
        return df.format(dateobj);
    }


    @Override
    public int compareTo(Commit commit) {
        return this.timeStamp.compareTo(commit.timeStamp);
    }
}

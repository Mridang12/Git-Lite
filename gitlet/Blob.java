package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {

    private String UID;
    private byte[] contents;

    static String blobDirectory = Utils.join(Main.gitletDirectory, "blobs").getPath();

    public Blob (File file) {
        contents = Utils.readContents(file);
        UID = Utils.sha1(contents);
    }

    public void toFile(String fileName) {
        Utils.writeContents(Utils.join(new File(Main.workingDirectory), fileName), this.contents);
    }

    public String serialize() {
        Utils.writeObject(Utils.join(blobDirectory, UID + ".data"), this);
        return UID;
    }

    public static Blob fromUID(String UID) {
        File blob = Utils.join(blobDirectory, UID + ".data");
        if (blob.exists()) {
            return Utils.readObject(blob, Blob.class);
        } else {
            Utils.message("No File with that id exists.");
            System.exit(0);
        }
        return null;
    }

    public static void deleteBlob(String UID) {
        File blob = Utils.join(blobDirectory, UID + ".data");
        if (blob.exists()) {
            blob.delete();
        }
    }

    public boolean equals(Blob obj) {
        return this.UID.equals(obj.UID);
    }

    public static boolean equals(String UID, String UID2) {
        return UID.equals(UID2);
    }

    public String getUID() {
        return UID;
    }

    public byte[] getContents() {
        return contents;
    }


}

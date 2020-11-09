package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {

    static String workingDirectory = System.getProperty("user.dir");
    static String gitletDirectory = Utils.join(workingDirectory, ".gitlet").getPath();


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        Repo repo;
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            System.exit(0);
        }

        switch (args[0]) {
            case "init" :
                assertCondition(args.length == 1, "Incorrect operands.");
                setUpRepo();
                break;
            case "add" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.add(args[1]);
                break;
            case "commit" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.commit(args[1]);
                break;
            case "rm" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.remove(args[1]);
                break;
            case "log" :
                assertCondition(args.length == 1, "Incorrect operands.");
                repo = new Repo();
                repo.log();
                break;
            case "global-log" :
                assertCondition(args.length == 1, "Incorrect operands.");
                repo = new Repo();
                repo.globalLog();
                break;
            case "find" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.find(args[1]);
                break;
            case "status" :
                assertCondition(args.length == 1, "Incorrect operands.");
                repo = new Repo();
                repo.status();
                break;
            case "checkout" :
                assertCondition(args.length >= 2 && args.length <= 4, "Incorrect operands.");
                repo = new Repo();
                if (args.length == 2) {
                    repo.checkout(args[1]);
                } else if (args.length == 3) {
                    assertCondition(args[1].equals("--"), "Incorrect operands.");
                    repo.checkout(repo.getCommitID(repo.getHead()), args[2]);
                } else {
                    assertCondition(args[2].equals("--"), "Incorrect operands.");
                    repo.checkout(args[1], args[3]);
                }
                break;
            case "branch" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.branch(args[1]);
                break;
            case "rm-branch" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.removeBranch(args[1]);
                break;
            case "reset" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.reset(args[1]);
                break;
            case "merge" :
                assertCondition(args.length == 2, "Incorrect operands.");
                repo = new Repo();
                repo.merge(args[1]);
                break;
            default:
                Utils.message("No command with that name exists.");
        }
    }

    public static boolean isInitialised() {
        return new File(gitletDirectory).exists();
    }

    public static void assertCondition(boolean condition, String message) {
        if (!condition) {
            Utils.message(message);
            System.exit(0);
        }
    }

    public static void setUpRepo() {
        if (!isInitialised()) {
            File gitlet = new File(gitletDirectory);
            gitlet.mkdir();
            Repo.setUpRepo();
        } else {
            Utils.message("A Gitlet version-control system already exists in the current directory.");
        }
    }

}
